
import com.virtuslab.gitmachete.buildsrc.*
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.BuildPluginTask
import org.jetbrains.intellij.platform.gradle.tasks.SignPluginTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

plugins {
  checkstyle
  `java-library`
  scala
  alias(libs.plugins.jetbrains.changelog)
  alias(libs.plugins.jetbrains.intellij)
  alias(libs.plugins.taskTree)
}

fun getFlagsForAddExports(vararg packages: String, module: String): List<String> = packages.toList().map { "--add-exports=$module/$it=ALL-UNNAMED" }

val targetJavaVersion: JavaVersion by extra(JavaVersion.VERSION_17)

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

val configCheckerDirectory: String by extra(rootProject.file("config/checker").path)

allprojects {
  repositories {
    mavenLocal()
    mavenCentral()
  }

  apply<JavaLibraryPlugin>()

  java {
    sourceCompatibility = targetJavaVersion
    targetCompatibility = targetJavaVersion // redundant, added for clarity
  }

  // String interpolation support, see https://github.com/antkorwin/better-strings.
  // This needs to be enabled in each subproject by default because there's going to be no warning
  // if this annotation processor isn't run in any subproject (the strings will be just interpreted
  // verbatim, without interpolation applied).
  // Otherwise, we'd only capture an unprocessed interpolation in ArchUnit tests by analyzing constant pools of classes.
  betterStrings()

  tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(
      listOf(
        // Enforce explicit `.toString()` call in code generated for string interpolations
        "-AcallToStringExplicitlyInInterpolations",
        // Treat each compiler warning (esp. the ones coming from Checker Framework) as an error.
        "-Werror",
        // Warn of type-unsafe operations on generics.
        "-Xlint:unchecked",
      ),
    )

    options.isFork = true
    // Required for better-strings to work under Java 17: https://github.com/antkorwin/better-strings/issues/21
    options.forkOptions.jvmArgs?.addAll(
      getFlagsForAddExports(
        "com.sun.tools.javac.api",
        "com.sun.tools.javac.code",
        "com.sun.tools.javac.processing",
        "com.sun.tools.javac.tree",
        "com.sun.tools.javac.util",
        module = "jdk.compiler",
      ),
    )

    // `sourceCompatibility` and `targetCompatibility` say nothing about the Java APIs available to the compiled code.
    // In fact, for X < Y it's perfectly possible to compile Java X code that uses Java Y APIs...
    // This will work fine, until we actually try to run those compiled classes under Java X-compatible JVM,
    // when we'll end up with NoSuchMethodError for APIs added between Java X and Java Y
    // (i.e. for X=8 and Y=11: InputStream#readAllBytes, Stream#takeWhile and String#isBlank).
    // `options.release = X` makes sure that regardless of Java version used to run the compiler,
    // only Java X-compatible APIs are available to the compiled code.
    options.release.set(Integer.parseInt(targetJavaVersion.majorVersion))

    // Add files from config/checker directory as inputs to java compilation (so that changes trigger recompilation).
    // These files are config files for the Checker Framework, which is for Java exclusively.
    inputs.dir(configCheckerDirectory)
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

  tasks.withType<KotlinCompile>().configureEach {
    val kotlinLanguageVersion = intellijVersions.earliestSupportedMajorKotlinVersion
    kotlinOptions {
      apiVersion = kotlinLanguageVersion
      languageVersion = kotlinLanguageVersion
    }
  }
}

subprojects {
  // This is necessary to make sure that `buildPlugin` task puts jars of all relevant subprojects
  // into the final zip.
  // No need to include near-empty (only with META-INF/MANIFEST.MF) jars
  // for subprojects that don't have any production code.
  if (sourceSets["main"].allSource.srcDirs.any { it?.exists() == true }) {
    rootProject.dependencies { implementation(project) }
  }

  // By default, the jar name will be formed only from the last segment of subproject path.
  // Since these last segments are NOT unique (there are many `api`s and `impl`s),
  // the effective jar name will be something like api.jar, api_1.jar, api_2.jar etc.,
  // which is suboptimal.
  // Let's use full name like frontend-ui-api.jar instead.
  base.archivesName.set(path.replaceFirst(":", "").replace(":", "-"))

  if (path.startsWith(":frontend:") && path != ":frontend:resourcebundles") {
    apply(plugin = "org.jetbrains.intellij.platform.module")

    applyGuiEffectChecker()

    repositories {
      mavenCentral()
      intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
      }
    }
    dependencies {
      intellijPlatform {
        intellijIdeaCommunity(intellijVersions.buildTarget)
        bundledPlugin("Git4Idea")
      }
    }
    intellijPlatform {
      // It only affects searchability of plugin-specific settings (which we don't provide so far).
      // Actions remain searchable anyway.
      // TODO (#270): to be re-enabled (at least in CI) once we provide custom settings
      buildSearchableOptions = false

      instrumentCode = false
    }
  }
}

// Root project config

group = "com.virtuslab"

configureVersionFromGit()

