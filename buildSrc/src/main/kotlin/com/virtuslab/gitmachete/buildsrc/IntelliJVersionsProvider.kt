package com.virtuslab.gitmachete.buildsrc

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URI

interface IntelliJVersionsProvider {
  fun listIntelliJVersionsForType(code: String, type: String, attribute: String): List<String>
}

class RealIntelliJVersionsProvider : IntelliJVersionsProvider {
  private fun fetchJson(url: String): String {
    val connection = (URI(url).toURL().openConnection() as? HttpURLConnection)!!
    connection.requestMethod = "GET"
    connection.connectTimeout = 5000
    connection.readTimeout = 5000

    return connection.inputStream.bufferedReader().use { it.readText() }
  }

  override fun listIntelliJVersionsForType(code: String, type: String, attribute: String): List<String> {
    val url = "https://data.services.jetbrains.com/products?code=$code&type=$type"
    val jsonString = fetchJson(url)

    val json = Json { ignoreUnknownKeys = true }
    val jsonElement = json.parseToJsonElement(jsonString)

    val releaseElements = jsonElement.jsonArray[0].jsonObject["releases"]?.jsonArray?.toList() ?: listOf()
    val result = releaseElements.mapNotNull { it.jsonObject[attribute]?.jsonPrimitive?.content }
    println("listIntelliJVersionsForType(code=$code, type=$type, attribute=$attribute) = $result\n")
    return result
  }
}

class MockIntelliJVersionsProvider(
  private val releaseVersions: List<String>,
  private val eapBuilds: List<String>,
) : IntelliJVersionsProvider {
  override fun listIntelliJVersionsForType(code: String, type: String, attribute: String): List<String> = when {
    code == "IC" && type == "release" && attribute == "version" -> releaseVersions
    code == "IU" && type == "eap" && attribute == "build" -> eapBuilds
    else -> emptyList()
  }
}
