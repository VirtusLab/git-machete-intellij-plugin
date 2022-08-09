package com.virtuslab.gitmachete.buildsrc

import com.virtuslab.gitmachete.buildsrc.IntellijVersionHelper.getMajorPart
import com.virtuslab.gitmachete.buildsrc.IntellijVersionHelper.toBuildNumber
import com.virtuslab.gitmachete.buildsrc.IntellijVersionHelper.versionIsNewerThan
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.jsoup.Jsoup
import org.jsoup.select.Elements

open class UpdateIntellijVersions : DefaultTask() {

  private val intellijReleasesContents: Elements by lazy {
    getLinksFromUrl("https://www.jetbrains.com/intellij-repository/releases/")
  }

  private val intellijSnapshotsContents: Elements by lazy {
    getLinksFromUrl("https://www.jetbrains.com/intellij-repository/snapshots/")
  }

  private fun getLinksFromUrl(repositoryUrl: String): Elements {
    return Jsoup.connect(repositoryUrl).get()
      .select("a[href^=${repositoryUrl}com/jetbrains/intellij/idea/ideaIC/][href$=.pom]")
  }

  private fun findFirstMatchingVersionNewerThan(repoLinks: Elements, regex: Regex, thresholdVersion: String): String? {
    for (link in repoLinks) {
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
  private fun findReleaseNewerThan(version: String): String? {
    return findMatchingVersionNewerThan(
      intellijReleasesContents,
      Regex("(?<=ideaIC-)(\\d+\\.)+\\d+(?=.pom)"),
      version
    )
  }

  private fun findLatestMinorOfVersion(versionNumber: String): String {
    val major = getMajorPart(versionNumber)

    return findMatchingVersionNewerThan(
      intellijReleasesContents,
      Regex("(?<=ideaIC-)$major(\\.\\d+)?(?=.pom)"),
      major
    ) ?: versionNumber
  }

  private fun findEapWithBuildNumberHigherThan(buildNumber: String): String? {
    return findMatchingVersionNewerThan(
      intellijSnapshotsContents,
      Regex("(?<=ideaIC-)\\d+\\.\\d+\\.\\d+(?=-EAP-SNAPSHOT.pom)"),
      buildNumber
    )
  }

  @TaskAction
  fun execute() {
    val properties = IntellijVersionHelper.getProperties()
    val latestStable = IntellijVersions.latestStable
    val latestMinorsList = IntellijVersions.latestMinorsOfOldSupportedMajors
      .map { findLatestMinorOfVersion(it) }

    properties.setProperty("latestMinorsOfOldSupportedMajors", latestMinorsList.joinToString(separator = ","))

    val newerStable = findReleaseNewerThan(latestStable)

    if (newerStable != null) {
      project.logger.lifecycle("latestStable is updated to $newerStable")
      properties.setProperty("latestStable", newerStable)
      properties.remove("eapOfLatestSupportedMajor")

      if (getMajorPart(latestStable) != getMajorPart(newerStable)) {
        val updatedMinorsList = latestMinorsList.plus(findLatestMinorOfVersion(latestStable))
        properties.setProperty("latestMinorsOfOldSupportedMajors", updatedMinorsList.joinToString(separator = ","))
      }
    } else {
      val buildNumberThreshold = IntellijVersions.eapOfLatestSupportedMajor?.replace("-EAP-SNAPSHOT", "")
        ?: "${toBuildNumber(latestStable)}.999999.999999"

      val newerEapBuildNumber = findEapWithBuildNumberHigherThan(buildNumberThreshold)

      if (newerEapBuildNumber != null) {
        project.logger.lifecycle("eapOfLatestSupportedMajor is updated to $newerEapBuildNumber")
        properties.setProperty("eapOfLatestSupportedMajor", "$newerEapBuildNumber-EAP-SNAPSHOT")
      }
    }

    IntellijVersionHelper.storeProperties(properties)
  }
}
