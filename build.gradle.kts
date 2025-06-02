
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
        intellijIdeaCommunity("252.18003.27")
        bundledPlugin("Git4Idea")
      }
    }
}
