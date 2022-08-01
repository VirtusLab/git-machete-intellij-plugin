import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `kotlin-dsl`
  // See https://youtrack.jetbrains.com/issue/KTIJ-19369
  // for the details of a false-positive error reported here by IntelliJ
  alias(libs.plugins.jetbrains.kotlin)
  alias(libs.plugins.versionsFilter)
  alias(libs.plugins.versionCatalogUpdate)
}

versionCatalogUpdate {
  sortByKey.set(false)

  // TODO (ben-manes/gradle-versions-plugin#284): `versionCatalogUpdate` should work on both the project and project's buildSrc
  //  The `keep` settings are needed so that a `versionCatalogUpdate` on buildSrc doesn't remove the dependencies of the project
  keep {
    keepUnusedVersions.set(true)
    keepUnusedLibraries.set(true)
    keepUnusedPlugins.set(true)
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
