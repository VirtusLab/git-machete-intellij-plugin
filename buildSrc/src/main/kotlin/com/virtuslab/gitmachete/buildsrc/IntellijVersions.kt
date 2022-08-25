package com.virtuslab.gitmachete.buildsrc

import com.virtuslab.gitmachete.buildsrc.IntellijVersionHelper.getPropertyOrNullIfEmpty

// See https://www.jetbrains.com/intellij-repository/releases/ -> Ctrl+F .idea
object IntellijVersions {

  private val intellijVersionsProp = IntellijVersionHelper.getProperties()

  // When this value is updated, remember to update:
  // 1. the minimum required IDEA version in README.md,
  // 2. version of Gradle Kotlin plugin in gradle/libs.versions.toml
  // Note that after bumping X.Y to A.B (A.B is later) the released plugin versions supporting X.Y remain available.
  // Dropping a support for an intellij version is less painful then,
  // since most likely some plugin version will still be downloadable (however not the latest).
  // Marking a release version as hidden is a way to forbid its download
  // (see https://plugins.jetbrains.com/plugin/14221-git-machete/versions).
  val earliestSupportedMajor: String = intellijVersionsProp.getProperty("earliestSupportedMajor")

  // Most recent minor versions of all major releases between earliest supported (incl.)
  // and latest stable (excl.), used for binary compatibility checks and UI tests
  val latestMinorsOfOldSupportedMajors: List<String> = intellijVersionsProp.getProperty("latestMinorsOfOldSupportedMajors").split(",")

  val latestStable: String = intellijVersionsProp.getProperty("latestStable")

  // Note that we have to use a "fixed snapshot" version X.Y.Z-EAP-SNAPSHOT (e.g. 211.4961.33-EAP-SNAPSHOT)
  // rather than a "rolling snapshot" X-EAP-SNAPSHOT (e.g. 211-EAP-SNAPSHOT)
  // to ensure that the builds are reproducible.
  // EAP-CANDIDATE-SNAPSHOTs can be used for binary compatibility checks,
  // but for some reason aren't resolved in UI tests.
  // Generally, see https://www.jetbrains.com/intellij-repository/snapshots/ -> Ctrl+F .idea
  // Use `null` if the latest supported major has a stable release (and not just EAPs).
  val eapOfLatestSupportedMajor: String? = intellijVersionsProp.getPropertyOrNullIfEmpty("eapOfLatestSupportedMajor")

  val latestSupportedMajor: String = if (eapOfLatestSupportedMajor != null) {
    IntellijVersionHelper.getFromBuildNumber(eapOfLatestSupportedMajor)
  } else {
    IntellijVersionHelper.getMajorPart(latestStable)
  }

  val buildTarget: String = eapOfLatestSupportedMajor ?: latestStable
}
