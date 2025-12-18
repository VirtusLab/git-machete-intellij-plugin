package com.virtuslab.gitmachete.buildsrc

import com.virtuslab.gitmachete.buildsrc.AnyVersion.Companion.productCode
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy

fun Project.configureUiTests() {
  val intellijVersions: IntellijVersions by rootProject.extra
  val isCI: Boolean by rootProject.extra

  val uiTest = extensions.getByType<JavaPluginExtension>().sourceSets.getByName("uiTest")

  tasks.named<KotlinCompile>("compileUiTestKotlin") {
    // See https://kotlinlang.org/docs/gradle-compilation-and-caches.html#defining-kotlin-compiler-execution-strategy
    // This is needed to avoid a fallback to In process since compilation in Kotlin daemon fails on
    // `bindSingleton<CIServer>(overrides = true)` in BaseUITestSuite, and it's unclear how to fix/replace it.
    compilerExecutionStrategy = KotlinCompilerExecutionStrategy.IN_PROCESS
  }

  val uiTestAgainst = project.properties["against"] as? String
  val uiTestTargetVersions: List<String> =
    if (uiTestAgainst != null) {
      intellijVersions.resolveIntelliJVersions(uiTestAgainst)
    } else {
      listOf(intellijVersions.buildTarget)
    }

  val allUiTests = uiTestTargetVersions.map { version ->
    tasks.register<Test>("uiTest_$version") {
      description = "Runs UI tests."
      group = "verification"

      systemProperty("intellij.version", version)
      // TODO (#2146): drop support for IntelliJ Community
      systemProperty("intellij.product", version.productCode())

      // FIXME (#2202): use IDE instance per method for 2025.3.x,
      // IDE instance per class for the other versions
      val ideInstancePer = if (version.startsWith("2025.3")) "method" else "class"
      systemProperty("ide.instance-per", ideInstancePer)

      testClassesDirs = uiTest.output.classesDirs
      classpath = configurations.getByName("uiTestRuntimeClasspath") + uiTest.output

      val buildPlugin = rootProject.tasks.findByPath(":buildPlugin")!!
      dependsOn(buildPlugin)
      systemProperty("path.to.build.plugin", buildPlugin.outputs.files.singleFile.path)

      val robotServerPluginZip = configurations.getByName("robotServerPluginZip")
      dependsOn(robotServerPluginZip)
      systemProperty("path.to.robot.server.plugin", robotServerPluginZip.singleFile.path)

      if (!isCI) {
        outputs.upToDateWhen { false }
      }

      val testFilter = project.properties["tests"]
      if (testFilter != null) {
        filter { includeTestsMatching("*.*$testFilter*") }
      }

      useJUnitPlatform()

      // Here, add-opens is needed to avoid
      // IllegalArgumentException: Unable to create converter for class com.intellij.remoterobot.client.ExecuteResponse
      // in com.intellij.remoterobot.RemoteRobot.runJs
      jvmArgs(getFlagsForAddOpens("java.lang", module = "java.base"))
      testLogging.showStandardStreams = true
    }
  }

  tasks.register("uiTest") {
    dependsOn(allUiTests)
  }
}
