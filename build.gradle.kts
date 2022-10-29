import com.dorongold.gradle.tasktree.TaskTreePlugin
import com.virtuslab.gitmachete.buildsrc.*
import nl.littlerobots.vcu.plugin.VersionCatalogUpdateExtension
import nl.littlerobots.vcu.plugin.VersionCatalogUpdatePlugin
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import se.ascp.gradle.GradleVersionsFilterPlugin
import java.util.Base64

plugins {
  checkstyle
  `java-library`
  scala
}

apply<GradleVersionsFilterPlugin>()
apply<VersionCatalogUpdatePlugin>()
apply<TaskTreePlugin>()

fun getFlagsForAddOpens(vararg packages: String, module: String): List<String> {
  return packages.toList().map { "--add-opens=$module/$it=ALL-UNNAMED" }
}

// TODO (#859): bump to Java 17 once we no longer support InteliJ 2022.1 (the last version to run on Java 11)
val targetJavaVersion: JavaVersion by extra(JavaVersion.VERSION_11)
// Since 2022.3, IntelliJ itself is compiled for Java 17 (classfiles version 44+17=61).
// 2022.2 is apparently compiled for Java 11 (classfiles version 44+11=55), but running on JBR 17 by default.
val requiredJdkVersion: JavaVersion by extra(JavaVersion.VERSION_17)

val ciBranch: String? by extra(System.getenv("CIRCLE_BRANCH"))
val isCI: Boolean by extra(System.getenv("CI") == "true")
val jetbrainsMarketplaceToken: String? by extra(System.getenv("JETBRAINS_MARKETPLACE_TOKEN"))

val intellijVersions by extra(
  IntellijVersions.from(
    intellijVersionsProperties = PropertiesHelper.getProperties(File("intellijVersions.properties")),
    overrideBuildTarget = project.properties["overrideBuildTarget"] as String?
  )
)

fun String.fromBase64(): String {
  return String(Base64.getDecoder().decode(this))
}
val pluginSignCertificateChain: String? by extra(System.getenv("PLUGIN_SIGN_CERT_CHAIN_BASE64")?.fromBase64())
val pluginSignPrivateKey: String? by extra(System.getenv("PLUGIN_SIGN_PRIVATE_KEY_BASE64")?.fromBase64())
val pluginSignPrivateKeyPass: String? by extra(System.getenv("PLUGIN_SIGN_PRIVATE_KEY_PASS"))

val compileJavaJvmArgs: List<String>? by extra((project.properties["compileJavaJvmArgs"] as String?)?.split(" "))
val shouldRunAllCheckers: Boolean by extra(isCI || project.hasProperty("runAllCheckers"))

tasks.register<UpdateIntellijVersions>("updateIntellijVersions")

tasks.register("printPluginZipPath") {
  doLast {
    val buildPlugin = tasks.findByPath(":buildPlugin")!!
    println(buildPlugin.outputs.files.first().path)
  }
}
tasks.register("printSignedPluginZipPath") {
  doLast {
    val signPlugin = tasks.findByPath(":signPlugin")!!
    println(signPlugin.outputs.files.first().path)
  }
}

val configCheckerDirectory: String by extra(rootProject.file("config/checker").path)

configure<VersionCatalogUpdateExtension> {
  sortByKey.set(false)

  // TODO (ben-manes/gradle-versions-plugin#284): `versionCatalogUpdate` should work on both the project and project's buildSrc
  //  The `keep` settings are needed so that a `versionCatalogUpdate` on the project doesn't remove the dependencies of buildSrc
  keep {
    keepUnusedVersions.set(true)
    keepUnusedLibraries.set(true)
    keepUnusedPlugins.set(true)
  }
}

allprojects {
  repositories {
    mavenLocal()
    mavenCentral()
  }

  apply<JavaLibraryPlugin>()

  java {
    toolchain {
      languageVersion.set(JavaLanguageVersion.of(requiredJdkVersion.toString()))
    }

    sourceCompatibility = targetJavaVersion
    targetCompatibility = targetJavaVersion // redundant, added for clarity
  }

  // String interpolation support, see https://github.com/antkorwin/better-strings
  // This needs to be enabled in each subproject by default because there's going to be no warning
  // if this annotation processor isn't run in any subproject (the strings will be just interpreted
  // verbatim, without interpolation applied).
  // We'd only capture that in CI's post-compile checks by analyzing constants in class files.
  dependencies {
    annotationProcessor(rootProject.libs.betterStrings)
    testAnnotationProcessor(rootProject.libs.betterStrings)
  }

  tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(
      listOf(
        // Enforce explicit `.toString()` call in code generated for string interpolations
        "-AcallToStringExplicitlyInInterpolations",
        // Treat each compiler warning (esp. the ones coming from Checker Framework) as an error.
        "-Werror",
        // Warn of type-unsafe operations on generics.
        "-Xlint:unchecked"
      )
    )

    options.isFork = true
    options.forkOptions.jvmArgs?.addAll(
      compileJavaJvmArgs
        ?: (if (isCI) listOf() else listOf("-Xmx6G", "-XX:+HeapDumpOnOutOfMemoryError"))
    )
    // Required for better-strings to work under Java 17: https://github.com/antkorwin/better-strings/issues/21
    options.forkOptions.jvmArgs?.addAll(
      getFlagsForAddOpens(
        "com.sun.tools.javac.api",
        "com.sun.tools.javac.code",
        "com.sun.tools.javac.processing",
        "com.sun.tools.javac.tree",
        "com.sun.tools.javac.util",
        module = "jdk.compiler"
      )
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
    // Required for PowerMock to work under Java 17
    jvmArgs(
      getFlagsForAddOpens(
        "java.io",
        "java.lang",
        "java.nio.file",
        "java.util.stream",
        module = "java.base"
      )
    )

    if (project.properties["forceRunTests"] != null) {
      outputs.upToDateWhen { false }
    }

    testLogging {
      events = setOf(TestLogEvent.FAILED)
      if (project.properties["printTestOutput"] != null) {
        events.addAll(setOf(TestLogEvent.STANDARD_OUT, TestLogEvent.STANDARD_ERROR))
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

  // A few libraries (like JGit and reflections) transitively pull in a version of slf4j-api
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
}

// Root project config

group = "com.virtuslab"

configureVersionFromGit()

configureIntellijPlugin()

val uiTest = sourceSets.create("uiTest")
val uiTestImplementation: Configuration by configurations.getting { extendsFrom(configurations.testImplementation.get()) }
val uiTestRuntimeOnly: Configuration by configurations.getting { extendsFrom(configurations.testRuntimeOnly.get()) }

configureUiTests()

applyKotlinConfig()
ideProbe()
lombok()
dependencies {
  // Checker is needed for root project runtime (not just compile-time) classpath for ArchUnit tests
  testImplementation(rootProject.libs.checker.qual)
  testImplementation(rootProject.libs.archunit)
}
