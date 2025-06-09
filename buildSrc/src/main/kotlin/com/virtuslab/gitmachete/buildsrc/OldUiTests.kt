package com.virtuslab.gitmachete.buildsrc

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register

fun Project.configureOldUiTests() {
  val isCI: Boolean by rootProject.extra
  val intellijVersions: IntellijVersions by rootProject.extra

  val sourceSets = extensions["sourceSets"] as? SourceSetContainer
  val oldUiTest = sourceSets!!["oldUiTest"]
  val oldUiTestsDir = "${System.getProperty("user.home")}/.ideprobe-uitests"

  val oldUiTestTargets: List<String> =
    if (project.properties["against"] != null) {
      intellijVersions.resolveIntelliJVersions(project.properties["against"] as? String)
    } else {
      listOf(intellijVersions.buildTarget)
    }

  oldUiTestTargets.onEach { version ->
    tasks.register<Test>("oldUiTest_$version") {
      description = "Runs old UI tests."
      group = "verification"

      testClassesDirs = oldUiTest.output.classesDirs
      classpath = configurations["oldUiTestRuntimeClasspath"] + oldUiTest.output

      val buildPlugin = tasks.findByPath(":buildPlugin")!!
      dependsOn(buildPlugin)
      environment("IDEPROBE_INTELLIJ_PLUGIN_URI", buildPlugin.outputs.files.first().path)
      environment("IDEPROBE_INTELLIJ_VERSION_BUILD", version.removeSuffix("-EAP-SNAPSHOT"))

      if (!isCI) {
        outputs.upToDateWhen { false }
      }

      if (project.properties["tests"] != null) {
        filter { includeTestsMatching("*.*${project.properties["tests"]}*") }
      }

      jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")

      if (project.hasProperty("virtualDisplay")) {
        environment("IDEPROBE_DISPLAY", "xvfb")
        environment(
          "IDEPROBE_PATHS_SCREENSHOTS",
          "$oldUiTestsDir/artifacts/uiTest$version/screenshots",
        )
        if (isCI) {
          environment("IDEPROBE_PATHS_BASE", oldUiTestsDir)
        }
      }

      environment("IDEPROBE_PATHS_LOG_EXPORT", "$oldUiTestsDir/idea-logs")

      testLogging {
        showStandardStreams = true
      }
    }
  }

  tasks.register("oldUiTest") {
    dependsOn(tasks.matching { task -> task.name.startsWith("oldUiTest_") })
  }
}
