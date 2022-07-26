package com.virtuslab.gitmachete.buildsrc

import java.io.File
import java.util.*
import kotlin.collections.HashMap

// TODO (#1004): Remove unsafe casts from class
object IntellijVersionHelper {
    val mapInstance: MutableMap<String, Any> = HashMap()

    fun getInstance(): MutableMap<String, Any> {
        if (mapInstance.isEmpty()) {
            val intellijVersionsProp = getProperties()
            // When this value is updated, remember to update the minimum required IDEA version in README.md.
            mapInstance["earliestSupportedMajor"] = intellijVersionsProp.getProperty("earliestSupportedMajor")
            // Most recent minor versions of all major releases
            // between earliest supported (incl.) and latest stable (excl.), used for binary compatibility checks and UI tests
            mapInstance["latestMinorsOfOldSupportedMajors"] = intellijVersionsProp.getProperty("latestMinorsOfOldSupportedMajors").split(",")
            mapInstance["latestStable"] = intellijVersionsProp.getProperty("latestStable")
            // Note that we have to use a "fixed snapshot" version X.Y.Z-EAP-SNAPSHOT (e.g. 211.4961.33-EAP-SNAPSHOT)
            // rather than a "rolling snapshot" X-EAP-SNAPSHOT (e.g. 211-EAP-SNAPSHOT)
            // to ensure that the builds are reproducible.
            // EAP-CANDIDATE-SNAPSHOTs can be used for binary compatibility checks,
            // but for some reason aren't resolved in UI tests.
            // Generally, see https://www.jetbrains.com/intellij-repository/snapshots/ -> Ctrl+F .idea
            // Use `null` if the latest supported major has a stable release (and not just EAPs).
            mapInstance["eapOfLatestSupportedMajor"] = intellijVersionsProp.getProperty("eapOfLatestSupportedMajor")

            mapInstance["latestSupportedMajor"] = if (mapInstance["eapOfLatestSupportedMajor"] != null) getFromBuildNumber(mapInstance["eapOfLatestSupportedMajor"]!! as String)
                else getMajorPart(mapInstance["latestStable"]!! as String)

            mapInstance["buildTarget"] = mapInstance["eapOfLatestSupportedMajor"] ?: mapInstance["latestStable"]!!
        }

        return mapInstance
    }

    /**
     * @param versionKey Either release number (like 2020.3) or key of intellijVersions (like eapOfLatestSupportedMajor)
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

        val versionValue = mapInstance[versionKey] ?: return emptyList()
        if (versionValue is List<*>) {
            return versionValue as List<String>
        }
        return listOf(versionValue) as List<String>
    }

    fun getFromBuildNumber(buildNumber: String): String {
        return "20${buildNumber.substring(0, 2)}.${buildNumber.substring(2, 3)}"
    }

    fun toBuildNumber(version: String): String {
        return version.substring(2, 6).filter{ it == '.'}
    }

    fun getMajorPart(version: String): String {
        return version.substring(0, 6)
    }

    fun getProperties(): Properties {
        val properties = Properties()
        properties.load(getFile().inputStream())
        return properties
    }

    fun storeProperties(properties: Properties, comment: String?  = null) {
        properties.store(getFile().writer(), comment)
    }

    private fun getFile(): File {
        return File("intellijVersions.properties")
    }
}