
import com.virtuslab.gitmachete.buildsrc.*
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.BuildPluginTask
import org.jetbrains.intellij.platform.gradle.tasks.SignPluginTask
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI
import java.util.Base64
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion as GradleKotlinVersion

plugins {
  checkstyle
  `java-library`
  alias(libs.plugins.jetbrains.changelog)
  alias(libs.plugins.jetbrains.intellij)
  alias(libs.plugins.taskTree)
}

val javaVersionProperties = PropertiesHelper.getProperties(rootDir.resolve("java-version.properties"))
val targetJavaVersion: JavaVersion by extra(
  JavaVersion.toVersion(javaVersionProperties.getProperty("jdkVersionForGradleAndGeneratedClassfiles").toInt()),
)

val ciBranch: String? by extra(System.getenv("CIRCLE_BRANCH"))
val isCI: Boolean by extra(System.getenv("CI") == "true")
val jetbrainsMarketplaceToken: String? by extra(System.getenv("JETBRAINS_MARKETPLACE_TOKEN"))

val intellijVersions by extra(
  IntellijVersions.from(
    intellijVersionsProperties = PropertiesHelper.getProperties(rootDir.resolve("intellij-versions.properties")),
    overrideBuildTarget = project.properties["overrideBuildTarget"] as String?,
  ),
)

fun String.fromBase64(): String = String(Base64.getDecoder().decode(this))

val pluginSignCertificateChain: String? by extra(System.getenv("PLUGIN_SIGN_CERT_CHAIN_BASE64")?.fromBase64())
val pluginSignPrivateKey: String? by extra(System.getenv("PLUGIN_SIGN_PRIVATE_KEY_BASE64")?.fromBase64())
val pluginSignPrivateKeyPass: String? by extra(System.getenv("PLUGIN_SIGN_PRIVATE_KEY_PASS"))

val shouldRunAllCheckers: Boolean by extra(isCI || project.hasProperty("runAllCheckers"))

tasks.register<UpdateIntellijVersions>("updateIntellijVersions")

// Optional Maven proxy URL from gitignored gradle-local.properties (see gradle-local.properties.example).
val optionalMavenProxyFromGradleLocal: String? = run {
  val f = rootDir.resolve("gradle-local.properties")
  if (!f.isFile) return@run null
  Properties().apply { f.reader().use { load(it) } }
    .getProperty("mavenProxyUrl")
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
}

// Either the corporate proxy or Maven Central - never both (see gradle-local.properties.example).
// gradle/init.gradle still rewrites any stray Central URLs (e.g. from plugins) when the proxy is set.
fun org.gradle.api.artifacts.dsl.RepositoryHandler.standardMavenRepositories() {
  mavenLocal()
  if (optionalMavenProxyFromGradleLocal != null) {
    maven(optionalMavenProxyFromGradleLocal)
  } else {
    mavenCentral()
  }
}

