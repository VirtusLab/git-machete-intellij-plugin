import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

// Not worth using Gradle toolchains, they don't seem to work as expected for buildSrc (or are just hard to configure properly).
// Let the developers install sdkman to switch Java versions instead.
val javaVersionProperties = Properties().apply {
  load(rootDir.parentFile.resolve("java-version.properties").inputStream())
}
val requiredJavaVersion = javaVersionProperties.getProperty("jdkVersionForRunningGradle").toInt()
val currentJavaVersion = JavaVersion.current()
if (currentJavaVersion != JavaVersion.toVersion(requiredJavaVersion)) {
  throw GradleException(
    "This build must be run under Java $requiredJavaVersion, rather than the current $currentJavaVersion. " +
      "Consider using sdkman with .sdkmanrc file for easily switching Java versions.",
  )
}

plugins {
  `kotlin-dsl`
  alias(libs.plugins.spotless)
  alias(libs.plugins.taskTree)
}

repositories {
  mavenLocal()
  mavenCentral()
  gradlePluginPortal()
}

val properties = Properties()
properties.load(rootDir.parentFile.resolve("intellij-versions.properties").inputStream())
val kotlinVersion = properties.getProperty("kotlinVersion")!!
val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
val kotlinxSerializationJsonVersion = properties.getProperty("kotlinxSerializationJsonVersion")!!
val kotlinxSerializationJson = "org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationJsonVersion"

dependencies {
  implementation(kotlinGradlePlugin)
  implementation(kotlinxSerializationJson)
  implementation(libs.pluginPackages.checkerFramework)
  implementation(libs.pluginPackages.grgit)
  implementation(libs.pluginPackages.spotless)
  testImplementation(libs.junit.api)
  testImplementation(libs.junit.params)
  testRuntimeOnly(libs.junit.engine)
  testRuntimeOnly(libs.junit.platformLauncher)
}

tasks.withType<Test> {
  useJUnitPlatform()

  testLogging {
    exceptionFormat = TestExceptionFormat.FULL
  }
}

spotless {
  val ktlintEditorConfig = mapOf(
    "indent_size" to 2,
    "ktlint_standard_no-wildcard-imports" to "disabled",
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
