
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
