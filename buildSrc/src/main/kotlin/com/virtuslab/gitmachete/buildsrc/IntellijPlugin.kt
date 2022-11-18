package com.virtuslab.gitmachete.buildsrc

import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.*
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.ChangelogPlugin
import org.jetbrains.changelog.ChangelogPluginExtension
import org.jetbrains.intellij.IntelliJPlugin
import org.jetbrains.intellij.IntelliJPluginExtension
import org.jetbrains.intellij.tasks.*
import java.util.*

fun Project.configureIntellijPlugin() {
  apply<IntelliJPlugin>()
  apply<ChangelogPlugin>()

  val isCI: Boolean by rootProject.extra
  val jetbrainsMarketplaceToken: String? by rootProject.extra
  val pluginSignPrivateKey: String? by rootProject.extra
  val pluginSignCertificateChain: String? by rootProject.extra
  val pluginSignPrivateKeyPass: String? by rootProject.extra

  val intellijVersions: IntellijVersions by rootProject.extra

  configure<IntelliJPluginExtension> {
    instrumentCode.set(false)
    pluginName.set("git-machete-intellij-plugin")
    version.set(intellijVersions.buildTarget)
    plugins.set(listOf("Git4Idea")) // Needed solely for ArchUnit
  }

  if (!isCI) {
    // The output of this task is for some reason very poorly cached,
    // and the task takes a significant amount of time,
    // while the index of searchable options is of little use for local development.
    tasks.withType<BuildSearchableOptionsTask> { enabled = false }
  }

  // This task should not be used - we don't use the "Unreleased" section anymore
  project.gradle.startParameter.excludedTaskNames.add("patchChangeLog")

  configure<ChangelogPluginExtension> {
    val PROSPECTIVE_RELEASE_VERSION: String by extra
    version.set("v$PROSPECTIVE_RELEASE_VERSION")
    headerParserRegex.set(Regex("""v\d+\.\d+\.\d+"""))
    path.set("${project.projectDir}/CHANGE-NOTES.md")
  }

  val changelog = extensions.getByType(ChangelogPluginExtension::class.java)

  val verifyVersionTask = tasks.register("verifyChangeLogVersion") {
    doLast {
      val prospectiveVersionSection = changelog.version.get()
      val latestVersionSection = changelog.getLatest()

      if (prospectiveVersionSection != latestVersionSection.version) {
        throw Exception(
          "$prospectiveVersionSection is not the latest in CHANGE-NOTES.md, " +
            "update the file or change the prospective version in version.gradle.kts"
        )
      }
    }
  }

  val verifyContentsTask = tasks.register("verifyChangeLogContents") {
    doLast {
      val prospectiveVersionSection = changelog.get(changelog.version.get())

      val renderItemStr = changelog.renderItem(prospectiveVersionSection)
      if (renderItemStr.isBlank()) {
        throw Exception("${prospectiveVersionSection.version} section is empty, update CHANGE-NOTES.md")
      }

      val listingElements = renderItemStr.split(System.lineSeparator()).drop(1)
      for (line in listingElements) {
        if (line.isNotBlank() && !line.startsWith("- ") && !line.startsWith("  ")) {
          throw Exception(
            "Update formatting in CHANGE-NOTES.md ${prospectiveVersionSection.version} section:" +
              "${System.lineSeparator()}$line"
          )
        }
      }
    }
  }

  tasks.register("verifyChangeLog") {
    dependsOn(verifyVersionTask, verifyContentsTask)
  }

  tasks.named<Zip>("buildPlugin") {
    dependsOn(verifyVersionTask)
  }

  tasks.withType<PatchPluginXmlTask> {
    // `sinceBuild` is exclusive when we are using `*` in version but inclusive when without `*`
    sinceBuild.set(
      IntellijVersionHelper.versionToBuildNumber(intellijVersions.earliestSupportedMajor)
    )

    // In `untilBuild` situation is inverted: it's inclusive when using `*` but exclusive when without `*`
    untilBuild.set(
      IntellijVersionHelper.versionToBuildNumber(intellijVersions.latestSupportedMajor) + ".*"
    )

    // Note that the first line of the description should be self-contained since it is placed into embeddable card:
    // see e.g. https://plugins.jetbrains.com/search?search=git%20machete
    pluginDescription.set(file("$rootDir/DESCRIPTION.html").readText())

    val item = changelog.getOrNull(changelog.version.get())
    if (item != null) {
      changeNotes.set("<h3>v${rootProject.version}</h3>\n\n" + changelog.renderItem(item, Changelog.OutputType.HTML))
    }
  }

  tasks.withType<RunIdeTask> { maxHeapSize = "4G" }

  tasks.withType<RunPluginVerifierTask> {
    val maybeEap = listOfNotNull(
      intellijVersions.eapOfLatestSupportedMajor?.replace("-EAP-(CANDIDATE-)?SNAPSHOT".toRegex(), "")
    )

    ideVersions.set(
      intellijVersions.latestMinorsOfOldSupportedMajors +
        intellijVersions.latestStable +
        maybeEap
    )

    val skippedFailureLevels =
      EnumSet.of(
        RunPluginVerifierTask.FailureLevel.DEPRECATED_API_USAGES,
        RunPluginVerifierTask.FailureLevel.EXPERIMENTAL_API_USAGES,
        RunPluginVerifierTask.FailureLevel.NOT_DYNAMIC,
        RunPluginVerifierTask.FailureLevel.SCHEDULED_FOR_REMOVAL_API_USAGES
      )
    failureLevel.set(EnumSet.complementOf(skippedFailureLevels))
  }

  tasks.withType<SignPluginTask> {
    certificateChain.set(pluginSignCertificateChain?.trimIndent())

    privateKey.set(pluginSignPrivateKey?.trimIndent())

    password.set(pluginSignPrivateKeyPass)
  }

  tasks.withType<PublishPluginTask> {
    token.set(jetbrainsMarketplaceToken)
  }
}
