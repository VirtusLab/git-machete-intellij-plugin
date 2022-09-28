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

project.extensions.add(
  "intellijVersions",
  IntellijVersions(IntellijVersionHelper.getProperties(), project.properties["overrideBuildTarget"] as String?)
)

apply<GradleVersionsFilterPlugin>()
apply<VersionCatalogUpdatePlugin>()
apply<TaskTreePlugin>()

if (JavaVersion.current() != JavaVersion.VERSION_11 && JavaVersion.current() != JavaVersion.VERSION_17) {
  throw GradleException(
    """Project must be built with JDK 11 or 17 since:
    1. as of v3.24, Checker Framework only supports JDK 8, 11 and 17: https://checkerframework.org/manual/#installation
    2. codebase is Java 11-compatible, so can't be built on JDK 8"""
  )
}

fun getFlagsForAddOpens(vararg packages: String, module: String): List<String> {
  return packages.toList().map { "--add-opens=$module/$it=ALL-UNNAMED" }
}

val javaMajorVersion: JavaVersion by extra(JavaVersion.VERSION_11)

val ciBranch: String? by extra(System.getenv("CIRCLE_BRANCH"))
val isCI: Boolean by extra(System.getenv("CI") == "true")
val jetbrainsMarketplaceToken: String? by extra(System.getenv("JETBRAINS_MARKETPLACE_TOKEN"))

fun String.fromBase64(): String {
  return String(Base64.getDecoder().decode(this))
}
val pluginSignCertificateChain: String? by extra(System.getenv("PLUGIN_SIGN_CERT_CHAIN_BASE64")?.fromBase64())
val pluginSignPrivateKey: String? by extra(System.getenv("PLUGIN_SIGN_PRIVATE_KEY_BASE64")?.fromBase64())
val pluginSignPrivateKeyPass: String? by extra(System.getenv("PLUGIN_SIGN_PRIVATE_KEY_PASS"))

val compileJavaJvmArgs: List<String>? by extra((project.properties["compileJavaJvmArgs"] as String?)?.split(" "))
val shouldRunAllCheckers: Boolean by extra(isCI || project.hasProperty("runAllCheckers"))

tasks.register<UpdateIntellijVersions>("updateIntellijVersions")

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
    sourceCompatibility = javaMajorVersion
    targetCompatibility = javaMajorVersion // redundant, added for clarity
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
    options.release.set(Integer.parseInt(javaMajorVersion.majorVersion))
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
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:all", "-quiet")
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
