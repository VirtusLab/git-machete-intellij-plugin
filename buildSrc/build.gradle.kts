import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import nl.littlerobots.vcu.plugin.VersionCatalogUpdateExtension
import nl.littlerobots.vcu.plugin.VersionCatalogUpdatePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import se.ascp.gradle.GradleVersionsFilterPlugin

plugins {
  `kotlin-dsl`
}

val kotlinLanguageVersion = "1.6"

// This is needed to use kotlin language version different from the default (1.4).
// See https://handstandsam.com/2022/04/13/using-the-kotlin-dsl-gradle-plugin-forces-kotlin-1-4-compatibility/.
afterEvaluate {
  tasks.withType<KotlinCompile>().configureEach {
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
    classpath(libs.pluginPackages.versionCatalogUpdate)
    classpath(libs.pluginPackages.versionsFilter)
  }
}

repositories {
  mavenLocal()
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  implementation(libs.jsoup)
  // Needed so that versionCatalogUpdate, when executed against buildSrc,
  // doesn't see jetbrains-annotations as an exceeded dependency and doesn't try to downgrade.
  // See https://github.com/littlerobots/version-catalog-update-plugin#exceeded-dependencies
  implementation(libs.jetbrains.annotations)
  implementation(libs.pluginPackages.aspectj.postCompileWeaving)
  implementation(libs.pluginPackages.checkerFramework)
  implementation(libs.pluginPackages.grgit)
  implementation(libs.pluginPackages.jetbrains.changelog)
  implementation(libs.pluginPackages.jetbrains.intellij)
  implementation(libs.pluginPackages.jetbrains.kotlin)
  implementation(libs.pluginPackages.spotless)
  implementation(libs.pluginPackages.taskTree)
  implementation(libs.pluginPackages.versionCatalogUpdate)
  implementation(libs.pluginPackages.versionsFilter)
  testImplementation(libs.junit)
}

apply<GradleVersionsFilterPlugin>()
apply<VersionCatalogUpdatePlugin>()
apply<KotlinPluginWrapper>()

// Let's use a low version so that buildSrc/ itself builds & executes properly
// on every machine even when running for the first time.
// In the top-level Gradle config, there is a Gradle toolchain,
// which makes sure that the project itself builds under the correct (high) Java version,
// even if it was previously missing from the machine.
val buildSrcJavaVersion = JavaVersion.VERSION_1_8.toString()

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
  sortByKey.set(false)

  // TODO (ben-manes/gradle-versions-plugin#284): `versionCatalogUpdate` should work on both the project and project's buildSrc
  //  The `keep` settings are needed so that a `versionCatalogUpdate` on buildSrc doesn't remove the dependencies of the project
  keep {
    keepUnusedVersions.set(true)
    keepUnusedLibraries.set(true)
    keepUnusedPlugins.set(true)
  }
}

apply<SpotlessPlugin>()
configure<SpotlessExtension> {
  kotlin {
    ktlint().editorConfigOverride(
      mapOf(
        "ktlint_disabled_rules" to "no-wildcard-imports,filename",
        "indent_size" to 2
      )
    )
    target("**/*.kt")
  }

  kotlinGradle {
    ktlint().editorConfigOverride(
      mapOf(
        "ktlint_disabled_rules" to "no-wildcard-imports",
        "indent_size" to 2
      )
    )
    target("**/*.gradle.kts")
  }
}

val isCI by extra(System.getenv("CI") == "true")

if (!isCI) { tasks.withType<KotlinCompile> { dependsOn("spotlessKotlinApply", "spotlessKotlinGradleApply") } }
