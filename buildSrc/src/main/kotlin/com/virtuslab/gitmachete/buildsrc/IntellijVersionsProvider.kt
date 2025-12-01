package com.virtuslab.gitmachete.buildsrc

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URI

data class KotlinLibraryVersions(
  val kotlinVersion: String,
)

interface IntelliJVersionsProvider {
  fun listIntelliJVersionsForType(code: String, type: String, attribute: String): List<String>
  fun getKotlinLibraryVersionsForIntelliJ(intellijVersion: String): KotlinLibraryVersions
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

  private fun extractLibraryVersionByUrl(jsonElement: kotlinx.serialization.json.JsonElement, url: String, intellijVersion: String): String {
    val matchingLibraries = jsonElement.jsonArray.filter { library ->
      library.jsonObject["url"]?.jsonPrimitive?.content == url
    }

    if (matchingLibraries.isEmpty()) {
      throw IllegalStateException("Library with URL $url not found for IntelliJ version $intellijVersion")
    }

    val versions = matchingLibraries.mapNotNull { library ->
      library.jsonObject["version"]?.jsonPrimitive?.content
    }

    if (versions.isEmpty()) {
      throw IllegalStateException("No version found for library with URL $url in IntelliJ version $intellijVersion")
    }

    // Find the highest version using compareTo
    return versions.map { ReleaseVersion(it) }
      .maxWithOrNull { a, b -> a compareTo b }?.value ?: versions.first()
  }

  override fun getKotlinLibraryVersionsForIntelliJ(intellijVersion: String): KotlinLibraryVersions {
    // See https://www.jetbrains.com/legal/third-party-software/?product=IIC&version=2024.2 for web version
    val url = "https://resources.jetbrains.com/storage/third-party-libraries/idea/ideaIC-$intellijVersion-third-party-libraries.json"
    val jsonString = fetchJson(url)

    val json = Json { ignoreUnknownKeys = true }
    val jsonElement = json.parseToJsonElement(jsonString)

    val kotlinVersion = extractLibraryVersionByUrl(
      jsonElement,
      "https://github.com/JetBrains/kotlin",
      intellijVersion,
    )

    println("getKotlinLibraryVersionsForIntelliJ($intellijVersion) = KotlinLibraryVersions(kotlinVersion=$kotlinVersion)\n")
    return KotlinLibraryVersions(kotlinVersion)
  }
}
