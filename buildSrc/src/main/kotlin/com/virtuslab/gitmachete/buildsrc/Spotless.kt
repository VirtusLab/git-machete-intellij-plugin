package com.virtuslab.gitmachete.buildsrc

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.configureSpotless() {
  apply<SpotlessPlugin>()
  // Every target below is anchored at the project directory (`src/...`, `*.gradle.kts`) rather than starting with `**/`.
  // Spotless compiles each pattern into a `fileTree(projectDir)` carrying that single include,
  // and a leading `**/` matches at any depth, which forbids Gradle from pruning a single directory
  // while snapshotting the task's inputs - it then walks and pattern-matches the entire project tree,
  // `.git` and the multi-gigabyte `.intellijPlatform` IDE sandbox included, on every build.
  // Anchoring also keeps out of scope, by construction rather than by exclusion:
  // generated sources under `build/` (GrammarKit parser/lexer, the baked-in version constant),
  // the sandbox that `prepareTestSandbox` populates (see JetBrains/intellij-platform-gradle-plugin#2096),
  // and IDE-managed output dirs such as `bin/`, whose stale copies of our sources would otherwise
  // get reformatted and invalidate these tasks whenever the IDE refreshes them.
  // Finally, it stops every parent project from redundantly formatting its children's sources.
  configure<SpotlessExtension> {
    java {
      importOrder("java", "javax", "", "com.virtuslab")
      // See https://github.com/diffplug/spotless/blob/master/ECLIPSE_SCREENSHOTS.md on importing
      // and exporting settings from Eclipse
      eclipse().configFile("$rootDir/config/spotless/formatting-rules.xml")
      removeUnusedImports()
      target("src/**/*.java")
    }

    val ktlintEditorConfig = mapOf(
      "indent_size" to 2,
      "ktlint_standard_comment-wrapping" to "disabled",
      "ktlint_standard_filename" to "disabled",
      "ktlint_standard_function-naming" to "disabled",
      "ktlint_standard_no-empty-file" to "disabled",
      "ktlint_standard_no-wildcard-imports" to "disabled",
      "ktlint_standard_value-argument-comment" to "disabled",
    )

    kotlin {
      ktlint().editorConfigOverride(ktlintEditorConfig)
      target("src/**/*.kt")
    }

    kotlinGradle {
      ktlint().editorConfigOverride(ktlintEditorConfig)
      // Each project covers its own build script, the root project additionally `settings.gradle.kts`
      // and `version.gradle.kts`; buildSrc has its own Spotless setup.
      target("*.gradle.kts")
    }
  }

  val isCI: Boolean by rootProject.extra

  if (!isCI) {
    tasks {
      withType<AbstractCompile> { dependsOn("spotlessKotlinGradleApply") }
      withType<JavaCompile> { dependsOn("spotlessJavaApply") }
      withType<KotlinCompile> { dependsOn("spotlessKotlinApply") }
    }
  }
}
