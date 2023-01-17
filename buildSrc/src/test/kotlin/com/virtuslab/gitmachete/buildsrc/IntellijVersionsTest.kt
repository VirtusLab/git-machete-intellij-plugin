package com.virtuslab.gitmachete.buildsrc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IntellijVersionsTest {
  @Test
  fun shouldResolveIntelliJVersions() {
    val iv = IntellijVersions(
      latestMinorsOfOldSupportedMajors = listOf("2020.3.4", "2021.1.3", "2021.2.4", "2021.3.3", "2022.1.4"),
      eapOfLatestSupportedMajor = null,
      earliestSupportedMajor = "2020.3",
      latestStable = "2022.2.2",
      buildTarget = "2022.2.2",
      latestSupportedMajor = "2022.2"
    )

    assertEquals(listOf("2020.3.4", "2021.1.3", "2021.2.4", "2021.3.3", "2022.1.4"), iv.resolveIntelliJVersions("latestMinorsOfOldSupportedMajors"))
    assertEquals(listOf<String>(), iv.resolveIntelliJVersions("eapOfLatestSupportedMajor"))
    assertEquals(listOf("2020.3"), iv.resolveIntelliJVersions("earliestSupportedMajor"))
    assertEquals(listOf("2022.2.2"), iv.resolveIntelliJVersions("latestStable"))
    assertEquals(listOf("2022.2.2"), iv.resolveIntelliJVersions("buildTarget"))
    assertEquals(listOf("2022.2"), iv.resolveIntelliJVersions("latestSupportedMajor"))
  }
}
