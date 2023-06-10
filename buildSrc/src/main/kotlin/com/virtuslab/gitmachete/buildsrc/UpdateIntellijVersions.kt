package com.virtuslab.gitmachete.buildsrc

import com.virtuslab.gitmachete.buildsrc.IntellijVersionHelper.versionIsNewerThan
import com.virtuslab.gitmachete.buildsrc.IntellijVersionHelper.versionToBuildNumber
import com.virtuslab.gitmachete.buildsrc.IntellijVersionHelper.versionToMajorVersion
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.provideDelegate
import org.jsoup.Jsoup
import java.net.URL

open class UpdateIntellijVersions : DefaultTask() {

  private val linksToIntellijReleases: List<String> by lazy {
    getLinksFromUrl("https://www.jetbrains.com/intellij-repository/releases/")
  }

  private val linksToIntellijSnapshots: List<String> by lazy {
    getLinksFromUrl("https://www.jetbrains.com/intellij-repository/snapshots/")
  }

  private fun getLinksFromUrl(repositoryUrl: String): List<String> {
    val selector = "a[href^=${repositoryUrl}com/jetbrains/intellij/idea/ideaIC/][href$=.pom]"
    val rawHtml = URL(repositoryUrl).openStream().readAllBytes().decodeToString()
    // Do not use Jsoup.connect(repositoryUrl) - since mid-2023, jetbrains.com seems to serve only part of the site to this client.
    val result = Jsoup.parse(rawHtml).select(selector).map { it.attr("href") }
    if (result.isEmpty()) {
      throw RuntimeException(
        "No links matching regex '$selector' have been found under $repositoryUrl. " +
          "This indicates that either the server doesn't expose the entire site contents to this programmatic HTTP client, " +
          "or that HTML structure of the site changed.",
      )
    }
    return result
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
      linksToIntellijReleases,
      Regex("""(?<=ideaIC-)(\d+\.)+\d+(?=.pom)"""),
      version,
    )
  }

  private fun findLatestMinorOfVersion(versionNumber: String): String {
    val major = versionToMajorVersion(versionNumber)

    return findFirstMatchingVersionNewerThan(
      linksToIntellijReleases,
      Regex("""(?<=ideaIC-)$major(\.\d+)?(?=.pom)"""),
      major,
    ) ?: versionNumber
  }

  private fun findEapWithBuildNumberHigherThan(buildNumber: String): String? {
    return findFirstMatchingVersionNewerThan(
      linksToIntellijSnapshots,
      Regex("""(?<=ideaIC-)\d+\.\d+\.\d+(?=-EAP-SNAPSHOT\.pom)"""),
      buildNumber,
    )
  }

  @TaskAction
  fun execute() {
    val intellijVersions: IntellijVersions by project.rootProject.extra
    val originalVersions = intellijVersions
    var updatedVersions = originalVersions
    val latestMinorsOfOldSupportedMajors = originalVersions.latestMinorsOfOldSupportedMajors.map { findLatestMinorOfVersion(it) }

    if (latestMinorsOfOldSupportedMajors != originalVersions.latestMinorsOfOldSupportedMajors) {
      logger.lifecycle("latestMinorsOfOldSupportedMajors have been updated to $latestMinorsOfOldSupportedMajors")
      updatedVersions = updatedVersions.copy(latestMinorsOfOldSupportedMajors = latestMinorsOfOldSupportedMajors)
    }

    val latestStable = originalVersions.latestStable
    val newerStable = findReleaseNewerThan(latestStable)

    if (newerStable != null) {
      logger.lifecycle("latestStable has been updated to $newerStable")
      updatedVersions = updatedVersions.copy(latestStable = newerStable)

      if (versionToMajorVersion(latestStable) != versionToMajorVersion(newerStable)) {
        val newLatestMinors = latestMinorsOfOldSupportedMajors.plus(findLatestMinorOfVersion(latestStable))
        logger.lifecycle("latestMinorsOfOldSupportedMajors have been updated to $newLatestMinors")
        logger.lifecycle("eapOfLatestSupportedMajor has been cleared")
        updatedVersions = updatedVersions.copy(
          latestMinorsOfOldSupportedMajors = newLatestMinors,
          eapOfLatestSupportedMajor = null,
        )
      }
    }

    val buildNumberThreshold = updatedVersions.eapOfLatestSupportedMajor?.replace("-EAP-SNAPSHOT", "")
      ?: "${versionToBuildNumber(updatedVersions.latestStable)}.999999.999999"

    val newerEapBuildNumber = findEapWithBuildNumberHigherThan(buildNumberThreshold)

    if (newerEapBuildNumber != null) {
      logger.lifecycle("eapOfLatestSupportedMajor has been updated to $newerEapBuildNumber")
      updatedVersions = updatedVersions.copy(eapOfLatestSupportedMajor = "$newerEapBuildNumber-EAP-SNAPSHOT")
    }

    if (originalVersions != updatedVersions) {
      PropertiesHelper.storeProperties(updatedVersions.toProperties(), project.rootDir.resolve("intellij-versions.properties"))
    }
  }
}
