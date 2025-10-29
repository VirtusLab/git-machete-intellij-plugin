package com.virtuslab.gitmachete.buildsrc

import com.virtuslab.gitmachete.buildsrc.PropertiesHelper.getPropertyOrNullIfEmpty
import java.lang.IllegalStateException
import java.util.Properties
import kotlin.reflect.full.memberProperties

sealed interface AnyVersion {
  val value: String

  /**
   * Compares this version with another version.
   * @return negative if this < other, zero if this == other, positive if this > other
   */
  infix fun compareTo(rhsVersion: AnyVersion): Int {
    if (rhsVersion.javaClass != this.javaClass) {
      throw IllegalArgumentException("$this and $rhsVersion cannot be compared")
    }
    val lhsSplit = value.split('.')
    val rhsSplit = rhsVersion.value.split('.')

    val firstDiff = lhsSplit.zip(rhsSplit).find { it.first != it.second }

    // 8.0.6 is older than 8.0.6.0, but zipped they will look like this: [(8,8), (0,0), (6,6)]
    if (firstDiff == null) {
      return lhsSplit.size.compareTo(rhsSplit.size)
    }

    return Integer.parseInt(firstDiff.first).compareTo(Integer.parseInt(firstDiff.second))
  }

  companion object {
    fun <T : AnyVersion> descendingComparator(): Comparator<T> = Comparator { a, b -> -(a compareTo b) }

    fun String.toPlainReleaseNumber(): Int {
      if ("""\d\d\d\.[.\d]+""".toRegex().matches(this)) {
        return this.take(3).toInt()
      } else if ("""\d\d\d\d\.\d[.\d]*""".toRegex().matches(this)) {
        return "${this[2]}${this[3]}${this[5]}".toInt()
      } else {
        throw IllegalArgumentException("Not a build number or release: $this")
      }
    }

    // TODO (#2146): drop support for IntelliJ Community
    // IntelliJ 2025.3 removed the distinction between Community and Ultimate
    fun String.productCode(): String = if (toPlainReleaseNumber() >= 253) "IU" else "IC"

    fun String.withProductCode(): String = "${productCode()}-$this"
  }
}

data class BuildNumber(override val value: String) : AnyVersion {
  fun toMajorVersion(): ReleaseVersion = ReleaseVersion("20${value.take(2)}.${value[2]}")
  override fun toString() = value
}

data class ReleaseVersion(override val value: String) : AnyVersion {
  fun toBuildNumber(): BuildNumber = BuildNumber(value.substring(2, 6).filter { it != '.' })
  fun toMajorVersion(): ReleaseVersion = ReleaseVersion(value.take(6))
  override fun toString() = value
}

