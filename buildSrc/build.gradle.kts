// Not worth using Gradle toolchains, they don't seem to work as expected for buildSrc (or are just hard to configure properly).
// Let the developers install sdkman to switch Java versions instead.
if (JavaVersion.current() != JavaVersion.VERSION_17) {
  throw GradleException("This build must be run under Java 17. Consider using sdkman with .sdkmanrc file for easily switching Java versions.")
}

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

dependencies {
  implementation(libs.pluginPackages.jetbrains.kotlin)
}
