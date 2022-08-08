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

  private fun findMatchingVersionNewerThan(regex: Regex, thresholdVersion: String): String? {
    val htmlContent = Jsoup.connect(intellijReleasesUrl).get()
    val links = htmlContent.select(
      "a[href^=${intellijReleasesUrl}com/jetbrains/intellij/idea/BUILD/][href$=.txt]"
    )

    for (link in links) {
      val attr = link.attr("href")
      val matchResult = regex.find(attr)

      if (matchResult != null) {
        val foundVersion = matchResult.value

        if (foundVersion versionIsNewerThan thresholdVersion) {
          return foundVersion
        }
      }
    }
    return null
  }
  fun checkForReleaseNewerThan(latestStable: String): String? {
    return findMatchingVersionNewerThan(Regex("(?<=BUILD-)(\\d+\\.)+\\d+(?=.txt)"), latestStable)
  }

  fun getLatestMinorOfMajor(major: String): String {
    return findMatchingVersionNewerThan(Regex("(?<=BUILD-)$major(\\.\\d+)?(?=.txt)"), major) ?: major
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
