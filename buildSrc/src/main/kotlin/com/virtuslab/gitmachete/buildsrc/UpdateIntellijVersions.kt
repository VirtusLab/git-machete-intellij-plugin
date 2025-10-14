package com.virtuslab.gitmachete.buildsrc

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.provideDelegate
import java.net.HttpURLConnection
import java.net.URL

open class UpdateIntellijVersions : DefaultTask() {

  // TODO (#2145): check for releases of IU from 2025.3 onwards, but IC up to 2025.2
  private val intellijReleases: List<String> by lazy {
    listIntelliJVersionsForType(code = "IC", type = "release", attribute = "version")
  }

  private val intellijSnapshots: List<String> by lazy {
    listIntelliJVersionsForType(code = "IU", type = "eap", attribute = "build")
  }

  private fun findFirstMatchingVersionNewerThan(versions: List<String>, thresholdVersion: String): String? = versions.firstOrNull { it versionIsNewerThan thresholdVersion }

  private fun findReleaseNewerThan(version: String): String? = findFirstMatchingVersionNewerThan(
    intellijReleases,
    version,
  )

  private fun findLatestMinorOfVersion(version: String): String {
    val major = version.versionToMajorVersion()
    return findFirstMatchingVersionNewerThan(
      intellijReleases.filter { it.startsWith(major) },
      version,
    ) ?: version
  }

  private fun findEapWithBuildNumberHigherThan(buildNumber: String): String? = findFirstMatchingVersionNewerThan(
    intellijSnapshots,
    buildNumber,
  )

  private fun fetchJson(url: String): String {
    val connection = (URL(url).openConnection() as? HttpURLConnection)!!
    connection.requestMethod = "GET"
    connection.connectTimeout = 5000
    connection.readTimeout = 5000

    return connection.inputStream.bufferedReader().use { it.readText() }
  }

  private fun listIntelliJVersionsForType(code: String, type: String, attribute: String): List<String> {
    val url = "https://data.services.jetbrains.com/products?code=$code&type=$type"
    val jsonString = fetchJson(url)

    val json = Json { ignoreUnknownKeys = true }
    val jsonElement = json.parseToJsonElement(jsonString)

    val releaseElements = jsonElement.jsonArray[0].jsonObject["releases"]?.jsonArray?.toList() ?: listOf()
    val result = releaseElements.mapNotNull { it.jsonObject[attribute]?.jsonPrimitive?.content }
    println("listIntelliJVersionsForType(code=$code, type=$type, attribute=$attribute) = $result\n")
    return result
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

      if (latestStable.versionToMajorVersion() != newerStable.versionToMajorVersion()) {
        val newLatestMinors = latestMinorsOfOldSupportedMajors.plus(findLatestMinorOfVersion(latestStable))
        logger.lifecycle("latestMinorsOfOldSupportedMajors have been updated to $newLatestMinors")
        logger.lifecycle("eapOfLatestSupportedMajor has been cleared")
        updatedVersions = updatedVersions.copy(
          latestMinorsOfOldSupportedMajors = newLatestMinors,
          eapOfLatestSupportedMajor = null,
        )
      }
    }

    val buildNumberThreshold = updatedVersions.eapOfLatestSupportedMajor
      ?: "${updatedVersions.latestStable.versionToBuildNumber()}.999999.999999"

    val newerEapBuildNumber = findEapWithBuildNumberHigherThan(buildNumberThreshold)

    if (newerEapBuildNumber != null) {
      logger.lifecycle("eapOfLatestSupportedMajor has been updated to $newerEapBuildNumber")
      updatedVersions = updatedVersions.copy(eapOfLatestSupportedMajor = newerEapBuildNumber)
    }

    if (originalVersions != updatedVersions) {
      PropertiesHelper.storeProperties(
        updatedVersions.toProperties(),
        project.rootDir.resolve("intellij-versions.properties"),
      )
    }
  }
}
