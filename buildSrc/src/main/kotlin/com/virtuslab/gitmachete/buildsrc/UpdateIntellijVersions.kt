package com.virtuslab.gitmachete.buildsrc

import com.virtuslab.gitmachete.buildsrc.IntellijVersionHelper.getMajorPart
import com.virtuslab.gitmachete.buildsrc.IntellijVersionHelper.getPropertyOrNullIfEmpty
import com.virtuslab.gitmachete.buildsrc.IntellijVersionHelper.toBuildNumber
import com.virtuslab.gitmachete.buildsrc.IntellijVersionHelper.versionIsNewerThan
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.jsoup.Jsoup

open class UpdateIntellijVersions : DefaultTask() {

  private val intellijReleasesContents: List<String> by lazy {
    getLinksFromUrl("https://www.jetbrains.com/intellij-repository/releases/")
  }

  private val intellijSnapshotsContents: List<String> by lazy {
    getLinksFromUrl("https://www.jetbrains.com/intellij-repository/snapshots/")
  }

  private fun getLinksFromUrl(repositoryUrl: String): List<String> {
    return Jsoup.connect(repositoryUrl).get()
      .select("a[href^=${repositoryUrl}com/jetbrains/intellij/idea/ideaIC/][href$=.pom]")
      .map { it.attr("href") }
  }

  private fun findFirstMatchingVersionNewerThan(repoLinks: List<String>, regex: Regex, thresholdVersion: String): String? {
    for (link in repoLinks) {
      val matchResult = regex.find(link)

      if (matchResult != null) {
        val foundVersion = matchResult.value

        if (foundVersion versionIsNewerThan thresholdVersion) {
          return foundVersion
        }
      }
    }
    return null
  }

  private fun findReleaseNewerThan(version: String): String? {
    return findFirstMatchingVersionNewerThan(
      intellijReleasesContents,
      Regex("(?<=ideaIC-)(\\d+\\.)+\\d+(?=.pom)"),
      version
    )
  }

  private fun findLatestMinorOfVersion(versionNumber: String): String {
    val major = getMajorPart(versionNumber)

    return findFirstMatchingVersionNewerThan(
      intellijReleasesContents,
      Regex("(?<=ideaIC-)$major(\\.\\d+)?(?=.pom)"),
      major
    ) ?: versionNumber
  }

  private fun findEapWithBuildNumberHigherThan(buildNumber: String): String? {
    return findFirstMatchingVersionNewerThan(
      intellijSnapshotsContents,
      Regex("(?<=ideaIC-)\\d+\\.\\d+\\.\\d+(?=-EAP-SNAPSHOT.pom)"),
      buildNumber
    )
  }

  @TaskAction
  fun execute() {
    val properties = IntellijVersionHelper.getProperties()
    val originalProperties = IntellijVersionHelper.getProperties()
    val latestStable = IntellijVersions.latestStable
    val latestMinorsList = IntellijVersions.latestMinorsOfOldSupportedMajors
      .map { findLatestMinorOfVersion(it) }

    properties.setProperty("latestMinorsOfOldSupportedMajors", latestMinorsList.joinToString(separator = ","))

    val newerStable = findReleaseNewerThan(latestStable)

    if (newerStable != null) {
      project.logger.lifecycle("latestStable is updated to $newerStable")
      properties.setProperty("latestStable", newerStable)

      if (getMajorPart(latestStable) != getMajorPart(newerStable)) {
        val updatedMinorsList = latestMinorsList.plus(findLatestMinorOfVersion(latestStable))
        properties.setProperty("latestMinorsOfOldSupportedMajors", updatedMinorsList.joinToString(separator = ","))
        properties.setProperty("eapOfLatestSupportedMajor", "")
      }
    }

    val maybeEapOfLatestSupportedMajor = properties.getPropertyOrNullIfEmpty("eapOfLatestSupportedMajor")

    val buildNumberThreshold = maybeEapOfLatestSupportedMajor?.replace("-EAP-SNAPSHOT", "")
      ?: "${toBuildNumber(properties.getProperty("latestStable"))}.999999.999999"

    val newerEapBuildNumber = findEapWithBuildNumberHigherThan(buildNumberThreshold)

    if (newerEapBuildNumber != null) {
      project.logger.lifecycle("eapOfLatestSupportedMajor is updated to $newerEapBuildNumber")
      properties.setProperty("eapOfLatestSupportedMajor", "$newerEapBuildNumber-EAP-SNAPSHOT")
    }

    if (originalProperties != properties) {
      IntellijVersionHelper.storeProperties(properties)
    }
  }
}
