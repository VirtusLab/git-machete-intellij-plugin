import com.virtuslab.gitmachete.buildsrc.UpdateEapBuildNumber.Companion.buildNumberIsNewerThan
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

class BuildNumberComparisonTest {
  @Test
  fun shouldRecognizeEqualVersions() {
    val version1 = "123.1234.56789"
    val version2 = "0.0.0"
    assertFalse(version1 buildNumberIsNewerThan version1)
    assertFalse(version2 buildNumberIsNewerThan version2)
  }

  @Test
  fun shouldRecognizeNotEqualVersions() {
    val olderVersion = "1111.11.111"
    val newerVersions = listOf("1112.11.111", "1111.12.111", "1111.11.112", "1111.11.111.0")

    for (newerVersion in newerVersions) assertTrue(newerVersion buildNumberIsNewerThan olderVersion)
  }
}
