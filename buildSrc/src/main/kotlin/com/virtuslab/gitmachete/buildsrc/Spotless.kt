package com.virtuslab.gitmachete.buildsrc

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.scala.ScalaCompile
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

    kotlin {
      ktlint().editorConfigOverride(mapOf("disabled_rules" to "no-wildcard-imports,filename", "indent_size" to 2))
      target("**/*.kt")
    }
    kotlinGradle {
      ktlint().editorConfigOverride(mapOf("disabled_rules" to "no-wildcard-imports", "indent_size" to 2))
      target("**/*.gradle.kts")
    }
    scala { scalafmt().configFile("$rootDir/scalafmt.conf") }
  }

  val isCI: Boolean by rootProject.extra

  if (!isCI) {
    tasks.withType<JavaCompile> { dependsOn("spotlessJavaApply") }
    tasks.withType<KotlinCompile> { dependsOn("spotlessKotlinApply") }
    tasks.withType<ScalaCompile> { dependsOn("spotlessScalaApply") }
  }
}
