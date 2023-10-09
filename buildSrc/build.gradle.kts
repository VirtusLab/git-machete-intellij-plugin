import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Not worth using Gradle toolchains, they don't seem to work as expected for buildSrc (or are just hard to configure properly).
// Let the developers install sdkman to switch Java versions instead.
if (JavaVersion.current() != JavaVersion.VERSION_17) {
  throw GradleException("This build must be run under Java 17. Consider using sdkman with .sdkmanrc file for easily switching Java versions.")
}

plugins {
  `kotlin-dsl`
  alias(libs.plugins.taskTree)
}

// This is needed to use kotlin language version different from the default (1.4).
// See https://handstandsam.com/2022/04/13/using-the-kotlin-dsl-gradle-plugin-forces-kotlin-1-4-compatibility/.
afterEvaluate {
  tasks.withType<KotlinCompile>().configureEach {
    val kotlinLanguageVersion = libs.pluginPackages.jetbrains.kotlin
      .get().versionConstraint.requiredVersion.replace("\\.\\d+$".toRegex(), "")

    kotlinOptions {
      apiVersion = kotlinLanguageVersion
      languageVersion = kotlinLanguageVersion
    }
  }
}

buildscript {
  repositories {
    gradlePluginPortal()
  }
  dependencies {
    classpath(libs.pluginPackages.jetbrains.kotlin)
    classpath(libs.pluginPackages.spotless)
  }
}

repositories {
  mavenLocal()
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  implementation(libs.jsoup)
  implementation(libs.pluginPackages.checkerFramework)
  implementation(libs.pluginPackages.grgit)
  implementation(libs.pluginPackages.jetbrains.changelog)
  implementation(libs.pluginPackages.jetbrains.intellij)
  implementation(libs.pluginPackages.jetbrains.kotlin)
  implementation(libs.pluginPackages.spotless)
  testImplementation(libs.junit.api)
}

tasks.withType<Test> {
  useJUnitPlatform()
}

project.tasks.withType<KotlinCompile> {
  kotlinOptions {
    allWarningsAsErrors = true
  }
}

apply<SpotlessPlugin>()
configure<SpotlessExtension> {
  val ktlintEditorConfig = mapOf(
    "ktlint_standard_no-wildcard-imports" to "disabled",
    "ktlint_standard_filename" to "disabled",
    "indent_size" to 2,
  )

  kotlin {
    ktlint().editorConfigOverride(ktlintEditorConfig)
    target("**/*.kt")
  }

  kotlinGradle {
    ktlint().editorConfigOverride(ktlintEditorConfig)
    target("**/*.gradle.kts")
  }
}

val isCI by extra(System.getenv("CI") == "true")

if (!isCI) {
  tasks.withType<KotlinCompile> { dependsOn("spotlessKotlinApply", "spotlessKotlinGradleApply") }
}
