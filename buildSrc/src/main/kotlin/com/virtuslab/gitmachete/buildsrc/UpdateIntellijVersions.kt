package com.virtuslab.gitmachete.buildsrc

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.provideDelegate

open class UpdateIntellijVersions : DefaultTask() {

  @TaskAction
  fun execute() {
    val intellijVersions: IntellijVersions by project.rootProject.extra
    val originalVersions = intellijVersions

    val versionsProvider = RealIntelliJVersionsProvider()
    val updater = IntellijVersionsUpdater(versionsProvider)

    val updatedVersions = updater.update(originalVersions) { message ->
      logger.lifecycle(message)
    }

    if (originalVersions != updatedVersions) {
      PropertiesHelper.storeProperties(
        updatedVersions.toProperties(),
        project.rootDir.resolve("intellij-versions.properties"),
      )
    }
  }
}