// See https://www.jetbrains.com/intellij-repository/releases/ -> Ctrl+F .idea
data class IntellijVersions(
  val earliestSupportedMajor: ReleaseVersion,
  val kotlinVersion: String,
  val kotlinxSerializationJsonVersion: String,
  val latestMinorsOfOldSupportedMajors: List<ReleaseVersion>,
  val latestStable: ReleaseVersion,
  val upcomingMajorEap: BuildNumber?,
  val overrideBuildTarget: String?,
) {
  val latestSupportedMajor: ReleaseVersion = (upcomingMajorEap?.toMajorVersion() ?: latestStable.toMajorVersion())

  // This allows to change the target IntelliJ version
  // by using a project property 'overrideBuildTarget' while running tasks like runIde
  val buildTarget: String = overrideBuildTarget ?: upcomingMajorEap?.value ?: latestStable.value

  companion object {
    fun from(intellijVersionsProperties: Properties, overrideBuildTarget: String?): IntellijVersions {
      // When this value is updated, remember to update:
      // 1. the minimum required IDEA version in README.md
      // 2. version of Kotlin and kotlinx-serialization-json - automatically via `./gradlew updateIntellijVersions`
      // Note that after bumping `earliestSupportedMajor` from AAAA.B to CCCC.D (CCCC.D is later)
      // the released plugin versions supporting AAAA.B remain available in JetBrains Marketplace.
      // Dropping a support for an IntelliJ version is less painful then,
      // since most likely some plugin version will still be downloadable (however not the latest).
      // Marking a release version as hidden is a way to forbid its download
      // (see https://plugins.jetbrains.com/plugin/14221-git-machete/versions).
      val earliestSupportedMajor = ReleaseVersion(intellijVersionsProperties.getProperty("earliestSupportedMajor"))
      // Every time `earliestSupportedMajor` is bumped, this should be bumped (using ./gradle updateIntellijVersions)
      // to the Kotlin version listed for `earliestSupportedMajor`
      // in https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#kotlin-standard-library
      val kotlinVersion: String = intellijVersionsProperties.getProperty("kotlinVersion")
      val kotlinxSerializationJsonVersion: String = intellijVersionsProperties.getProperty("kotlinxSerializationJsonVersion")

      // Most recent minor versions of all major releases between the earliest supported (incl.)
      // and latest stable (excl.), used for binary compatibility checks and UI tests
      val latestMinorsOfOldSupportedMajors: List<ReleaseVersion> = intellijVersionsProperties.getProperty("latestMinorsOfOldSupportedMajors").split(",").map { ReleaseVersion(it) }

      val latestStable = ReleaseVersion(intellijVersionsProperties.getProperty("latestStable"))

      // Note that we have to use a "fixed snapshot" version X.Y.Z-EAP-SNAPSHOT (e.g. 211.4961.33-EAP-SNAPSHOT)
      // rather than a "rolling snapshot" X-EAP-SNAPSHOT (e.g. 211-EAP-SNAPSHOT)
      // to ensure that the builds are reproducible.
      // EAP-CANDIDATE-SNAPSHOTs apparently canNOT be used for either binary compatibility checks or UI tests.
      // Generally, see https://www.jetbrains.com/intellij-repository/snapshots/ -> Ctrl+F .idea
      // Use `null` if the latest supported major has a stable release (and not just EAPs).
      val upcomingMajorEap: BuildNumber? = intellijVersionsProperties.getPropertyOrNullIfEmpty("upcomingMajorEap")
        ?.let { BuildNumber(it) }

      return IntellijVersions(
        earliestSupportedMajor = earliestSupportedMajor,
        kotlinVersion = kotlinVersion,
        kotlinxSerializationJsonVersion = kotlinxSerializationJsonVersion,
        latestMinorsOfOldSupportedMajors = latestMinorsOfOldSupportedMajors,
        latestStable = latestStable,
        upcomingMajorEap = upcomingMajorEap,
        overrideBuildTarget = overrideBuildTarget,
      )
    }
  }

  /**
   * @param versionKey Either release number (like 2020.3) or key of intellijVersions (like upcomingMajorEap)
   * @returns Corresponding release numbers.
   */
  fun resolveIntelliJVersions(versionKey: String): List<String> {
    val regex = "^[0-9].*$".toRegex()
    if (regex.matches(versionKey)) {
      return listOf(versionKey)
    }

    val propertyValue: Any? =
      IntellijVersions::class.memberProperties
        .single { it.name == versionKey }
        .get(this)

    return when (propertyValue) {
      null -> listOf()
      is String -> listOf(propertyValue)
      is AnyVersion -> listOf(propertyValue.value)
      is List<*> -> propertyValue.mapNotNull { it as? AnyVersion }.map { it.value }
      else -> throw IllegalStateException("Unexpected property value found for $versionKey: $propertyValue")
    }
  }

  fun toProperties(): Properties {
    val p = Properties()
    p.setProperty("upcomingMajorEap", upcomingMajorEap?.value ?: "")
    p.setProperty("earliestSupportedMajor", earliestSupportedMajor.value)
    p.setProperty("kotlinVersion", kotlinVersion)
    p.setProperty("kotlinxSerializationJsonVersion", kotlinxSerializationJsonVersion)
    p.setProperty("latestMinorsOfOldSupportedMajors", latestMinorsOfOldSupportedMajors.joinToString(separator = ",") { it.value })
    p.setProperty("latestStable", latestStable.value)
    return p
  }
}
