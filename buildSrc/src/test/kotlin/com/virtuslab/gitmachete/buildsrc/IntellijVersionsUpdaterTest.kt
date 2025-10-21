package com.virtuslab.gitmachete.buildsrc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.Properties
import java.util.stream.Stream

class IntellijVersionsUpdaterTest {
  companion object {
    @JvmStatic
    fun testCases(): Stream<String> = Stream.of(
      "0-no-updates-available",
      "1-first-eap-of-upcoming-major",
      "2-new-minor-with-eap",
      "3-new-minor-no-eap",
      "4-newer-eap-for-same-major",
      "5-eap-major-becomes-stable",
    )
  }

  private fun loadVersionsFromResource(resourcePath: String): List<String> = javaClass.getResourceAsStream(resourcePath)?.bufferedReader()?.use { reader ->
    reader.readText().split(Regex("\\s+")).filter { it.isNotBlank() }
  } ?: emptyList()

  private fun loadPropertiesFromResource(resourcePath: String): Properties {
    val properties = Properties()
    javaClass.getResourceAsStream(resourcePath)?.use { properties.load(it) }
    return properties
  }

  @ParameterizedTest
  @MethodSource("testCases")
  fun shouldUpdateVersionsCorrectly(testCaseName: String) {
    val basePath = "/intellij-versions-test-cases/$testCaseName"

    // Load input data
    val iuReleaseVersions = loadVersionsFromResource("$basePath/INPUT.iu-release-versions.txt")
    val iuEapBuilds = loadVersionsFromResource("$basePath/INPUT.iu-eap-versions.txt")
    val inputProperties = loadPropertiesFromResource("$basePath/INPUT.intellij-versions.properties")
    val expectedOutputProperties = loadPropertiesFromResource("$basePath/OUTPUT.intellij-versions.properties")

    // Create mock provider and updater
    val mockProvider = MockIntelliJVersionsProvider(iuReleaseVersions, iuEapBuilds)
    val updater = IntellijVersionsUpdater(mockProvider)

    // Parse input versions from properties
    val originalVersions = IntellijVersions.from(inputProperties, null)

    // Run the update
    val updatedVersions = updater.update(originalVersions.earliestSupportedMajor)

    // Convert to properties and compare
    val actualOutputProperties = updatedVersions.toProperties()

    val propertyKeys = listOf(
      "earliestSupportedMajor",
      "kotlinVersion",
      "kotlinxSerializationJsonVersion",
      "latestMinorsOfOldSupportedMajors",
      "latestStable",
      "upcomingMajorEap",
    )

    for (key in propertyKeys) {
      assertEquals(
        expectedOutputProperties.getProperty(key),
        actualOutputProperties.getProperty(key),
        "$key mismatch",
      )
    }
  }
}
