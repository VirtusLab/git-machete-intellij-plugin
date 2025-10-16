package com.virtuslab.gitmachete.buildsrc

class IntellijVersionsUpdater(private val versionsProvider: IntelliJVersionsProvider) {

  // TODO (#2145): check for releases of IU from 2025.3 onwards, but IC up to 2025.2
  private val intellijReleases: List<ReleaseVersion> by lazy {
    versionsProvider.listIntelliJVersionsForType(code = "IC", type = "release", attribute = "version").map { ReleaseVersion(it) }
  }

  private val intellijSnapshots: List<BuildNumber> by lazy {
    versionsProvider.listIntelliJVersionsForType(code = "IU", type = "eap", attribute = "build").map { BuildNumber(it) }
  }

  private fun <T : AnyVersion> findFirstMatchingVersionNewerThan(versions: List<T>, thresholdVersion: T): T? = versions.firstOrNull { it isNewerThan thresholdVersion }

  private fun findReleaseNewerThan(version: ReleaseVersion): ReleaseVersion? = findFirstMatchingVersionNewerThan(intellijReleases, version)

  private fun findLatestMinorOfVersion(version: ReleaseVersion): ReleaseVersion {
    val major = version.toMajorVersion()
    return findFirstMatchingVersionNewerThan(
      intellijReleases.filter { it.value.startsWith(major.value) },
      version,
    ) ?: version
  }

  private fun findEapWithBuildNumberHigherThan(buildNumber: BuildNumber): BuildNumber? = findFirstMatchingVersionNewerThan(intellijSnapshots, buildNumber)

  fun update(originalVersions: IntellijVersions, logger: ((String) -> Unit)? = null): IntellijVersions {
    var updatedVersions = originalVersions

    // As per https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#kotlin-standard-library:
    // "If a plugin supports multiple platform versions, it must (...) target the lowest bundled stdlib version".
    val earliestSupportedMajorKotlinVersion = versionsProvider.getKotlinVersionForIntelliJ(originalVersions.earliestSupportedMajor.value)
    if (earliestSupportedMajorKotlinVersion != originalVersions.earliestSupportedMajorKotlinVersion) {
      logger?.invoke("earliestSupportedMajorKotlinVersion has been updated to $earliestSupportedMajorKotlinVersion")
      updatedVersions = updatedVersions.copy(earliestSupportedMajorKotlinVersion = earliestSupportedMajorKotlinVersion)
    }

    val latestMinorsOfOldSupportedMajors = originalVersions.latestMinorsOfOldSupportedMajors.map { findLatestMinorOfVersion(it) }

    if (latestMinorsOfOldSupportedMajors != originalVersions.latestMinorsOfOldSupportedMajors) {
      logger?.invoke("latestMinorsOfOldSupportedMajors have been updated to $latestMinorsOfOldSupportedMajors")
      updatedVersions = updatedVersions.copy(latestMinorsOfOldSupportedMajors = latestMinorsOfOldSupportedMajors)
    }

    val latestStable = originalVersions.latestStable
    val newerStable = findReleaseNewerThan(latestStable)

    if (newerStable != null) {
      logger?.invoke("latestStable has been updated to $newerStable")
      updatedVersions = updatedVersions.copy(latestStable = newerStable)

      if (latestStable.toMajorVersion() != newerStable.toMajorVersion()) {
        val newLatestMinors = latestMinorsOfOldSupportedMajors.plus(findLatestMinorOfVersion(latestStable))
        logger?.invoke("latestMinorsOfOldSupportedMajors have been updated to $newLatestMinors")
        logger?.invoke("eapOfLatestSupportedMajor has been cleared")
        updatedVersions = updatedVersions.copy(
          latestMinorsOfOldSupportedMajors = newLatestMinors,
          eapOfLatestSupportedMajor = null,
        )
      }
    }

    val buildNumberThreshold = updatedVersions.eapOfLatestSupportedMajor
      ?: BuildNumber("${updatedVersions.latestStable.toBuildNumber().value}.999999.999999")

    val newerEapBuildNumber = findEapWithBuildNumberHigherThan(buildNumberThreshold)

    if (newerEapBuildNumber != null) {
      logger?.invoke("eapOfLatestSupportedMajor has been updated to $newerEapBuildNumber")
      updatedVersions = updatedVersions.copy(eapOfLatestSupportedMajor = newerEapBuildNumber)
    }

    return updatedVersions
  }
}
