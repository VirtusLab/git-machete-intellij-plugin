import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `kotlin-dsl`
  alias(libs.plugins.jetbrains.kotlin)
  alias(libs.plugins.versions)
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
  implementation(gradleApi())
  implementation(libs.aspectj.postCompileWeaving)
  implementation(libs.checkerFramework)
  implementation(libs.jetbrains.intellij)
  implementation(libs.jetbrains.kotlin)
  implementation(libs.jsoup)
  implementation(libs.spotless)
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
