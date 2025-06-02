
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
  `java-library`
  id("org.jetbrains.intellij.platform") version "2.6.0"
}

allprojects {
  repositories {
    mavenLocal()
    mavenCentral()
  }

  apply<JavaLibraryPlugin>()

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
        // 2025.1.1.1 - no problem
        intellijIdeaCommunity("252.13776.59")
        bundledPlugin("Git4Idea")
      }
    }
}
