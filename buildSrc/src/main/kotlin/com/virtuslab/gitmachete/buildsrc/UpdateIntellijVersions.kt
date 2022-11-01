package com.virtuslab.gitmachete.buildsrc

import com.virtuslab.gitmachete.buildsrc.IntellijVersionHelper.versionIsNewerThan
import com.virtuslab.gitmachete.buildsrc.IntellijVersionHelper.versionToBuildNumber
import com.virtuslab.gitmachete.buildsrc.IntellijVersionHelper.versionToMajorVersion
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.provideDelegate
import org.jsoup.Jsoup
import java.io.File

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
      Regex("""(?<=ideaIC-)(\d+\.)+\d+(?=.pom)"""),
      version
    )
  }

  private fun findLatestMinorOfVersion(versionNumber: String): String {
    val major = versionToMajorVersion(versionNumber)

    return findFirstMatchingVersionNewerThan(
      intellijReleasesContents,
      Regex("""(?<=ideaIC-)$major(\.\d+)?(?=.pom)"""),
      major
    ) ?: versionNumber
  }

  private fun findEapWithBuildNumberHigherThan(buildNumber: String): String? {
    return findFirstMatchingVersionNewerThan(
      intellijSnapshotsContents,
      Regex("""(?<=ideaIC-)\d+\.\d+\.\d+(?=-EAP-SNAPSHOT\.pom)"""),
      buildNumber
    )
  }

  @TaskAction
  fun execute() {
    val intellijVersions: IntellijVersions by project.rootProject.extra
    val originalVersions = intellijVersions
    var updatedVersions = originalVersions
    val latestMinorsOfOldSupportedMajors = originalVersions.latestMinorsOfOldSupportedMajors.map { findLatestMinorOfVersion(it) }

    if (latestMinorsOfOldSupportedMajors != originalVersions.latestMinorsOfOldSupportedMajors) {
      project.logger.lifecycle("latestMinorsOfOldSupportedMajors are updated to $latestMinorsOfOldSupportedMajors")
      updatedVersions = updatedVersions.copy(latestMinorsOfOldSupportedMajors = latestMinorsOfOldSupportedMajors)
    }

    val latestStable = originalVersions.latestStable
    val newerStable = findReleaseNewerThan(latestStable)

    if (newerStable != null) {
      project.logger.lifecycle("latestStable is updated to $newerStable")
      updatedVersions = updatedVersions.copy(latestStable = newerStable)

      if (versionToMajorVersion(latestStable) != versionToMajorVersion(newerStable)) {
        updatedVersions = updatedVersions.copy(
          latestMinorsOfOldSupportedMajors = latestMinorsOfOldSupportedMajors.plus(findLatestMinorOfVersion(latestStable)),
          eapOfLatestSupportedMajor = null
        )
      }
    }

    val buildNumberThreshold = updatedVersions.eapOfLatestSupportedMajor?.replace("-EAP-SNAPSHOT", "")
      ?: "${versionToBuildNumber(updatedVersions.latestStable)}.999999.999999"

    val newerEapBuildNumber = findEapWithBuildNumberHigherThan(buildNumberThreshold)

    if (newerEapBuildNumber != null) {
      project.logger.lifecycle("eapOfLatestSupportedMajor is updated to $newerEapBuildNumber")
      updatedVersions = updatedVersions.copy(eapOfLatestSupportedMajor = "$newerEapBuildNumber-EAP-SNAPSHOT")
    }

    if (originalVersions != updatedVersions) {
      PropertiesHelper.storeProperties(updatedVersions.toProperties(), File("intellij-versions.properties"))
    }
  }
}
