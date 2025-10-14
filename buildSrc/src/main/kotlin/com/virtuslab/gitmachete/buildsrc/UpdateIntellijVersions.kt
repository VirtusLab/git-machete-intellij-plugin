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
import java.net.URI

open class UpdateIntellijVersions : DefaultTask() {

  // TODO (#2145): check for releases of IU from 2025.3 onwards, but IC up to 2025.2
  private val intellijReleases: List<ReleaseVersion> by lazy {
    listIntelliJVersionsForType(code = "IC", type = "release", attribute = "version").map { ReleaseVersion(it) }
  }

  private val intellijSnapshots: List<BuildNumber> by lazy {
    listIntelliJVersionsForType(code = "IU", type = "eap", attribute = "build").map { BuildNumber(it) }
  }

  private fun <T : AnyVersion> findFirstMatchingVersionNewerThan(versions: List<T>, thresholdVersion: T): T? = versions.firstOrNull { it isNewerThan thresholdVersion }

  private fun findReleaseNewerThan(version: ReleaseVersion): ReleaseVersion? = findFirstMatchingVersionNewerThan(
    intellijReleases,
    version,
  )

  private fun findLatestMinorOfVersion(version: ReleaseVersion): ReleaseVersion {
    val major = version.toMajorVersion()
    return findFirstMatchingVersionNewerThan(
      intellijReleases.filter { it.value.startsWith(major.value) },
      version,
    ) ?: version
  }

  private fun findEapWithBuildNumberHigherThan(buildNumber: BuildNumber): BuildNumber? = findFirstMatchingVersionNewerThan(
    intellijSnapshots,
    buildNumber,
  )

  private fun fetchJson(url: String): String {
    val connection = (URI(url).toURL().openConnection() as? HttpURLConnection)!!
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

      if (latestStable.toMajorVersion() != newerStable.toMajorVersion()) {
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
      ?: BuildNumber("${updatedVersions.latestStable.toBuildNumber()}.999999.999999")

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
