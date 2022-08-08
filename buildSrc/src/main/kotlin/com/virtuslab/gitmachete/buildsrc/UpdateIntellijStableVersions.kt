package com.virtuslab.gitmachete.buildsrc

import com.virtuslab.gitmachete.buildsrc.IntellijVersionHelper.getMajorPart
import com.virtuslab.gitmachete.buildsrc.IntellijVersionHelper.versionIsNewerThan
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.jsoup.Jsoup

open class UpdateIntellijStableVersions : DefaultTask() {
  @Internal
  val intellijReleasesUrl: String = "https://www.jetbrains.com/intellij-repository/releases/"

  fun checkForReleaseNewerThan(latestStable: String): String? {
    val htmlContent = Jsoup.connect(intellijReleasesUrl).get()
    val links =
      htmlContent.select(
        "a[href^=${intellijReleasesUrl}com/jetbrains/intellij/idea/BUILD/][href$=.txt]"
      )

    for (link in links) {
      val attr = link.attr("href")
      val regex = Regex("(?<=BUILD-)(\\d+\\.)+\\d+(?=.txt)")
      val matchResult = regex.find(attr)

      if (matchResult != null) {
        val foundVersionNumber = matchResult.value

        if (foundVersionNumber versionIsNewerThan latestStable) {
          return foundVersionNumber
        }
      }
    }
    return null
  }

  fun getLatestMinorOfMajor(major: String): String {
    val htmlContent = Jsoup.connect(intellijReleasesUrl).get()
    val links =
      htmlContent.select(
        "a[href^=${intellijReleasesUrl}com/jetbrains/intellij/idea/BUILD/][href$=.txt]"
      )

    var latestMinor = major

    for (link in links) {
      val attr = link.attr("href")
      val regex = Regex("(?<=BUILD-)$major(\\.\\d+)?(?=.txt)")
      val matchResult = regex.find(attr)

      if (matchResult != null && matchResult.value versionIsNewerThan latestMinor) {
        latestMinor = matchResult.value
        break
      }
    }
    return latestMinor
  }

  @TaskAction
  fun execute() {
    val properties = IntellijVersionHelper.getProperties()

    val latestStable = IntellijVersions.latestStable

    val newerStable = checkForReleaseNewerThan(latestStable)

    if (!newerStable.isNullOrEmpty()) {
      project.logger.lifecycle("latestStable is updated to $newerStable")
      properties.setProperty("latestStable", "$newerStable")
      properties.remove("eapOfLatestSupportedMajor")
      val majorOfLatestStable = getMajorPart(latestStable)
      if (majorOfLatestStable != getMajorPart(newerStable)) {
        val latestMinorsList = IntellijVersions.latestMinorsOfOldSupportedMajors.toMutableList()
        val latestMinorOfReplacedMajor = getLatestMinorOfMajor(majorOfLatestStable)

        latestMinorsList.removeIf { getMajorPart(it) == majorOfLatestStable }
        latestMinorsList.add(latestMinorOfReplacedMajor)
        properties.setProperty("latestMinorsOfOldSupportedMajors", latestMinorsList.joinToString(separator = ","))
      }
      IntellijVersionHelper.storeProperties(properties)
    }
  }
}
