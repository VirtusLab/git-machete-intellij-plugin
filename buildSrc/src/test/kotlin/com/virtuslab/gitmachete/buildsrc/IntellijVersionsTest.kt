package com.virtuslab.gitmachete.buildsrc

import com.virtuslab.gitmachete.buildsrc.AnyVersion.Companion.toPlainReleaseNumber
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Properties

class IntellijVersionsTest {
  @Test
  fun shouldResolveIntelliJVersions() {
    val iv = IntellijVersions(
      earliestSupportedMajor = ReleaseVersion("2020.3"),
      earliestSupportedMajorKotlinVersion = "1.9",
      latestMinorsOfOldSupportedMajors = listOf("2020.3.4", "2021.1.3", "2021.2.4", "2021.3.3", "2022.1.4").map { ReleaseVersion(it) },
      latestStable = ReleaseVersion("2022.2.2"),
      eapOfLatestSupportedMajor = null,
      latestSupportedMajor = ReleaseVersion("2022.2"),
      buildTarget = "2022.2.2",
    )

    assertEquals(listOf("2020.3.4", "2021.1.3", "2021.2.4", "2021.3.3", "2022.1.4"), iv.resolveIntelliJVersions("latestMinorsOfOldSupportedMajors"))
    assertEquals(listOf<String>(), iv.resolveIntelliJVersions("eapOfLatestSupportedMajor"))
    assertEquals(listOf("2020.3"), iv.resolveIntelliJVersions("earliestSupportedMajor"))
    assertEquals(listOf("2022.2.2"), iv.resolveIntelliJVersions("latestStable"))
    assertEquals(listOf("2022.2.2"), iv.resolveIntelliJVersions("buildTarget"))
    assertEquals(listOf("2022.2"), iv.resolveIntelliJVersions("latestSupportedMajor"))
  }

  @Test
  fun shouldRecognizeEqualVersions() {
    val version1 = BuildNumber("123.1234.56789")
    val version2 = BuildNumber("0.0.0")
    assertFalse(version1 isNewerThan version1)
    assertFalse(version2 isNewerThan version2)

    assertThrows<IllegalArgumentException> { version1 isNewerThan ReleaseVersion("2025.3") }
  }

  @Test
  fun shouldRecognizeNotEqualVersions() {
    val olderVersion = BuildNumber("1111.11.111")
    val newerVersions = listOf("1112.11.111", "1111.12.111", "1111.11.112", "1111.11.111.0").map { BuildNumber(it) }

    for (newerVersion in newerVersions) assertTrue(newerVersion isNewerThan olderVersion)
  }

  @Test
  fun shouldDerivePlainReleaseNumber() {
    assertEquals(242, "2024.2.6".toPlainReleaseNumber())
    assertEquals(253, "2025.3.1.2.3".toPlainReleaseNumber())
    assertEquals(253, "253.25908.13".toPlainReleaseNumber())
    assertEquals(261, "261.12345.6".toPlainReleaseNumber())
    assertThrows<IllegalArgumentException> { "12.34.56".toPlainReleaseNumber() }
  }

  private fun loadVersionsFromResource(resourcePath: String): List<String> = javaClass.getResourceAsStream(resourcePath)?.bufferedReader()?.use { reader ->
    reader.readText().split(Regex("\\s+")).filter { it.isNotBlank() }
  } ?: emptyList()

  private fun loadPropertiesFromResource(resourcePath: String): Properties {
    val properties = Properties()
    javaClass.getResourceAsStream(resourcePath)?.use { properties.load(it) }
    return properties
  }

  private fun runUpdateTest(testCaseName: String) {
    val basePath = "/intellij-versions-test-cases/$testCaseName"

    // Load input data
    val icReleaseVersions = loadVersionsFromResource("$basePath/INPUT.ic-release-versions.txt")
    val iuEapBuilds = loadVersionsFromResource("$basePath/INPUT.iu-eap-versions.txt")
    val inputProperties = loadPropertiesFromResource("$basePath/INPUT.intellij-versions.properties")
    val expectedOutputProperties = loadPropertiesFromResource("$basePath/OUTPUT.intellij-versions.properties")

    // Create mock provider and updater
    val mockProvider = MockIntelliJVersionsProvider(icReleaseVersions, iuEapBuilds)
    val updater = IntellijVersionsUpdater(mockProvider)

    // Parse input versions from properties
    val originalVersions = IntellijVersions.from(inputProperties, null)

    // Run the update
    val updatedVersions = updater.update(originalVersions)

    // Convert to properties and compare
    val actualOutputProperties = updatedVersions.toProperties()

    val propertyKeys = listOf(
      "earliestSupportedMajor",
      "earliestSupportedMajorKotlinVersion",
      "latestMinorsOfOldSupportedMajors",
      "latestStable",
      "eapOfLatestSupportedMajor",
    )

    for (key in propertyKeys) {
      assertEquals(
        expectedOutputProperties.getProperty(key),
        actualOutputProperties.getProperty(key),
        "$key mismatch",
      )
    }
  }

  @Test
  fun shouldNotUpdateWhenNoNewVersionsAvailable() {
    runUpdateTest("no-updates-available")
  }
}
