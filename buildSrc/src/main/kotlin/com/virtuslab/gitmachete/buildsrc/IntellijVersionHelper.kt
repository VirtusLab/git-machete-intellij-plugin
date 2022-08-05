package com.virtuslab.gitmachete.buildsrc

import java.io.File
import java.util.*
import kotlin.reflect.full.memberProperties

// TODO (#1004): Remove unsafe casts from class
object IntellijVersionHelper {
  /**
   * @param versionKey Either release number (like 2020.3) or key of intellijVersions (like
   * eapOfLatestSupportedMajor)
   * @returns Corresponding release numbers.
   */
  fun resolveIntelliJVersions(versionKey: String?): List<String> {
    if (versionKey == null) {
      return emptyList()
    }
    val regex = "/^[0-9].*$/".toRegex()
    if (regex.matches(versionKey)) {
      return listOf(versionKey)
    }

    val propertiesList = listOfNotNull(
      IntellijVersions::class.memberProperties
        .single { it.name == versionKey }
        .get(IntellijVersions::class.objectInstance!!)
    )

    return propertiesList.flatMap { if (it is List<*>) it else listOf(it) }.map { it as String }
  }

  fun getFromBuildNumber(buildNumber: String): String {
    return "20${buildNumber.substring(0, 2)}.${buildNumber.substring(2, 3)}"
  }

  fun toBuildNumber(version: String): String {
    return version.substring(2, 6).filter { it != '.' }
  }

  fun getMajorPart(version: String): String {
    return version.substring(0, 6)
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
}
