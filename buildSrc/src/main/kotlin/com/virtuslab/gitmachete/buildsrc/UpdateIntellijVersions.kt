package com.virtuslab.gitmachete.buildsrc

import com.virtuslab.gitmachete.buildsrc.IntellijVersionHelper.getMajorPart
import com.virtuslab.gitmachete.buildsrc.IntellijVersionHelper.toBuildNumber
import com.virtuslab.gitmachete.buildsrc.IntellijVersionHelper.versionIsNewerThan
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.jsoup.Jsoup

open class UpdateIntellijVersions : DefaultTask() {
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
  private fun findReleaseNewerThan(latestStable: String): String? {
    return findMatchingVersionNewerThan(Regex("(?<=BUILD-)(\\d+\\.)+\\d+(?=.txt)"), latestStable)
  }

  private fun findLatestMinorOfMajor(major: String): String {
    return findMatchingVersionNewerThan(Regex("(?<=BUILD-)$major(\\.\\d+)?(?=.txt)"), major) ?: major
  }

  private fun findEapWithBuildNumberHigherThan(latestEapBuildNumber: String): String? {
    return findMatchingVersionNewerThan(
      Regex("(?<=ideaIC-)\\d+\\.\\d+\\.\\d+(?=-EAP-SNAPSHOT.pom)"),
      latestEapBuildNumber
    )
  }

  private fun getUpdatedLatestMinorsList(majorOfReplacedStable: String): List<String> {
    val latestMinorsList = IntellijVersions.latestMinorsOfOldSupportedMajors
    val latestMinorOfReplacedMajor = findLatestMinorOfMajor(majorOfReplacedStable)

    return latestMinorsList.filter { getMajorPart(it) != majorOfReplacedStable }
      .map { findLatestMinorOfMajor(getMajorPart(it)) }
      .plus(latestMinorOfReplacedMajor)
  }

  @TaskAction
  fun execute() {
    val properties = IntellijVersionHelper.getProperties()
    val latestStable = IntellijVersions.latestStable

    val newerStable = findReleaseNewerThan(latestStable)

    if (!newerStable.isNullOrEmpty()) {
      project.logger.lifecycle("latestStable is updated to $newerStable")
      properties.setProperty("latestStable", newerStable)
      properties.remove("eapOfLatestSupportedMajor")

      val majorOfLatestStable = getMajorPart(latestStable)
      if (majorOfLatestStable != getMajorPart(newerStable)) {
        val latestMinorsList = getUpdatedLatestMinorsList(majorOfLatestStable)
        properties.setProperty("latestMinorsOfOldSupportedMajors", latestMinorsList.joinToString(separator = ","))
      }
      IntellijVersionHelper.storeProperties(properties)
    } else {
      val buildNumberThreshold = IntellijVersions.eapOfLatestSupportedMajor?.replace("-EAP-SNAPSHOT", "")
        ?: "${toBuildNumber(latestStable)}.999999.999999"

      val newerEapBuildNumber = findEapWithBuildNumberHigherThan(buildNumberThreshold)

      if (!newerEapBuildNumber.isNullOrEmpty()) {
        project.logger.lifecycle("eapOfLatestSupportedMajor is updated to $newerEapBuildNumber")
        properties.setProperty("eapOfLatestSupportedMajor", "$newerEapBuildNumber-EAP-SNAPSHOT")
        IntellijVersionHelper.storeProperties(properties)
      }
    }
  }
}
