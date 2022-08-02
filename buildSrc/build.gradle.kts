import nl.littlerobots.vcu.plugin.VersionCatalogUpdateExtension
import nl.littlerobots.vcu.plugin.VersionCatalogUpdatePlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import se.ascp.gradle.GradleVersionsFilterPlugin

plugins {
  `kotlin-dsl`
}

buildscript {
  repositories {
    gradlePluginPortal()
  }
  dependencies {
    classpath(libs.pluginPackages.jetbrains.kotlin)
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
  implementation(libs.pluginPackages.jetbrains.intellij)
  implementation(libs.pluginPackages.jetbrains.kotlin)
  implementation(libs.pluginPackages.spotless)
  testImplementation(libs.junit)
}

val javaMajorVersion = JavaVersion.VERSION_11

project.tasks.withType<KotlinCompile> {
  kotlinOptions {
    allWarningsAsErrors = true
    jvmTarget = javaMajorVersion.majorVersion
  }
}

kotlin {
  kotlinDslPluginOptions {
    jvmTarget.set(javaMajorVersion.majorVersion)
  }
}

apply<VersionCatalogUpdatePlugin>()
apply<GradleVersionsFilterPlugin>()
apply(plugin = "org.jetbrains.kotlin.jvm")

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
