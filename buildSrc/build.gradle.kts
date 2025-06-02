plugins {
  `kotlin-dsl`
}

buildscript {
  repositories {
    gradlePluginPortal()
  }
  dependencies {
    classpath(libs.pluginPackages.jetbrains.kotlin)
  }
}

repositories {
  mavenLocal()
  mavenCentral()
  gradlePluginPortal()
}
