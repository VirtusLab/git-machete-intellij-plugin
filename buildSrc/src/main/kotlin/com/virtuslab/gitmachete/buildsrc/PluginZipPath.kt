package com.virtuslab.gitmachete.buildsrc

import org.gradle.api.Project
import java.nio.file.Files
import java.nio.file.Paths

fun Project.printPluginZipPath() {
    val buildPlugin = tasks.findByPath(":buildPlugin")!!
    println(buildPlugin.outputs.files.first().path)
}
