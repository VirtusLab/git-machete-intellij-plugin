package com.virtuslab.gitmachete.buildsrc

import org.gradle.internal.util.PropertiesUtils
import java.io.File
import java.util.*

object PropertiesHelper {
  fun Properties.getPropertyOrNullIfEmpty(key: String): String? {
    val value = getProperty(key)
    return if (value == "") null else value
  }

  fun getProperties(file: File): Properties {
    val properties = Properties()
    properties.load(file.inputStream())
    return properties
  }

  fun storeProperties(properties: Properties, file: File) {
    PropertiesUtils.store(properties, file)
  }
}
