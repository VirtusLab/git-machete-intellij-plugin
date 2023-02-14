import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import nl.littlerobots.vcu.plugin.VersionCatalogUpdateExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `kotlin-dsl`
  alias(libs.plugins.taskTree)
  alias(libs.plugins.versionCatalogUpdate)
  alias(libs.plugins.versionsFilter)
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

// Let's use a low version so that buildSrc/ itself builds & executes properly
// on every machine even when running for the first time.
// In the top-level Gradle config, there is a Gradle toolchain,
// which makes sure that the project itself builds under the correct (high) Java version,
// even if it was previously missing from the machine.
val buildSrcJavaVersion = JavaVersion.VERSION_17.toString()

project.tasks.withType<KotlinCompile> {
  kotlinOptions {
    allWarningsAsErrors = true
    jvmTarget = buildSrcJavaVersion
  }
}

kotlin {
  kotlinDslPluginOptions {
    jvmTarget.set(buildSrcJavaVersion)
  }
}

configure<VersionCatalogUpdateExtension> {
  // To keep pluginPackages at the end
  sortByKey.set(false)
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

if (!isCI) { tasks.withType<KotlinCompile> { dependsOn("spotlessKotlinApply", "spotlessKotlinGradleApply") } }
