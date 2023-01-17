package com.virtuslab.gitmachete.buildsrc

import com.virtuslab.gitmachete.buildsrc.IntellijVersionHelper.versionIsNewerThan
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IntellijVersionHelperTest {
  @Test
  fun shouldRecognizeEqualVersions() {
    val version1 = "123.1234.56789"
    val version2 = "0.0.0"
    assertFalse(version1 versionIsNewerThan version1)
    assertFalse(version2 versionIsNewerThan version2)
  }

  @Test
  fun shouldRecognizeNotEqualVersions() {
    val olderVersion = "1111.11.111"
    val newerVersions = listOf("1112.11.111", "1111.12.111", "1111.11.112", "1111.11.111.0")

    for (newerVersion in newerVersions) assertTrue(newerVersion versionIsNewerThan olderVersion)
  }
}
