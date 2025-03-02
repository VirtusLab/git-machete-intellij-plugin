package com.virtuslab.gitmachete.buildsrc

import org.ajoberstar.grgit.gradle.GrgitPlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.provideDelegate

fun Project.configureVersionFromGit() {
  apply<GrgitPlugin>()

  apply(from = "version.gradle.kts")
  val prospectiveReleaseVersion: String by extra

  val ciBranch: String? by rootProject.extra

  if (ciBranch == "master") {
    // More precisely, "soon-to-happen-in-this-pipeline release version" in case of master builds
    version = prospectiveReleaseVersion
  } else if (!file(".git").exists()) {
    // To make sure it's safe for runs where .git folder is unavailable
    version = "$prospectiveReleaseVersion-SNAPSHOT"
  } else {
    val maybeSnapshot = if (ciBranch == "develop") "" else "-SNAPSHOT"

    val git = org.ajoberstar.grgit.Grgit.open(mapOf("currentDir" to projectDir))
    val tags = git.tag.list().sortedBy { it.dateTime }
    val commitsSinceLastTag = if (tags.isEmpty()) {
      git.log(mapOf("includes" to listOf("HEAD")))
    } else {
      git.log(mapOf("includes" to listOf("HEAD"), "excludes" to listOf(tags.last().fullName)))
    }
    val maybeCommitCount = if (commitsSinceLastTag.isEmpty()) "" else "-${commitsSinceLastTag.size}"
    val shortCommitHash = git.head().abbreviatedId
    val maybeDirty = if (git.status().isClean) "" else "-dirty"
    git.close()

    version = "$prospectiveReleaseVersion$maybeCommitCount$maybeSnapshot+git.$shortCommitHash$maybeDirty"
  }
}
