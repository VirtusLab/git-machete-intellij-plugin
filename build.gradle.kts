
import com.virtuslab.gitmachete.buildsrc.*
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
  `java-library`
  id("org.jetbrains.intellij.platform") version "2.6.0"
}

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
