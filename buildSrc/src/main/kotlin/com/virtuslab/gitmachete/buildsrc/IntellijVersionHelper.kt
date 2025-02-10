package com.virtuslab.gitmachete.buildsrc

object IntellijVersionHelper {
  fun buildNumberToMajorVersion(buildNumber: String): String = "20${buildNumber.substring(0, 2)}.${buildNumber.substring(2, 3)}"

  fun versionToBuildNumber(version: String): String = version.substring(2, 6).filter { it != '.' }

  fun versionToMajorVersion(version: String): String = version.substring(0, 6)

  infix fun String.versionIsNewerThan(rhsVersion: String): Boolean {
    val lhsSplit = this.split('.')
    val rhsSplit = rhsVersion.split('.')

    val firstDiff = lhsSplit.zip(rhsSplit).find { it.first != it.second }

    // 8.0.6 is older than 8.0.6.0, but zipped they will look like this: [(8,8), (0,0), (6,6)]
    if (firstDiff == null) {
      return lhsSplit.size > rhsSplit.size
    }

    return Integer.parseInt(firstDiff.first) > Integer.parseInt(firstDiff.second)
  }
}
