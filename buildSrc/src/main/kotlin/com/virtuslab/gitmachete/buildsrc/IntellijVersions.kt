package com.virtuslab.gitmachete.buildsrc

import com.virtuslab.gitmachete.buildsrc.PropertiesHelper.getPropertyOrNullIfEmpty
import java.lang.IllegalStateException
import java.util.Properties
import kotlin.reflect.full.memberProperties

// See https://www.jetbrains.com/intellij-repository/releases/ -> Ctrl+F .idea
data class IntellijVersions(
  val earliestSupportedMajor: String,
  val latestMinorsOfOldSupportedMajors: List<String>,
  val latestStable: String,
  val eapOfLatestSupportedMajor: String?,
  val latestSupportedMajor: String,
  val buildTarget: String
) {
  companion object {
    fun from(intellijVersionsProperties: Properties, overrideBuildTarget: String?): IntellijVersions {
      // When this value is updated, remember to update:
      // 1. the minimum required IDEA version in README.md,
      // 2. version of Gradle Kotlin plugin in gradle/libs.versions.toml
      // Note that after bumping `earliestSupportedMajor`from A.B to C.D (C.D is later)
      // the released plugin versions supporting A.B remain available in JetBrains Marketplace.
      // Dropping a support for an IntelliJ version is less painful then,
      // since most likely some plugin version will still be downloadable (however not the latest).
      // Marking a release version as hidden is a way to forbid its download
      // (see https://plugins.jetbrains.com/plugin/14221-git-machete/versions).
      val earliestSupportedMajor: String = intellijVersionsProperties.getProperty("earliestSupportedMajor")

      // Most recent minor versions of all major releases between earliest supported (incl.)
      // and latest stable (excl.), used for binary compatibility checks and UI tests
      val latestMinorsOfOldSupportedMajors: List<String> = intellijVersionsProperties.getProperty("latestMinorsOfOldSupportedMajors").split(",")

      val latestStable: String = intellijVersionsProperties.getProperty("latestStable")

      // Note that we have to use a "fixed snapshot" version X.Y.Z-EAP-SNAPSHOT (e.g. 211.4961.33-EAP-SNAPSHOT)
      // rather than a "rolling snapshot" X-EAP-SNAPSHOT (e.g. 211-EAP-SNAPSHOT)
      // to ensure that the builds are reproducible.
      // EAP-CANDIDATE-SNAPSHOTs apparently canNOT be used for either binary compatibility checks or UI tests.
      // Generally, see https://www.jetbrains.com/intellij-repository/snapshots/ -> Ctrl+F .idea
      // Use `null` if the latest supported major has a stable release (and not just EAPs).
      val eapOfLatestSupportedMajor: String? = intellijVersionsProperties.getPropertyOrNullIfEmpty("eapOfLatestSupportedMajor")

      val latestSupportedMajor: String = if (eapOfLatestSupportedMajor != null) {
        IntellijVersionHelper.buildNumberToMajorVersion(eapOfLatestSupportedMajor)
      } else {
        IntellijVersionHelper.versionToMajorVersion(latestStable)
      }

      // This allows to change the target IntelliJ version
      // by using a project property 'overrideBuildTarget' while running tasks like runIde
      val buildTarget: String = overrideBuildTarget ?: eapOfLatestSupportedMajor ?: latestStable

      return IntellijVersions(
        earliestSupportedMajor = earliestSupportedMajor,
        latestMinorsOfOldSupportedMajors = latestMinorsOfOldSupportedMajors,
        latestStable = latestStable,
        eapOfLatestSupportedMajor = eapOfLatestSupportedMajor,
        latestSupportedMajor = latestSupportedMajor,
        buildTarget = buildTarget
      )
    }
  }

  /**
   * @param versionKey Either release number (like 2020.3) or key of intellijVersions (like
   * eapOfLatestSupportedMajor)
   * @returns Corresponding release numbers.
   */
  fun resolveIntelliJVersions(versionKey: String?): List<String> {
    if (versionKey == null) {
      return emptyList()
    }
    val regex = "^[0-9].*$".toRegex()
    if (regex.matches(versionKey)) {
      return listOf(versionKey)
    }

    val propertyValue: Any? =
      IntellijVersions::class.memberProperties
        .single { it.name == versionKey }
        .get(this)

    if (propertyValue == null) {
      return listOf()
    } else if (propertyValue is String) {
      return listOf(propertyValue)
    } else if (propertyValue is List<*>) {
      return propertyValue.mapNotNull { it as? String }
    } else {
      throw IllegalStateException("Unexpected property value found for $versionKey: $propertyValue")
    }
  }

  fun toProperties(): Properties {
    val p = Properties()
    p.setProperty("eapOfLatestSupportedMajor", eapOfLatestSupportedMajor ?: "")
    p.setProperty("earliestSupportedMajor", earliestSupportedMajor)
    p.setProperty("latestMinorsOfOldSupportedMajors", latestMinorsOfOldSupportedMajors.joinToString(separator = ","))
    p.setProperty("latestStable", latestStable)
    return p
  }
}
