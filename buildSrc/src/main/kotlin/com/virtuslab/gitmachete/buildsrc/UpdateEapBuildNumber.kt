package com.virtuslab.gitmachete.buildsrc

import com.virtuslab.gitmachete.buildsrc.IntellijVersionHelper.versionIsNewerThan
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.jsoup.Jsoup

open class UpdateEapBuildNumber : DefaultTask() {

  @Internal
  val intellijSnapshotsUrl: String = "https://www.jetbrains.com/intellij-repository/snapshots/"

  fun checkForEapWithBuildNumberHigherThan(latestEapBuildNumber: String): String? {
    val htmlContent = Jsoup.connect(intellijSnapshotsUrl).get()
    val links =
      htmlContent.select(
        "a[href^=${intellijSnapshotsUrl}com/jetbrains/intellij/idea/ideaIC/][href$=-EAP-SNAPSHOT.pom]"
      )

    for (link in links) {
      val attr = link.attr("href")
      val regex = Regex("(?<=ideaIC-)(\\d+\\.)+\\d+(?=-EAP-SNAPSHOT.pom)")
      val matchResult = regex.find(attr)

      if (matchResult != null) {
        val foundBuildNumber = matchResult.value

        if (foundBuildNumber versionIsNewerThan latestEapBuildNumber) {
          return foundBuildNumber
        }
      }
    }
    return null
  }

  @TaskAction
  fun execute() {
    val properties = IntellijVersionHelper.getProperties()

    val buildNumberThreshold = IntellijVersions.eapOfLatestSupportedMajor?.replace("-EAP-SNAPSHOT", "")
      ?: "${IntellijVersionHelper.toBuildNumber(IntellijVersions.latestStable)}.999999.999999"

    val newerEapBuildNumber = checkForEapWithBuildNumberHigherThan(buildNumberThreshold)

    if (!newerEapBuildNumber.isNullOrEmpty()) {
      project.logger.lifecycle("eapOfLatestSupportedMajor is updated to $newerEapBuildNumber")
      properties.setProperty("eapOfLatestSupportedMajor", "$newerEapBuildNumber-EAP-SNAPSHOT")
      IntellijVersionHelper.storeProperties(properties)
    }
  }
}
