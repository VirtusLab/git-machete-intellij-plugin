package com.virtuslab.gitmachete.buildsrc

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.util.VersionNumber
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

import java.util.regex.Matcher
import java.util.regex.Pattern

open class UpdateEapBuildNumber : DefaultTask() {

    @Internal
    var intellijSnapshotsUrl: String = "https://www.jetbrains.com/intellij-repository/snapshots/"

    @Internal
    @Option(option = "exit-code", description = "Return the exit code 0 when the new eap build number is found and 1 otherwise")
    var exitCode: Boolean = false

    fun checkForEapWithBuildNumberHigherThan(latestEapBuildNumber: String): String? {
        val htmlContent = Jsoup.connect(intellijSnapshotsUrl).get()
        val links = htmlContent.select("a[href^=" + intellijSnapshotsUrl + "com/jetbrains/intellij/idea/ideaIC/][href$=\"-EAP-SNAPSHOT.pom\"]")

        for (link in links) {
            val attr = link.attr("href")
            val pattern = Pattern.compile("(?<=ideaIC-)\\d+\\.\\d+\\.\\d+(?=-EAP-SNAPSHOT.pom)")
            val matcher = pattern.matcher(attr)

            if (matcher.find()) {
                val foundBuildNumber = matcher.group()

                if (VersionNumber.parse(foundBuildNumber) > VersionNumber.parse(latestEapBuildNumber)) {
                    return foundBuildNumber
                }
            }
        }
        return null
    }

    @TaskAction
    fun execute() {
        val properties = IntellijVersionHelper.getProperties()
        val latestEapBuildNumber = IntellijVersionHelper.instance["eapOfLatestSupportedMajor"] as String?

        val buildNumberThreshold = if (!latestEapBuildNumber.isNullOrEmpty())
                latestEapBuildNumber.replace("-EAP-SNAPSHOT", "")
            else
                IntellijVersionHelper.toBuildNumber(IntellijVersionHelper.instance["latestStable"] as String)+".999999.999999"

        val newerEapBuildNumber = checkForEapWithBuildNumberHigherThan(buildNumberThreshold)

        if (!newerEapBuildNumber.isNullOrEmpty()) {
            project.logger.lifecycle("EapOfLatestSupportedMajor is updated to $newerEapBuildNumber build number")
            properties.setProperty("eapOfLatestSupportedMajor", "$newerEapBuildNumber-EAP-SNAPSHOT")
            IntellijVersionHelper.storeProperties(properties)
        }

        if (exitCode && newerEapBuildNumber.isNullOrEmpty()) {
            throw Exception("New eap build number not found")
        }
    }
}
