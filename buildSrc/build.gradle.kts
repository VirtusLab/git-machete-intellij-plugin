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
  kotlinOptions.freeCompilerArgs += listOf("-Xskip-metadata-version-check")

  kotlinOptions.jvmTarget = javaMajorVersion.majorVersion
}
