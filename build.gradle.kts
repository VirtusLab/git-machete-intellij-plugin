
import com.virtuslab.gitmachete.buildsrc.*
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
  `java-library`
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
