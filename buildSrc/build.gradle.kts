import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

// Gradle toolchains aren't easily applicable to buildSrc itself, so we only enforce a *minimum* JVM
// for running Gradle here. The bytecode targeted by the plugin is decoupled and driven by the
// `java { toolchain { ... } }` block in the root build (which is auto-provisioned via Foojay).
val minJavaVersionForRunningGradle = 21
val currentJavaVersion = JavaVersion.current()
if (currentJavaVersion < JavaVersion.toVersion(minJavaVersionForRunningGradle)) {
  throw GradleException(
    "This build must be run under Java $minJavaVersionForRunningGradle or newer, rather than the current $currentJavaVersion. " +
      "Consider using sdkman for easily switching Java versions.",
  )
}

plugins {
  `kotlin-dsl`
  alias(libs.plugins.spotless)
  alias(libs.plugins.taskTree)
}

// Same optional proxy as root: gradle-local.properties at repo root (see gradle-local.properties.example).
val optionalMavenProxyFromGradleLocal: String? = run {
  val f = rootDir.parentFile.resolve("gradle-local.properties")
  if (!f.isFile) return@run null
  Properties().apply { f.reader().use { load(it) } }
    .getProperty("mavenProxyUrl")
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
}

repositories {
  mavenLocal()
  if (optionalMavenProxyFromGradleLocal != null) {
    maven(optionalMavenProxyFromGradleLocal)
  } else {
    mavenCentral()
  }
  gradlePluginPortal()
}

val properties = Properties()
properties.load(rootDir.parentFile.resolve("intellij-versions.properties").inputStream())
val kotlinVersion = properties.getProperty("kotlinVersion")!!
val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"

dependencies {
  implementation(kotlinGradlePlugin)
  implementation(libs.kotlinxSerializationJson)
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
