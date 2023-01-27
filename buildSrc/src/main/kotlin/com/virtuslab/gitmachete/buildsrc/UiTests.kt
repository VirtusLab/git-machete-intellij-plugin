package com.virtuslab.gitmachete.buildsrc

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register

fun Project.configureUiTests() {
  val isCI: Boolean by rootProject.extra
  val intellijVersions: IntellijVersions by rootProject.extra

  tasks["compileUiTestScala"].finalizedBy("classpathIndexCleanup")
  tasks["processUiTestResources"].finalizedBy("classpathIndexCleanup")

  val sourceSets = extensions["sourceSets"] as? SourceSetContainer
  val uiTest = sourceSets!!["uiTest"]
  val uiTestsDir = "${System.getProperty("user.home")}/.ideprobe-uitests"

  val uiTestTargets: List<String> =
    if (project.properties["against"] != null) {
      intellijVersions.resolveIntelliJVersions(project.properties["against"] as? String)
    } else {
      listOf(intellijVersions.buildTarget)
    }

  uiTestTargets.onEach { version ->
    tasks.register<Test>("uiTest_$version") {
      description = "Runs UI tests."
      group = "verification"

      testClassesDirs = uiTest.output.classesDirs
      classpath = configurations["uiTestRuntimeClasspath"] + uiTest.output

      val buildPlugin = tasks.findByPath(":buildPlugin")!!
      dependsOn(buildPlugin)
      environment("IDEPROBE_INTELLIJ_PLUGIN_URI", buildPlugin.outputs.files.first().path)
      environment("IDEPROBE_INTELLIJ_VERSION_BUILD", version)

      if (!isCI) {
        outputs.upToDateWhen { false }
      }

      if (project.properties["tests"] != null) {
        filter { includeTestsMatching("*.*${project.properties["tests"]}*") }
      }

      if (project.hasProperty("virtualDisplay")) {
        environment("IDEPROBE_DISPLAY", "xvfb")
        environment(
          "IDEPROBE_PATHS_SCREENSHOTS",
          "$uiTestsDir/artifacts/uiTest$version/screenshots"
        )
        if (isCI) {
          environment("IDEPROBE_PATHS_BASE", uiTestsDir)
        }
      }

      environment("IDEPROBE_PATHS_LOG_EXPORT", "$uiTestsDir/idea-logs")

      testLogging {
        showStandardStreams = true
      }
    }
  }

  tasks.register("uiTest") {
    dependsOn(tasks.matching { task -> task.name.startsWith("uiTest_") })
  }
}
