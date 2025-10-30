package com.virtuslab.gitmachete.buildsrc

import com.virtuslab.gitmachete.buildsrc.AnyVersion.Companion.toPlainReleaseNumber
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class IntellijVersionsTest {
  @Test
  fun shouldResolveIntelliJVersions() {
    val iv = IntellijVersions(
      earliestSupportedMajor = ReleaseVersion("2020.3"),
      kotlinVersion = "1.9",
      kotlinxSerializationJsonVersion = "1.6.3",
      latestMinorsOfOldSupportedMajors = listOf("2020.3.4", "2021.1.3", "2021.2.4", "2021.3.3", "2022.1.4").map { ReleaseVersion(it) },
      latestStable = ReleaseVersion("2022.2.2"),
      upcomingMajorEap = null,
      overrideBuildTarget = null,
    )

    assertEquals(listOf("2020.3.4", "2021.1.3", "2021.2.4", "2021.3.3", "2022.1.4"), iv.resolveIntelliJVersions("latestMinorsOfOldSupportedMajors"))
    assertEquals(listOf<String>(), iv.resolveIntelliJVersions("upcomingMajorEap"))
    assertEquals(listOf("2020.3"), iv.resolveIntelliJVersions("earliestSupportedMajor"))
    assertEquals(listOf("2022.2.2"), iv.resolveIntelliJVersions("latestStable"))
    assertEquals(listOf("2022.2.2"), iv.resolveIntelliJVersions("buildTarget"))
    assertEquals(listOf("2022.2"), iv.resolveIntelliJVersions("latestSupportedMajor"))
  }

  @Test
  fun shouldResolveIntelliJVersionsWithOverrideBuildTarget() {
    val iv = IntellijVersions(
      earliestSupportedMajor = ReleaseVersion("2020.3"),
      kotlinVersion = "1.9",
      kotlinxSerializationJsonVersion = "1.6.3",
      latestMinorsOfOldSupportedMajors = listOf("2020.3.4", "2021.1.3", "2021.2.4", "2021.3.3", "2022.1.4").map { ReleaseVersion(it) },
      latestStable = ReleaseVersion("2022.2.2"),
      upcomingMajorEap = BuildNumber("223.7571.182"),
      overrideBuildTarget = "2021.3.2",
    )

    assertEquals(listOf("2020.3.4", "2021.1.3", "2021.2.4", "2021.3.3", "2022.1.4"), iv.resolveIntelliJVersions("latestMinorsOfOldSupportedMajors"))
    assertEquals(listOf("223.7571.182"), iv.resolveIntelliJVersions("upcomingMajorEap"))
    assertEquals(listOf("2020.3"), iv.resolveIntelliJVersions("earliestSupportedMajor"))
    assertEquals(listOf("2022.2.2"), iv.resolveIntelliJVersions("latestStable"))
    assertEquals(listOf("2021.3.2"), iv.resolveIntelliJVersions("buildTarget"))
    assertEquals(listOf("2022.3"), iv.resolveIntelliJVersions("latestSupportedMajor"))
  }

  @Test
  fun shouldRecognizeEqualVersions() {
    val version1 = BuildNumber("123.1234.56789")
    val version2 = BuildNumber("0.0.0")
    assertEquals(0, version1 compareTo version1)
    assertEquals(0, version2 compareTo version2)
  }

  @Test
  fun shouldRecognizeNotEqualVersions() {
    val olderVersion = BuildNumber("1111.11.111")
    val newerVersions = listOf("1112.11.111", "1111.12.111", "1111.11.112", "1111.11.111.0").map { BuildNumber(it) }

    for (newerVersion in newerVersions) assertTrue((newerVersion compareTo olderVersion) > 0)
  }

  @Test
  fun shouldDerivePlainReleaseNumber() {
    assertEquals(242, "2024.2.6".toPlainReleaseNumber())
    assertEquals(253, "2025.3.1.2.3".toPlainReleaseNumber())
    assertEquals(253, "253.25908.13".toPlainReleaseNumber())
    assertEquals(261, "261.12345.6".toPlainReleaseNumber())
    assertThrows<IllegalArgumentException> { "12.34.56".toPlainReleaseNumber() }
  }
}
