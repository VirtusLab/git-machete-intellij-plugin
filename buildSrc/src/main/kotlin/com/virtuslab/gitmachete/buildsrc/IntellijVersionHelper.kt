package com.virtuslab.gitmachete.buildsrc

import java.io.File
import java.util.*

object IntellijVersionHelper {
  fun getFromBuildNumber(buildNumber: String): String {
    return "20${buildNumber.substring(0, 2)}.${buildNumber.substring(2, 3)}"
  }

  fun toBuildNumber(version: String): String {
    return version.substring(2, 6).filter { it != '.' }
  }

  fun getMajorPart(version: String): String {
    return version.substring(0, 6)
  }

  fun Properties.getPropertyOrNullIfEmpty(key: String): String? {
    val value = getProperty(key)
    return if (value == "") null else value
  }

  fun getProperties(): Properties {
    val properties = Properties()
    properties.load(getFile().inputStream())
    return properties
  }

  fun storeProperties(properties: Properties, comment: String? = null) {
    properties.store(getFile().writer(), comment)
  }

  private fun getFile(): File {
    return File("intellijVersions.properties")
  }

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
