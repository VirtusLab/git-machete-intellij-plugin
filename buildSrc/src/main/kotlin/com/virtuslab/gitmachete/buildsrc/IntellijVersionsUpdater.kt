package com.virtuslab.gitmachete.buildsrc

class IntellijVersionsUpdater(private val versionsProvider: IntelliJVersionsProvider) {

  // Let's check for releases of IU even up to 2025.2 (they seem to mimic IC releases 1:1 anyway),
  // even though we actually used to build against IntelliJ Community for those versions.
  private val intellijReleases: List<ReleaseVersion> by lazy {
    versionsProvider.listIntelliJVersionsForType(code = "IU", type = "release", attribute = "version")
      .map { ReleaseVersion(it) }
      .sortedWith(AnyVersion.descendingComparator())
  }

  private val intellijSnapshots: List<BuildNumber> by lazy {
    versionsProvider.listIntelliJVersionsForType(code = "IU", type = "eap", attribute = "build")
      .map { BuildNumber(it) }
      .sortedWith(AnyVersion.descendingComparator())
  }

  fun update(earliestSupportedMajor: ReleaseVersion): IntellijVersions {
    // As per https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#kotlin-standard-library:
    // "If a plugin supports multiple platform versions, it must (...) target the lowest bundled stdlib version".
    val kotlinLibraryVersions = versionsProvider.getKotlinLibraryVersionsForIntelliJ(earliestSupportedMajor.value)

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

    val upcomingMajorEap = intellijSnapshots.first().takeIf {
      it.toMajorVersion().value > latestStableMajor.value
    }

    return IntellijVersions(
      earliestSupportedMajor = earliestSupportedMajor,
      kotlinVersion = kotlinLibraryVersions.kotlinVersion,
      latestMinorsOfOldSupportedMajors = latestMinorsOfOldSupportedMajors,
      latestStable = latestStable,
      upcomingMajorEap = upcomingMajorEap,
      overrideBuildTarget = null,
    )
  }
}
