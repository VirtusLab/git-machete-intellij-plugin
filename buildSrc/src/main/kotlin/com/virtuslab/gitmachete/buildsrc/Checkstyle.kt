package com.virtuslab.gitmachete.buildsrc

import org.gradle.api.Project
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.plugins.quality.CheckstylePlugin
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named

fun Project.configureCheckstyle() {
  apply<CheckstylePlugin>()
  configure<CheckstyleExtension> {
    tasks.named<Checkstyle>("checkstyleTest") { enabled = false }

    configProperties = mapOf("rootCheckstyleConfigDir" to "${rootDir}/config/checkstyle")
  }
}
