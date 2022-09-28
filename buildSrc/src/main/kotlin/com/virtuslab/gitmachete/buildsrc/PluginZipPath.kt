package com.virtuslab.gitmachete.buildsrc

import org.gradle.api.Project

fun Project.printPluginZipPath() {
  val buildPlugin = tasks.findByPath(":buildPlugin")!!
  println(buildPlugin.outputs.files.first().path)
}
