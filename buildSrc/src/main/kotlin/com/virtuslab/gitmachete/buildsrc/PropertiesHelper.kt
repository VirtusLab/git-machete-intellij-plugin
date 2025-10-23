package com.virtuslab.gitmachete.buildsrc

import org.gradle.internal.util.PropertiesUtils
import java.io.File
import java.util.*

object PropertiesHelper {
  fun Properties.getPropertyOrNullIfEmpty(key: String): String? = getProperty(key).takeIf { it != "" }

  fun getProperties(file: File): Properties = Properties().apply {
    load(file.inputStream())
  }

  fun storeProperties(properties: Properties, file: File) {
    PropertiesUtils.store(properties, file)
  }
}
