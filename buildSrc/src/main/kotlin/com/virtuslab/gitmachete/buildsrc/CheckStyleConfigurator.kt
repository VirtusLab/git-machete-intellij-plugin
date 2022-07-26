package com.virtuslab.gitmachete.buildsrc

import org.gradle.api.Project
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.plugins.quality.CheckstylePlugin
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named

object CheckStyleConfigurator {
    fun configure(project: Project) {
        project.apply<CheckstylePlugin>()
        project.configure<CheckstyleExtension> {
            project.tasks.named<Checkstyle>("checkstyleTest") {
                enabled = false
            }

            configProperties = mapOf("rootCheckstyleConfigDir" to "${project.rootDir}/config/checkstyle")
        }
    }
}