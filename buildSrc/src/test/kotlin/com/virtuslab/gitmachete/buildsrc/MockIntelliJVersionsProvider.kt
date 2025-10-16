package com.virtuslab.gitmachete.buildsrc

class MockIntelliJVersionsProvider(
  private val releaseVersions: List<String>,
  private val eapBuilds: List<String>,
) : IntelliJVersionsProvider {
  override fun listIntelliJVersionsForType(code: String, type: String, attribute: String): List<String> = when {
    code == "IC" && type == "release" && attribute == "version" -> releaseVersions
    code == "IU" && type == "eap" && attribute == "build" -> eapBuilds
    else -> emptyList()
  }

  override fun getKotlinVersionForIntelliJ(intellijVersion: String): String = "1.9.22"
}
