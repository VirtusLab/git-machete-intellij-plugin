package com.virtuslab.gitmachete.buildsrc

import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
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

  configure<IntelliJPluginExtension> {
    instrumentCode.set(false)
    pluginName.set("git-machete-intellij-plugin")
    version.set(IntellijVersions.buildTarget)
    plugins.set(listOf("git4idea")) // Needed solely for ArchUnit
  }

  if (!isCI) {
    // The output of this task is for some reason very poorly cached,
    // and the task takes a significant amount of time,
    // while the index of searchable options is of little use for local development.
    tasks.withType<BuildSearchableOptionsTask> { enabled = false }
  }

  configure<ChangelogPluginExtension> {
    val PROSPECTIVE_RELEASE_VERSION: String by extra
    version.set("v$PROSPECTIVE_RELEASE_VERSION")
    header.set(version)
    headerParserRegex.set(Regex("v\\d+\\.\\d+\\.\\d+"))
    path.set("${project.projectDir}/CHANGE-NOTES.md")
    unreleasedTerm.set("Unreleased")
    groups.set(emptyList())
  }

  val changelog = extensions.getByType(ChangelogPluginExtension::class.java)

  tasks.withType<PatchPluginXmlTask> {
    // `sinceBuild` is exclusive when we are using `*` in version but inclusive when without `*`
    sinceBuild.set(
      IntellijVersionHelper.toBuildNumber(IntellijVersions.earliestSupportedMajor)
    )

    // In `untilBuild` situation is inverted: it's inclusive when using `*` but exclusive when without `*`
    untilBuild.set(
      IntellijVersionHelper.toBuildNumber(IntellijVersions.latestSupportedMajor) + ".*"
    )

    // Note that the first line of the description should be self-contained since it is placed into embeddable card:
    // see e.g. https://plugins.jetbrains.com/search?search=git%20machete
    pluginDescription.set(file("$rootDir/DESCRIPTION.html").readText())

    changeNotes.set(
      "<h3>v${rootProject.version}</h3>\n\n${
      (changelog.getOrNull(changelog.version.get()) ?: changelog.getUnreleased()).toHTML()
      }"
    )
  }

  tasks.withType<RunIdeTask> { maxHeapSize = "4G" }

  tasks.withType<RunPluginVerifierTask> {
    val maybeEap = listOfNotNull(
      IntellijVersions.eapOfLatestSupportedMajor?.replace("-EAP-(CANDIDATE-)?SNAPSHOT".toRegex(), "")
    )

    ideVersions.set(
      IntellijVersions.latestMinorsOfOldSupportedMajors +
        IntellijVersions.latestStable +
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

  tasks.withType<PublishPluginTask> { token.set(jetbrainsMarketplaceToken) }
}
