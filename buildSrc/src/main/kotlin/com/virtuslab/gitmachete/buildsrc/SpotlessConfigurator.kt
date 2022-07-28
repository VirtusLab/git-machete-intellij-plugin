package com.virtuslab.gitmachete.buildsrc

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

object SpotlessConfigurator {
  fun configure(project: Project) {
    project.apply<SpotlessPlugin>()
    project.configure<SpotlessExtension> {
      java {
        importOrder("java", "javax", "", "com.virtuslab")
        // See https://github.com/diffplug/spotless/blob/master/ECLIPSE_SCREENSHOTS.md on importing
        // and exporting settings from Eclipse
        eclipse().configFile("${project.rootDir}/config/spotless/formatting-rules.xml")
        removeUnusedImports()
        targetExclude("**/build/generated/**/*.*")
      }

      kotlin {
        target("**/*.kt", "**/*.kts")
        targetExclude("**/build/*")
        // TODO (#1004): Try to customize ktfmt or use a different formatter
        // ktfmt()
      }
      scala { scalafmt().configFile("${project.rootDir}/scalafmt.conf") }
    }
    val isCI: Boolean by project.rootProject.extra

    if (!isCI) {
      project.tasks.withType<JavaCompile> { dependsOn("spotlessJavaApply") }
      project.tasks.withType<KotlinCompile> { dependsOn("spotlessKotlinApply") }
      project.tasks.withType<ScalaCompile> { dependsOn("spotlessScalaApply") }
    }
  }
}
