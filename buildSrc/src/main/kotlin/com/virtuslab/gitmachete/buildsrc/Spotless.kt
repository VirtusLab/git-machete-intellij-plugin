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
  configure<SpotlessExtension> {
    java {
      importOrder("java", "javax", "", "com.virtuslab")
      // See https://github.com/diffplug/spotless/blob/master/ECLIPSE_SCREENSHOTS.md on importing
      // and exporting settings from Eclipse
      eclipse().configFile("$rootDir/config/spotless/formatting-rules.xml")
      removeUnusedImports()
      targetExclude("**/build/generated/**/*.*")
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
      target("**/*.kt")
    }

    kotlinGradle {
      ktlint().editorConfigOverride(ktlintEditorConfig)
      target("**/*.gradle.kts")
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
