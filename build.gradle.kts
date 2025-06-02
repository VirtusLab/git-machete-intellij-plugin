
import com.virtuslab.gitmachete.buildsrc.*
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  checkstyle
  `java-library`
  scala
  alias(libs.plugins.jetbrains.intellij)
}

val targetJavaVersion: JavaVersion by extra(JavaVersion.VERSION_17)

val intellijVersions by extra(
  IntellijVersions.from(
    intellijVersionsProperties = PropertiesHelper.getProperties(rootDir.resolve("intellij-versions.properties")),
    overrideBuildTarget = project.properties["overrideBuildTarget"] as String?,
  ),
)

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
    testLogging {
      exceptionFormat = TestExceptionFormat.FULL
      showCauses = true
      showExceptions = true
      showStackTraces = true
    }
  }
}

subprojects {
    apply(plugin = "org.jetbrains.intellij.platform.module")

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

// Root project config

group = "com.virtuslab"

repositories {
  mavenCentral()
  intellijPlatform {
    defaultRepositories()
    jetbrainsRuntime()
  }
}

intellijPlatform {
  pluginConfiguration {
    name = "Git Machete"
    // Note that the first line of the description should be self-contained since it is placed into embeddable card:
    // see e.g. https://plugins.jetbrains.com/search?search=git%20machete
    description = file("$rootDir/DESCRIPTION.html").readText()

    ideaVersion {
      // `sinceBuild` is exclusive when we are using `*` in version but inclusive when without `*`
      sinceBuild = IntellijVersionHelper.versionToBuildNumber(intellijVersions.earliestSupportedMajor)
      // In `untilBuild` situation is inverted: it's inclusive when using `*` but exclusive when without `*`
      untilBuild = IntellijVersionHelper.versionToBuildNumber(intellijVersions.latestSupportedMajor) + ".*"
    }
  }
}

dependencies {
  intellijPlatform {
    intellijIdeaCommunity(intellijVersions.buildTarget)
  }
}
