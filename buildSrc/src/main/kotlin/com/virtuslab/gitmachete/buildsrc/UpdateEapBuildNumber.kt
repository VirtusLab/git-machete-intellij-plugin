package com.virtuslab.gitmachete.buildsrc

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.jsoup.Jsoup
import java.util.regex.Pattern

open class UpdateEapBuildNumber : DefaultTask() {

  @Internal
  val intellijSnapshotsUrl: String = "https://www.jetbrains.com/intellij-repository/snapshots/"

  fun checkForEapWithBuildNumberHigherThan(latestEapBuildNumber: String): String? {
    val htmlContent = Jsoup.connect(intellijSnapshotsUrl).get()
    val links =
      htmlContent.select(
        "a[href^=" +
          intellijSnapshotsUrl +
          "com/jetbrains/intellij/idea/ideaIC/][href$=\"-EAP-SNAPSHOT.pom\"]"
      )

    for (link in links) {
      val attr = link.attr("href")
      val pattern = Pattern.compile("(?<=ideaIC-)\\d+\\.\\d+\\.\\d+(?=-EAP-SNAPSHOT.pom)")
      val matcher = pattern.matcher(attr)

      if (matcher.find()) {
        val foundBuildNumber = matcher.group()

        if (foundBuildNumber buildNumberIsNewerThan latestEapBuildNumber) {
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

  companion object {
    infix fun String.buildNumberIsNewerThan(rhsBuildNumber: String): Boolean {
      val lhsSplit = this.split('.')
      val rhsSplit = rhsBuildNumber.split('.')

      val firstDiff = lhsSplit.zip(rhsSplit).find { it.first != it.second }

      // 8.0.6 is older than 8.0.6.0, but zipped they will look like this: [(8,8), (0,0), (6,6)]
      if (firstDiff == null) {
        return lhsSplit.size > rhsSplit.size
      }

      return Integer.parseInt(firstDiff.first) > Integer.parseInt(firstDiff.second)
    }
  }
}
