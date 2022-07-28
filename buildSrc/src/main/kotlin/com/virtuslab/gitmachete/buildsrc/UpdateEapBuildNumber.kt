package com.virtuslab.gitmachete.buildsrc

import java.util.regex.Pattern
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.jsoup.Jsoup

open class UpdateEapBuildNumber : DefaultTask() {

  @Internal
  val intellijSnapshotsUrl: String = "https://www.jetbrains.com/intellij-repository/snapshots/"

  @Internal
  @Option(
      option = "exit-code",
      description =
          "If set to true, the task will return the exit code 1 when the new EAP build number is NOT found")
  var exitCode: Boolean = false

  fun checkForEapWithBuildNumberHigherThan(latestEapBuildNumber: String): String? {
    val htmlContent = Jsoup.connect(intellijSnapshotsUrl).get()
    val links =
        htmlContent.select(
            "a[href^=" +
                intellijSnapshotsUrl +
                "com/jetbrains/intellij/idea/ideaIC/][href$=\"-EAP-SNAPSHOT.pom\"]")

    for (link in links) {
      val attr = link.attr("href")
      val pattern = Pattern.compile("(?<=ideaIC-)\\d+\\.\\d+\\.\\d+(?=-EAP-SNAPSHOT.pom)")
      val matcher = pattern.matcher(attr)

      if (matcher.find()) {
        val foundBuildNumber = matcher.group()

        if (foundBuildNumber buildNumberIsNewerThan latestEapBuildNumber) {
          return foundBuildNumber
        }
      }
    }
    return null
  }

  @TaskAction
  fun execute() {
    val properties = IntellijVersionHelper.getProperties()
    val latestEapBuildNumber =
        IntellijVersionHelper.instance["eapOfLatestSupportedMajor"] as String?

    val buildNumberThreshold =
        if (!latestEapBuildNumber.isNullOrEmpty()) latestEapBuildNumber.replace("-EAP-SNAPSHOT", "")
        else
            "${IntellijVersionHelper.toBuildNumber(IntellijVersionHelper.instance["latestStable"] as String)}.999999.999999"

    val newerEapBuildNumber = checkForEapWithBuildNumberHigherThan(buildNumberThreshold)

    if (!newerEapBuildNumber.isNullOrEmpty()) {
      project.logger.lifecycle(
          "eapOfLatestSupportedMajor is updated to $newerEapBuildNumber build number")
      properties.setProperty("eapOfLatestSupportedMajor", "$newerEapBuildNumber-EAP-SNAPSHOT")
      IntellijVersionHelper.storeProperties(properties)
    }

    if (exitCode && newerEapBuildNumber.isNullOrEmpty()) {
      throw Exception("New eap build number not found")
    }
  }

  companion object {
    infix fun String.buildNumberIsNewerThan(otherBuildNumber: String): Boolean {
      val lhsSplit = this.split('.')
      val rhsSplit = otherBuildNumber.split('.')

      val firstDiff = lhsSplit.zip(rhsSplit).find { it.first != it.second }

      if (firstDiff == null) {
        return lhsSplit.size > rhsSplit.size
      }

      return Integer.parseInt(firstDiff.first) > Integer.parseInt(firstDiff.second)
    }
  }
}