allprojects {
  repositories {
    standardMavenRepositories()
  }

  apply<JavaLibraryPlugin>()

  java {
    // Drives sourceCompatibility, targetCompatibility and `javac --release` in one place.
    // Auto-provisioned via Foojay (see settings.gradle.kts) when no matching JDK is present locally,
    // so the Gradle process JVM no longer has to match the bytecode target.
    toolchain {
      languageVersion.set(JavaLanguageVersion.of(targetJavaVersion.majorVersion.toInt()))
    }
  }

  // String interpolation support, see https://github.com/antkorwin/better-strings.
  // This needs to be enabled in each subproject by default because there's going to be no warning
  // if this annotation processor isn't run in any subproject (the strings will be just interpreted
  // verbatim, without interpolation applied).
  // In such case, we'd only capture an unprocessed interpolation in ArchUnit tests by analyzing constant pools of classes.
  betterStrings()

  tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(
      listOf(
        // Treat each compiler warning (esp. the ones coming from Checker Framework) as an error.
        "-Werror",
        // Warn of type-unsafe operations on generics.
        "-Xlint:unchecked",
      ),
    )

    options.isFork = true

    // No need to set `options.release` here: the Java toolchain (configured in `allprojects { java { toolchain { ... } } }`)
    // pins the compiler JDK to the same major as the bytecode target, so the available Java APIs match by construction.
  }

  tasks.withType<Javadoc> {
    // See JDK-8200363 (https://bugs.openjdk.java.net/browse/JDK-8200363) for information about the `-Xwerror` option:
    // this is needed to make sure that javadoc always fails on warnings
    // (esp. important on CI since javadoc there for some reason seems to never raise any errors otherwise).

    // The '-quiet' as second argument is actually a hack around
    // https://github.com/gradle/gradle/issues/2354:
    // since the one-parameter `addStringOption` doesn't seem to work, we need to add an extra
    // `-quiet`, which is added anyway by Gradle.
    (options as StandardJavadocDocletOptions).addStringOption("Xwerror", "-quiet")
    // Suppress `doclint` for `missing`; otherwise javadoc for every member would be required.
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:all,-missing", "-quiet")
    options.quiet()
  }

  tasks.withType<Test> {
    useJUnitPlatform()

    if (project.properties["forceRunTests"] != null) {
      outputs.upToDateWhen { false }
    }

    testLogging {
      if (project.properties["printTestOutput"] != null) {
        showStandardStreams = true
      }

      exceptionFormat = TestExceptionFormat.FULL
      showCauses = true
      showExceptions = true
      showStackTraces = true
    }
  }

  configureCheckerFramework()
  configureCheckstyle()
  configureSpotless()

  // A few libraries (like JGit) transitively pull in a version of slf4j-api
  // that might be different from the slf4j-api version that IntelliJ depends on.
  // SLF4J guarantees that the code compiled against a certain slf4j-api version will work with any
  // other version of slf4j-api (http://www.slf4j.org/manual.html#compatibility).
  // We rely on that guarantee: our plugin effectively uses whatever slf4j-api version is provided by IntelliJ.
  // SLF4J does NOT guarantee, however, that slf4j-api version X will work with any slf4j
  // implementation version Y for X != Y.
  // To avoid a clash between JGit&co.'s slf4j-api and Intellij's slf4j implementation
  // (and also between JGit&co.'s slf4j-api and Intellij's slf4j-api), we need to exclude the former
  // from ALL dependencies.
  configurations.runtimeClasspath { exclude(group = "org.slf4j", module = "slf4j-api") }

  tasks.withType<KotlinCompile> {
    val kotlinVersionStr = intellijVersions.kotlinVersion.replace("""^(\d+\.\d+).*""".toRegex(), "$1")
    val kotlinVersion = GradleKotlinVersion.fromVersion(kotlinVersionStr)
    compilerOptions {
      apiVersion.set(kotlinVersion)
      languageVersion.set(kotlinVersion)
    }
  }
}

subprojects {
  // This is necessary to make sure that `buildPlugin` task puts jars of all relevant subprojects
  // into the final zip.
  // No need to include near-empty (only with META-INF/MANIFEST.MF) jars
  // for subprojects that don't have any production code.
  if (sourceSets["main"].allSource.srcDirs.any { it.exists() }) {
    rootProject.dependencies { implementation(project) }
  }

  // By default, the jar name will be formed only from the last segment of subproject path.
  // Since these last segments are NOT unique (there are many `api`s and `impl`s),
  // the effective jar name will be something like api.jar, api_1.jar, api_2.jar etc.,
  // which is suboptimal.
  // Let's use full name like frontend-ui-api.jar instead.
  base.archivesName.set(path.replaceFirst(":", "").replace(":", "-"))

  if (path.startsWith(":frontend:")) {
    // We use `.base` rather than `.module` on purpose: `.base` provides exactly what frontend
    // subprojects need (the `intellijPlatform { ... }` dependencies/repositories DSL and IJ platform
    // jars on the compile classpath via `compileOnly`) without any of the stuff that `.module` adds
    // on top - most notably the `composedJar`/`instrumentedJar` tasks, the `-base` archive classifier
    // from `JarCompanion`, and (since intellij-platform-gradle-plugin 2.14.0) the auto-inference that
    // treats every `ProjectDependency` into a "pure module project" as a `pluginModule(...)` entry and
    // packages the resulting jar into `lib/modules/` in the final plugin zip.
    // That last behavior is incompatible with our flat (v1) plugin.xml layout: classes under
    // `lib/modules/` are not on the plugin's main runtime classpath unless declared as v2
    // `<content><module .../></content>`, which we don't use.
    apply(plugin = "org.jetbrains.intellij.platform.base")

    applyGuiEffectChecker()

    repositories {
      standardMavenRepositories()
      intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
      }
    }

    dependencies {
      intellijPlatform {
        intellijIdea(intellijVersions.buildTarget)
        bundledPlugin("Git4Idea")
      }
    }

    // The `.base` plugin extends `compileOnly`/`testCompileOnly` from the IJ platform
    // configurations, so IJ classes are visible at compile time. It does NOT, however,
    // put them on the `testRuntimeClasspath` - the `.module` plugin normally does that
    // indirectly by re-registering the `test` task via `TestCompanion`/`TestIdeTask`
    // (see `TestIdeTask.configuration` in intellij-platform-gradle-plugin), which
    // explicitly sets `classpath = files(..., intellijPlatformTestClasspath, ...)`.
    // Since we're not using `.module`, we wire the IJ platform test classpath (a
    // resolvable configuration created by `.base`, transitively extending from
    // `intellijPlatform`/`intellijPlatformPlugins`/`intellijPlatformBundledPlugins`/
    // `intellijPlatformBundledModules`) onto the plain Gradle `test` task ourselves.
    tasks.withType<Test>().configureEach {
      classpath += configurations["intellijPlatformTestClasspath"]
    }
  }
}

