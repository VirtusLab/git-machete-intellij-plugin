package com.virtuslab.gitmachete.buildsrc

class IntellijVersionsUpdater(private val versionsProvider: IntelliJVersionsProvider) {

  // TODO (#2145): check for releases of IU from 2025.3 onwards, but IC up to 2025.2
  // Sorted in descending order (newest first)
  private val intellijReleases: List<ReleaseVersion> by lazy {
    versionsProvider.listIntelliJVersionsForType(code = "IC", type = "release", attribute = "version")
      .map { ReleaseVersion(it) }
      .sortedWith(AnyVersion.descendingComparator())
  }

  // Sorted in descending order (newest first)
  private val intellijSnapshots: List<BuildNumber> by lazy {
    versionsProvider.listIntelliJVersionsForType(code = "IU", type = "eap", attribute = "build")
      .map { BuildNumber(it) }
      .sortedWith(AnyVersion.descendingComparator())
  }

  fun update(earliestSupportedMajor: ReleaseVersion): IntellijVersions {
    // As per https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#kotlin-standard-library:
    // "If a plugin supports multiple platform versions, it must (...) target the lowest bundled stdlib version".
    val earliestSupportedMajorKotlinVersion = versionsProvider.getKotlinVersionForIntelliJ(earliestSupportedMajor.value)

    // Find the latest stable release
    val latestStable = intellijReleases.first()
    val latestStableMajor = latestStable.toMajorVersion()

    // Find latest minors for all major versions between earliestSupportedMajor (inclusive) and latestStable (exclusive)
    val latestMinorsOfOldSupportedMajors = intellijReleases
      .groupBy { it.toMajorVersion().value }
      .filterKeys { it >= earliestSupportedMajor.value && it < latestStableMajor.value }
      .values
      .map { it.first() }
      .sortedWith(AnyVersion.descendingComparator())
      .reversed()

    val eapOfLatestSupportedMajor = intellijSnapshots.first().takeIf {
      it.toMajorVersion().value > latestStableMajor.value
    }

    return IntellijVersions(
      earliestSupportedMajor = earliestSupportedMajor,
      earliestSupportedMajorKotlinVersion = earliestSupportedMajorKotlinVersion,
      latestMinorsOfOldSupportedMajors = latestMinorsOfOldSupportedMajors,
      latestStable = latestStable,
      eapOfLatestSupportedMajor = eapOfLatestSupportedMajor,
      overrideBuildTarget = null,
    )
  }
}
