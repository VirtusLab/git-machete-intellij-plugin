package com.virtuslab.gitmachete.buildsrc

import org.junit.Assert.assertEquals
import org.junit.Test

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

    assertEquals(iv.resolveIntelliJVersions("latestMinorsOfOldSupportedMajors"), listOf("2020.3.4", "2021.1.3", "2021.2.4", "2021.3.3", "2022.1.4"))
    assertEquals(iv.resolveIntelliJVersions("eapOfLatestSupportedMajor"), listOf<String>())
    assertEquals(iv.resolveIntelliJVersions("earliestSupportedMajor"), listOf("2020.3"))
    assertEquals(iv.resolveIntelliJVersions("latestStable"), listOf("2022.2.2"))
    assertEquals(iv.resolveIntelliJVersions("buildTarget"), listOf("2022.2.2"))
    assertEquals(iv.resolveIntelliJVersions("latestSupportedMajor"), listOf("2022.2"))
  }
}