// Root project config

group = "com.virtuslab"

configureVersionFromGit()

repositories {
  standardMavenRepositories()
  intellijPlatform {
    defaultRepositories()
    jetbrainsRuntime()
  }
}

// This task should not be used - we don't use the "Unreleased" section anymore
project.gradle.startParameter.excludedTaskNames.add("patchChangeLog")

changelog {
  val prospectiveReleaseVersion: String by extra
  version.set("v$prospectiveReleaseVersion")
  headerParserRegex.set(Regex("""v\d+\.\d+\.\d+"""))
  path.set("${project.projectDir}/CHANGE-NOTES.md")
}

val verifyVersionTask = tasks.register("verifyChangeLogVersion") {
  doLast {
    val prospectiveVersionSection = changelog.version.get()
    val latestVersionSection = changelog.getLatest()

    if (prospectiveVersionSection != latestVersionSection.version) {
      throw Exception(
        "$prospectiveVersionSection is not the latest in CHANGE-NOTES.md, " +
          "update the file or change the prospective version in version.gradle.kts",
      )
    }
  }
}

val verifyContentsTask = tasks.register("verifyChangeLogContents") {
  doLast {
    val prospectiveVersionSection = changelog.get(changelog.version.get())

    val renderItemStr = changelog.renderItem(prospectiveVersionSection)
    if (renderItemStr.isBlank()) {
      throw Exception("${prospectiveVersionSection.version} section is empty, update CHANGE-NOTES.md")
    }

    val listingElements = renderItemStr.split(System.lineSeparator()).drop(1)
    for (line in listingElements) {
      if (line.isNotBlank() && !line.startsWith("- ") && !line.startsWith("  ")) {
        throw Exception(
          "Update formatting in CHANGE-NOTES.md ${prospectiveVersionSection.version} section:" +
            "${System.lineSeparator()}$line",
        )
      }
    }
  }
}

tasks.register("verifyChangeLog") {
  dependsOn(verifyVersionTask, verifyContentsTask)
}

tasks.register("printPluginZipPath") {
  doLast {
    val buildPlugin = tasks.findByPath(":buildPlugin")!! as BuildPluginTask
    println(buildPlugin.archiveFile.get().asFile.path)
  }
}

tasks.register("printSignedPluginZipPath") {
  // Querying the mapped value of map(task ':signPlugin' property 'archiveFile')
  // before task ':buildPlugin' has completed is not supported
  dependsOn(":buildPlugin")

  doLast {
    val signPlugin = tasks.findByPath(":signPlugin")!! as SignPluginTask
    println(signPlugin.signedArchiveFile.get().asFile.path)
  }
}

val verifyPluginZipTask = tasks.register("verifyPluginZip") {
  val buildPlugin = tasks.findByPath(":buildPlugin")!! as BuildPluginTask
  dependsOn(buildPlugin)

  doLast {
    val pluginZipPath = buildPlugin.archiveFile.get().asFile.path
    val jarsInPluginZip = ZipFile(pluginZipPath).use { zf ->
      zf.stream()
        .map(ZipEntry::getName)
        .map { it.removePrefix("git-machete-intellij-plugin/").removePrefix("lib/").removeSuffix(".jar") }
        .filter { it.isNotEmpty() }
        .toList()
    }

    for (proj in subprojects) {
      val projJar = proj.path.replaceFirst(":", "").replace(":", "-")
      val javaExtension = proj.extensions.findByType<JavaPluginExtension>()
      val hasSourceCode = javaExtension?.sourceSets?.get("main")?.allSource?.srcDirs?.any { it.exists() } ?: false

      if (hasSourceCode) {
        check(projJar in jarsInPluginZip) {
          "$projJar.jar was expected in plugin zip ($pluginZipPath) but was NOT found" +
            "\nAll entries: $jarsInPluginZip"
        }
      } else {
        check(projJar !in jarsInPluginZip) {
          "$projJar.jar was NOT expected in plugin zip ($pluginZipPath) but was found" +
            "\nAll entries: $jarsInPluginZip"
        }
      }
    }

    val expectedLibs = listOf("org.eclipse.jgit", "slf4j-lambda-core", "vavr", "vavr-match")
    for (expectedLib in expectedLibs) {
      val libRegexStr = "^" + expectedLib.replace(".", "\\.") + "-[0-9.]+.*$"
      check(jarsInPluginZip.any { it.matches(libRegexStr.toRegex()) }) {
        "A jar for $expectedLib was expected in plugin zip ($pluginZipPath) but was NOT found\nAll entries: $jarsInPluginZip"
      }
    }

    val forbiddenLibPrefixes = listOf("idea", "kotlin", "lombok", "remote-robot", "slf4j")
    for (jar in jarsInPluginZip) {
      check(forbiddenLibPrefixes.none { jar.startsWith(it) } || expectedLibs.any { jar.startsWith(it) }) {
        "$jar.jar was NOT expected in plugin zip ($pluginZipPath) but was found\nAll entries: $jarsInPluginZip"
      }
    }
  }
}

