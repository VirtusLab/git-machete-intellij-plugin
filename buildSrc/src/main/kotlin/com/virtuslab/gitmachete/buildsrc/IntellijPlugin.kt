package com.virtuslab.gitmachete.buildsrc

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.*
import org.jetbrains.changelog.ChangelogPlugin
import org.jetbrains.changelog.ChangelogPluginExtension
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

fun Project.configureIntellijPlugin() {
  apply<ChangelogPlugin>()

  val intellijVersions: IntellijVersions by rootProject.extra

  // This task should not be used - we don't use the "Unreleased" section anymore
  project.gradle.startParameter.excludedTaskNames.add("patchChangeLog")

  configure<ChangelogPluginExtension> {
    val prospectiveReleaseVersion: String by extra
    version.set("v$prospectiveReleaseVersion")
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
            "update the file or change the prospective version in version.gradle.kts",
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
              "${System.lineSeparator()}$line",
          )
        }
      }
    }
  }

  tasks.register("verifyChangeLog") {
    dependsOn(verifyVersionTask, verifyContentsTask)
  }

  tasks.register("printPluginZipPath") {
    doLast {
      val buildPlugin = tasks.findByPath(":buildPlugin")!!
      println(buildPlugin.outputs.files.first().path)
    }
  }

  tasks.register("printSignedPluginZipPath") {
    // Required to prevent https://github.com/VirtusLab/git-machete-intellij-plugin/issues/1358
    dependsOn(":buildPlugin")

    doLast {
      val signPlugin = tasks.findByPath(":signPlugin")!!
      println(signPlugin.outputs.files.first().path)
    }
  }

  val verifyPluginZipTask = tasks.register("verifyPluginZip") {
    val buildPlugin = tasks.findByPath(":buildPlugin")!!
    dependsOn(buildPlugin)

    doLast {
      val pluginZipPath = buildPlugin.outputs.files.first().path
      val jarsInPluginZip = ZipFile(pluginZipPath).use { zf ->
        zf.stream()
          .map(ZipEntry::getName)
          .map { it.removePrefix("git-machete-intellij-plugin/").removePrefix("lib/").removeSuffix(".jar") }
          .filter { it.isNotEmpty() }
          .toList()
      }

      for (proj in subprojects) {
        val projJar = proj.path.replaceFirst(":", "").replace(":", "-")
        val javaExtension = proj.extensions.getByType<JavaPluginExtension>()
        if (javaExtension.sourceSets["main"].allSource.srcDirs.any { it?.exists() ?: false }) {
          check(projJar in jarsInPluginZip) {
            "$projJar.jar was expected in plugin zip ($pluginZipPath) but was NOT found"
          }
        } else {
          check(projJar !in jarsInPluginZip) {
            "$projJar.jar was NOT expected in plugin zip ($pluginZipPath) but was found"
          }
        }
      }

      val expectedLibs = listOf("org.eclipse.jgit", "slf4j-lambda-core", "vavr", "vavr-match")
      for (expectedLib in expectedLibs) {
        val libRegexStr = "^" + expectedLib.replace(".", "\\.") + "-[0-9.]+.*$"
        check(jarsInPluginZip.any { it.matches(libRegexStr.toRegex()) }) {
          "A jar for $expectedLib was expected in plugin zip ($pluginZipPath) but was NOT found"
        }
      }

      val forbiddenLibPrefixes = listOf("ide-probe", "idea", "kotlin", "lombok", "remote-robot", "scala", "slf4j")
      for (jar in jarsInPluginZip) {
        check(forbiddenLibPrefixes.none { jar.startsWith(it) } || expectedLibs.any { jar.startsWith(it) }) {
          "$jar.jar was NOT expected in plugin zip ($pluginZipPath) but was found"
        }
      }
    }
  }

  tasks.named<Zip>("buildPlugin") {
    dependsOn(verifyVersionTask)
    finalizedBy(verifyPluginZipTask)
  }

//  tasks.withType<PatchPluginXmlTask> {
//
//    val item = changelog.getOrNull(changelog.version.get())
//    if (item != null) {
//      changeNotes.set(changelog.renderItem(item, Changelog.OutputType.HTML))
//    }
//  }
}
