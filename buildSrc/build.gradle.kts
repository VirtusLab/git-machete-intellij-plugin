import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `java-library`
  `kotlin-dsl`
  alias(libs.plugins.jetbrains.kotlin)
}

repositories {
  mavenLocal()
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  implementation(gradleApi())
  implementation(libs.aspectj.postCompileWeaving)
  implementation(libs.jsoup)
  implementation(libs.jetbrains.intellij)
  implementation(libs.jetbrains.kotlin)
  implementation(libs.spotless)
  implementation(libs.checkerFramework)
}

val javaMajorVersion = JavaVersion.VERSION_11

project.tasks.withType<KotlinCompile> {
//  TODO (#785): revert this setting
//  kotlinOptions.allWarningsAsErrors = true

  // Supress the warnings about different version of Kotlin used for compilation
  // than bundled into the `buildTarget` version of IntelliJ.
  // For compilation we use the Kotlin version from the earliest support IntelliJ,
  // and as per https://kotlinlang.org/docs/components-stability.html,
  // code compiled against an older version of kotlin-stdlib should work
  // when a newer version of kotlin-stdlib is provided as a drop-in replacement.
  kotlinOptions.freeCompilerArgs += listOf("-Xskip-metadata-version-check")

  kotlinOptions.jvmTarget = javaMajorVersion.majorVersion
}