repositories {
  mavenCentral()
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
      val javaExtension = proj.extensions.getByType<JavaPluginExtension>()
      if (javaExtension.sourceSets["main"].allSource.srcDirs.any { it?.exists() ?: false }) {
        check(projJar in jarsInPluginZip) {
          "$projJar.jar was expected in plugin zip ($pluginZipPath) but was NOT found"
        }
      } else {
        check(projJar !in jarsInPluginZip) {
          "$projJar.jar was NOT expected in plugin zip ($pluginZipPath) but was found"
        }
      }
    }

    val expectedLibs = listOf("org.eclipse.jgit", "slf4j-lambda-core", "vavr", "vavr-match")
    for (expectedLib in expectedLibs) {
      val libRegexStr = "^" + expectedLib.replace(".", "\\.") + "-[0-9.]+.*$"
      check(jarsInPluginZip.any { it.matches(libRegexStr.toRegex()) }) {
        "A jar for $expectedLib was expected in plugin zip ($pluginZipPath) but was NOT found"
      }
    }

    val forbiddenLibPrefixes = listOf("ide-probe", "idea", "kotlin", "lombok", "remote-robot", "scala", "slf4j")
    for (jar in jarsInPluginZip) {
      check(forbiddenLibPrefixes.none { jar.startsWith(it) } || expectedLibs.any { jar.startsWith(it) }) {
        "$jar.jar was NOT expected in plugin zip ($pluginZipPath) but was found"
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
      sinceBuild = IntellijVersionHelper.versionToBuildNumber(intellijVersions.earliestSupportedMajor)
      // In `untilBuild` situation is inverted: it's inclusive when using `*` but exclusive when without `*`
      untilBuild = IntellijVersionHelper.versionToBuildNumber(intellijVersions.latestSupportedMajor) + ".*"
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
      val maybeEap = listOfNotNull(intellijVersions.eapOfLatestSupportedMajor)
      val ideVersions = intellijVersions.latestMinorsOfOldSupportedMajors + intellijVersions.latestStable + maybeEap
      ides(ideVersions)
    }
  }
}

dependencies {
  intellijPlatform {
    intellijIdeaCommunity(intellijVersions.buildTarget)
    bundledPlugin("Git4Idea")
    pluginVerifier()
    zipSigner()
  }
}

val oldUiTest = sourceSets.create("oldUiTest")
val oldUiTestImplementation: Configuration by configurations.getting { extendsFrom(configurations.testImplementation.get()) }
// This configuration apparently needs to be defined explicitly (despite not being used explicitly anywhere)
// so that old UI test runtime classpath inherits `testRuntimeOnly` dependencies of the root project.
val oldUiTestRuntimeOnly: Configuration by configurations.getting { extendsFrom(configurations.testRuntimeOnly.get()) }
configureOldUiTests()
dependencies {
  oldUiTestImplementation(testFixtures(project(":testCommon")))
  compileOnly(libs.scalaLibrary) // only needed to prevent IntelliJ loading error
}

applyKotlinConfig()
archunit()
// Checker is needed in root project runtime (not just compile-time) classpath for ArchUnit tests
checkerQual("test")
ideProbe()
jgit("test")
junit()
lombok("test")
vavr("test")

// This is needed solely for ArchUnit tests that detect unprocessed string interpolations
// to access constant pools of classes.
tasks.withType<Test> {
  jvmArgs(getFlagsForAddExports("jdk.internal.reflect", module = "java.base"))
}

val uiTest = sourceSets.create("uiTest") {
  compileClasspath += sourceSets.main.get().output
  runtimeClasspath += sourceSets.main.get().output
}

val uiTestImplementation by configurations.getting {
  extendsFrom(configurations.testImplementation.get())
}

val uiTestRuntimeOnly by configurations.getting {
  extendsFrom(configurations.testRuntimeOnly.get())
}

val robotServerPluginZip by configurations.creating

repositories {
  maven {
    url = URI("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
  }
}

dependencies {
  intellijPlatform {
    testFramework(TestFrameworkType.Starter, configurationName = uiTestImplementation.name)
  }

  uiTestImplementation(testFixtures(project(":testCommon")))
  uiTestImplementation(libs.junit.api)
  uiTestImplementation(libs.kodein)
  uiTestImplementation(libs.okhttp)
  uiTestImplementation(libs.remoteRobot.client)

  uiTestRuntimeOnly(libs.junit.platformLauncher)
  uiTestRuntimeOnly(libs.kotlin.coroutines)

  robotServerPluginZip(libs.remoteRobot.serverPlugin) {
    artifact {
      type = "zip"
    }
  }
}

val uiTestTargetVersions: List<String> =
  if (project.properties["against"] != null) {
    intellijVersions.resolveIntelliJVersions(project.properties["against"] as? String)
  } else {
    listOf(intellijVersions.buildTarget)
  }

uiTestTargetVersions.onEach { version ->
  tasks.register<Test>("uiTest_$version") {
    description = "Runs UI tests."
    group = "verification"

    systemProperty("intellij.version", version.replace("-EAP-SNAPSHOT", ""))

    testClassesDirs = uiTest.output.classesDirs
    classpath = configurations["uiTestRuntimeClasspath"] + uiTest.output

    val buildPlugin = tasks.findByPath(":buildPlugin")!!
    dependsOn(buildPlugin)
    systemProperty("path.to.build.plugin", buildPlugin.outputs.files.singleFile.path)

    dependsOn(robotServerPluginZip)
    systemProperty("path.to.robot.server.plugin", robotServerPluginZip.singleFile.path)

    if (!isCI) {
      outputs.upToDateWhen { false }
    }

    val testFilter = project.properties["tests"]
    if (testFilter != null) {
      filter { includeTestsMatching("*.*$testFilter*") }
    }

    useJUnitPlatform()
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    testLogging.showStandardStreams = true
  }
}

tasks.register("uiTest") {
  dependsOn(tasks.matching { task -> task.name.startsWith("uiTest_") })
}
