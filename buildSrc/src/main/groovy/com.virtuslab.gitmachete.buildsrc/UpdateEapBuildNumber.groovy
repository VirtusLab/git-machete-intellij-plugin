package com.virtuslab.gitmachete.buildsrc

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

import java.util.regex.Matcher
import java.util.regex.Pattern

class UpdateEapBuildNumber extends DefaultTask {

    final String intellijSnapshotsUrl = "https://www.jetbrains.com/intellij-repository/snapshots/"

    @Input
    @Optional
    @Option(option = 'exit-code', description = 'Return the exit code 0 when the new eap build number is found and 1 otherwise')
    Boolean exitCode

    private String checkForEapWithBuildNumberHigherThan(String latestEapBuildNumber) {
        Document htmlContent = Jsoup.connect(intellijSnapshotsUrl).get()
        Elements links = htmlContent.select('a[href^=' + intellijSnapshotsUrl + 'com/jetbrains/intellij/idea/ideaIC/][href$="-EAP-SNAPSHOT.pom"]')
        for (Element link : links) {
            String attr = link.attr("href")
            Pattern pattern = Pattern.compile("(?<=ideaIC-)\\d+\\.\\d+\\.\\d+(?=-EAP-SNAPSHOT.pom)")
            Matcher matcher = pattern.matcher(attr)
            if (matcher.find()) {
                String foundBuildNumber = matcher.group()
                if (VersionNumber.parse(foundBuildNumber) > VersionNumber.parse(latestEapBuildNumber)) {
                    return foundBuildNumber
                }
            }
        }
        return null
    }

    @TaskAction
    void execute() {
        Properties properties = IntellijVersionHelper.getProperties()
        File propFile = IntellijVersionHelper.getFile()
        String latestEapBuildNumber = project.intellijVersions.eapOfLatestSupportedMajor
        String buildNumberThreshold = latestEapBuildNumber ? latestEapBuildNumber.replace('-EAP-SNAPSHOT', '') :
                project.intellijVersionToBuildNumber(project.intellijVersions.latestStable)+'.999999.999999'
        String newerEapBuildNumber = checkForEapWithBuildNumberHigherThan(buildNumberThreshold)
        if (newerEapBuildNumber) {
            project.logger.lifecycle("EapOfLatestSupportedMajor is updated to " + newerEapBuildNumber + " build number")
            properties.setProperty("eapOfLatestSupportedMajor", newerEapBuildNumber+"-EAP-SNAPSHOT")
            properties.store(propFile.newWriter(), /*comments*/ null)
        }
        if (exitCode && !newerEapBuildNumber) {
            throw new Exception("New eap build number not found")
        }
    }
}