tasks.named<Zip>("buildPlugin") {
  dependsOn(verifyVersionTask)
  finalizedBy(verifyPluginZipTask)
}

intellijPlatform {
  buildSearchableOptions = false
  instrumentCode = false

  pluginConfiguration {
    name = "Git Machete"
    // Note that the first line of the description should be self-contained since it is placed into embeddable card:
    // see e.g. https://plugins.jetbrains.com/search?search=git%20machete
    description = file("$rootDir/DESCRIPTION.html").readText()

    val changelogItem = changelog.getOrNull(changelog.version.get())
    if (changelogItem != null) {
      changeNotes = changelog.renderItem(changelogItem, Changelog.OutputType.HTML)
    }

    ideaVersion {
      // `sinceBuild` is exclusive when we are using `*` in version but inclusive when without `*`
      sinceBuild = intellijVersions.earliestSupportedMajor.toBuildNumber().value
      // In `untilBuild` situation is inverted: it's inclusive when using `*` but exclusive when without `*`
      untilBuild = intellijVersions.latestSupportedMajor.toBuildNumber().value + ".*"
    }
  }

  signing {
    certificateChain = pluginSignCertificateChain?.trimIndent()
    privateKey = pluginSignPrivateKey?.trimIndent()
    password = pluginSignPrivateKeyPass
  }

  publishing {
    token = jetbrainsMarketplaceToken
  }

  pluginVerification {
    ides {
      // This could also be handled by `recommended()` DSL,
      // but with this explicit approach, the IDE versions used for verification
      // are fully controlled by repository contents (intellij-versions.properties),
      // so the builds are more reproducible in this respect.
      val maybeEap = listOfNotNull(intellijVersions.upcomingMajorEap)
      val ideVersions = intellijVersions.latestMinorsOfOldSupportedMajors + intellijVersions.latestStable + maybeEap
      ideVersions.map { it.value }.forEach {
        create("IU", it)
      }
    }
    failureLevel.set(
      setOf(
        VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS,
        VerifyPluginTask.FailureLevel.NON_EXTENDABLE_API_USAGES,
        VerifyPluginTask.FailureLevel.PLUGIN_STRUCTURE_WARNINGS,
        VerifyPluginTask.FailureLevel.MISSING_DEPENDENCIES,
      ),
    )
  }
}

tasks.runIde {
  jvmArgs("-Xmx20G")
}

dependencies {
  intellijPlatform {
    intellijIdea(intellijVersions.buildTarget)
    bundledPlugin("Git4Idea")
    pluginVerifier()
    zipSigner()
  }
}

applyKotlinConfig()
archunit()
// Checker is needed in root project runtime (not just compile-time) classpath for ArchUnit tests
checkerQual("test")
jgit("test")
junit()
lombok("test")
vavr("test")

val uiTest = sourceSets.create("uiTest")
val uiTestImplementation by configurations.getting
val uiTestRuntimeOnly by configurations.getting
val robotServerPluginZip by configurations.creating

repositories {
  maven {
    url = URI("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
  }
}

dependencies {
  intellijPlatform {
    // Note that theoretically, we should compile UI tests for each IDE version
    // against test framework (ide-starter, driver-sdk etc.) for this particular version,
    // as there's no guarantee that test framework version X will be compatible with IDE version Y for X != Y.
    // See https://youtrack.jetbrains.com/issue/IJPL-234281.
    // We're cutting corners here to keep the build setup simpler.
    testFramework(TestFrameworkType.Starter, configurationName = uiTestImplementation.name)
  }

  junit("uiTest")
  uiTestImplementation(testFixtures(project(":testCommon")))
  uiTestImplementation(libs.kodein)
  uiTestImplementation(libs.okhttp)
  uiTestImplementation(libs.remoteRobot.client)
  uiTestRuntimeOnly(libs.kotlin.coroutines)
  // Required at runtime by `com.intellij.platform.testFramework.teamCity.TeamCityReporter`,
  // which `LocalIDEProcess` invokes to emit per-test progress to the surrounding CI process.
  // Not pulled in transitively via `TestFrameworkType.Starter`, so we add it explicitly.
  uiTestRuntimeOnly(libs.teamcity.serviceMessages)

  robotServerPluginZip(libs.remoteRobot.serverPlugin) {
    artifact {
      type = "zip"
    }
  }
}

configureUiTests()
