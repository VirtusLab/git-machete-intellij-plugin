package com.virtuslab.gitmachete.buildsrc

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy

fun Project.configureUiTests() {
  val intellijVersions: IntellijVersions by rootProject.extra
  val isCI: Boolean by rootProject.extra

  val uiTest = extensions.getByType<JavaPluginExtension>().sourceSets.getByName("uiTest")

  tasks.named<KotlinCompile>("compileUiTestKotlin") {
    // See https://kotlinlang.org/docs/gradle-compilation-and-caches.html#defining-kotlin-compiler-execution-strategy
    // This is needed to avoid a fallback to In process since compilation in Kotlin daemon fails on
    // `bindSingleton<CIServer>(overrides = true)` in BaseUITest, and it's unclear how to fix/replace it.
    compilerExecutionStrategy = KotlinCompilerExecutionStrategy.IN_PROCESS
  }

  val uiTestAgainst = project.properties["against"] as? String
  val uiTestTargetVersions: List<String> =
    if (uiTestAgainst != null) {
      intellijVersions.resolveIntelliJVersions(uiTestAgainst)
    } else {
      listOf(intellijVersions.buildTarget)
    }

  val jacocoAgentRuntime = configurations.getByName("jacocoAgentRuntime")

  val allUiTests = uiTestTargetVersions.map { version ->
    tasks.register<Test>("uiTest_$version") {
      description = "Runs UI tests."
      group = "verification"

      systemProperty("intellij.version", version)
      systemProperty("ide.instance-per", "class") // a slower alternative: "method"

      testClassesDirs = uiTest.output.classesDirs
      classpath = configurations.getByName("uiTestRuntimeClasspath") + uiTest.output

      val buildPlugin = rootProject.tasks.findByPath(":buildPlugin")!!
      dependsOn(buildPlugin)
      systemProperty("path.to.build.plugin", buildPlugin.outputs.files.singleFile.path)

      val robotServerPluginZip = configurations.getByName("robotServerPluginZip")
      dependsOn(robotServerPluginZip)
      systemProperty("path.to.robot.server.plugin", robotServerPluginZip.singleFile.path)

      // Hand the standalone JaCoCo agent jar and a per-IDE-version `.exec` destination to
      // `BaseUITest`, which then attaches the agent to the IDE-under-test JVM via
      // `applyVMOptionsPatch`. The Gradle test JVM that runs RemoteRobot client code is just a
      // thin shell that doesn't load production plugin classes, so the only coverage worth
      // collecting lives in the IDE process.
      dependsOn(jacocoAgentRuntime)
      val jacocoAgentJar = jacocoAgentRuntime.singleFile
      val jacocoExecFile = rootProject.layout.buildDirectory.file("jacoco/uiTest_$version.exec").get().asFile
      systemProperty("jacoco.agent.jar", jacocoAgentJar.absolutePath)
      systemProperty("jacoco.exec.file", jacocoExecFile.absolutePath)
      doFirst { jacocoExecFile.parentFile.mkdirs() }

      // The Gradle JaCoCo plugin auto-attaches its agent to every `Test` task; for `uiTest_*`
      // that produces a near-empty exec (no production classes load in this JVM) which then
      // pollutes the aggregated report. The IDE-side exec we wire above is the canonical one.
      the<JacocoTaskExtension>().isEnabled = false

      if (!isCI) {
        outputs.upToDateWhen { false }
      }

      val testFilter = project.properties["tests"]
      if (testFilter != null) {
        filter { includeTestsMatching("*.*$testFilter*") }
      }

      useJUnitPlatform()

      // IllegalArgumentException: Unable to create converter for class com.intellij.remoterobot.client.ExecuteResponse
      // in com.intellij.remoterobot.RemoteRobot.runJs
      jvmArgs(getFlagsForAddOpens("java.lang", module = "java.base"))
      // Since 2026.1 EAP:
      // InaccessibleObjectException: Unable to make public static
      // javax.swing.TimerQueue javax.swing.TimerQueue.sharedInstance() accessible:
      // module java.desktop does not "opens javax.swing" to unnamed module
      jvmArgs(getFlagsForAddOpens("javax.swing", module = "java.desktop"))
      testLogging.showStandardStreams = true
    }
  }

  tasks.register("uiTest") {
    dependsOn(allUiTests)
  }
}

fun Project.configureUiTestCoverage() {
  // Standalone JaCoCo agent jar that `configureUiTests()` hands to the IDE-under-test JVM via
  // system properties. The `runtime`-classified artifact is the bare jar suitable for
  // `-javaagent:`; the unclassified jar wraps the agent as a nested resource, which is how
  // Gradle's built-in `jacocoAgent` configuration ships it but requires runtime extraction.
  val jacocoToolVersion = the<JacocoPluginExtension>().toolVersion
  val jacocoAgentRuntime = configurations.create("jacocoAgentRuntime") {
    isCanBeConsumed = false
    isCanBeResolved = true
  }
  dependencies.add(jacocoAgentRuntime.name, "org.jacoco:org.jacoco.agent:$jacocoToolVersion:runtime")

  // UI-test JaCoCo report. The per-subproject `jacocoTestReport` tasks cover the unit-test layer
  // (Codecov merges their XMLs server-side from the CI working-tree walk); this task handles the
  // orthogonal UI-test layer, whose `.exec` files are written by the JaCoCo agent attached to the
  // IDE-under-test JVM at `<root>/build/jacoco/uiTest_<intellijVersion>.exec`. In CI only the
  // `ui-tests-recent` job runs this (against a single IDE version per build), but the `fileTree`
  // glob lets a local `./gradlew uiTest_a uiTest_b jacocoUiTestReport` invocation still fuse
  // multiple execs into one report.
  val jacocoUiTestReport = tasks.register<JacocoReport>("jacocoUiTestReport") {
    group = "verification"
    description = "Generates a JaCoCo report from coverage harvested by UI tests."

    // `fileTree` resolves lazily at task-execution time and silently drops files that don't
    // exist yet, so this stays usable regardless of which `uiTest_*` task(s) ran.
    executionData.from(fileTree(layout.buildDirectory.dir("jacoco")) { include("uiTest_*.exec") })

    // Pin coverage to the production class output dirs of each subproject (not the jars
    // repackaged into the plugin zip): JaCoCo matches by fully-qualified class name regardless
    // of where the bytecode physically sits at runtime, and pointing at `classes/{java,kotlin}
    // /main` is what lets the HTML report link back to source.
    val mainSourceSets = subprojects.mapNotNull { sp ->
      sp.extensions.findByType<JavaPluginExtension>()
        ?.sourceSets
        ?.findByName("main")
        ?.takeIf { ss -> ss.allSource.srcDirs.any { it.exists() } }
    }
    classDirectories.setFrom(mainSourceSets.map { it.output.classesDirs })
    sourceDirectories.setFrom(mainSourceSets.flatMap { it.allSource.srcDirs })

    // `html.required = false` / `xml.required = true` are set centrally by the
    // `tasks.withType<JacocoReport>` block in the root build script, since Codecov ingests JaCoCo
    // XML and renders its own HTML. CSV stays off (JaCoCo plugin default).

    doFirst {
      check(executionData.files.isNotEmpty()) {
        "No JaCoCo UI-test execution data found. Run `./gradlew uiTest` first."
      }
    }
  }

  // Don't actively run UI tests; assemble whatever execs already exist. This keeps the report
  // cheap to re-run after iterating on a single IDE version.
  jacocoUiTestReport.configure { mustRunAfter(tasks.matching { it.name.startsWith("uiTest") }) }
}
