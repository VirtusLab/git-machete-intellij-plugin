package com.virtuslab.gitmachete.uitest

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.isProjectOpened
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.xQuery
import com.intellij.ide.starter.ci.CIServer
import com.intellij.ide.starter.ci.NoCIServer
import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.models.IdeInfo
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.ide.starter.runner.Starter
import com.intellij.platform.testFramework.teamCity.TeamCityReporter.SyntheticTestKind
import com.intellij.remoterobot.RemoteRobot
import com.virtuslab.gitmachete.testcommon.SetupScripts
import com.virtuslab.gitmachete.testcommon.TestGitRepository
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.fail
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission.*
import kotlin.time.Duration.Companion.minutes

abstract class BaseUITestSuite : TestGitRepository(SetupScripts.SETUP_WITH_SINGLE_REMOTE) {
  companion object {
    val robot = RemoteRobot("http://127.0.0.1:8580")
    private val intelliJVersion = System.getProperty("intellij.version")

    fun <T> retryOnConnectException(attempts: Int, block: () -> T): T = try {
      block()
    } catch (e: java.net.ConnectException) {
      if (attempts > 1) {
        println("Retrying due to ${e.message}...")
        Thread.sleep(3000)
        retryOnConnectException(attempts - 1, block)
      } else {
        throw RuntimeException("Retries failed", e)
      }
    }

    fun Driver.waitForProject() {
      println("Waiting for project to open...")
      var attemptsLeft = 3
      while (!isProjectOpened() && attemptsLeft > 0) {
        Thread.sleep(3000)
        attemptsLeft--
      }
      if (!isProjectOpened()) {
        throw IllegalStateException("Project has still not been opened, aborting")
      }

      println("Waiting for indicators...")
      myWaitForIndicators(1.minutes)
      println("Project opened")
    }

    private fun testCase(projectInfo: ProjectInfoSpec): TestCase<ProjectInfoSpec> {
      // ide-starter ships only the core squashed jar with us; the per-product DI bindings
      // (registered via ServiceLoader<IdeProductInit> in product-specific modules like
      // `intellij.tools.ide.starter.product.idea_ultimate`) are not on our classpath, so
      // looking up IdeInfo in DI by tag would fail. Construct it inline instead.
      val ideInfo = IdeInfo(
        productCode = "IU",
        platformPrefix = "idea",
        executableFileName = "idea",
        fullName = "IDEA",
        qodanaProductCode = "QDJVM",
      )
      val testCase = TestCase(ideInfo, projectInfo)
      return if (intelliJVersion.matches("20[0-9][0-9]\\.[0-9].*".toRegex())) {
        testCase.withVersion(intelliJVersion)
      } else {
        testCase.withBuildNumber(intelliJVersion)
      }
    }

    fun startIde(projectInfo: ProjectInfoSpec): BackgroundRun {
      di = DI {
        extend(di)
        bindSingleton<CIServer>(overrides = true) {
          object : CIServer by NoCIServer {
            override fun reportTestFailure(
              testName: String,
              message: String,
              details: String,
              linkToLogs: String?,
              kind: SyntheticTestKind,
              generifyTestName: Boolean,
            ) {
              fail { "$testName fails: $message. \n$details" }
            }
          }
        }
      }

      println("IDE instance starting...")
      val ideStarter = Starter.newContext(
        testName = "ui-test",
        testCase = testCase(projectInfo),
      ).skipIndicesInitialization().apply {
        val pathToBuildPlugin = System.getProperty("path.to.build.plugin")
        val pathToRobotServerPlugin = System.getProperty("path.to.robot.server.plugin")
        pluginConfigurator
          .installPluginFromPath(File(pathToBuildPlugin).toPath())
          .installPluginFromPath(File(pathToRobotServerPlugin).toPath())

        // FIXME (LLM-22958): disable `Unlock next-level development with free AI` dialog
        //  which interferes with automatic UI tests since 2025.3
        applyVMOptionsPatch { addSystemProperty("llm.show.ai.promotion.window.on.start", "false") }

        // Opt the IDE-under-test out of the Islands A/B experiment so it keeps the classic default
        // Look and Feel. The stripped IDE-under-test doesn't bundle the Islands editor color scheme,
        // so with the Islands theme active the first project-view repaint logs
        // `Theme Islands Dark refers to unknown color scheme Islands Dark`, which ide-starter's
        // ErrorReporterToCI reports as a test failure. `applyIslandsTheme` returns early
        // on `control.option`, leaving the Dark/Darcula default whose color scheme does resolve.
        applyVMOptionsPatch { addSystemProperty("platform.experiment.ab.manual.option", "control.option") }

        // When the surrounding Gradle task supplies a JaCoCo agent jar and a destination (the
        // `uiTest_*` task family in `UITests.kt`), attach the agent to the IDE-under-test JVM
        // so UI-test runs contribute to the coverage report.
        val jacocoAgentJar = System.getProperty("jacoco.agent.jar")
        val jacocoExecFile = System.getProperty("jacoco.exec.file")
        if (jacocoAgentJar != null && jacocoExecFile != null) {
          applyVMOptionsPatch { addLine(jacocoJavaAgentArgument(jacocoAgentJar, jacocoExecFile)) }
        }
      }
      // TODO (#2288): drop `.alsoBypassTestNameSynchronizer()` once 2026.1 support is removed
      val backgroundRun = ideStarter.runIdeWithDriver().alsoBypassTestNameSynchronizer()
      println("IDE instance started")

      println("Rhino project initializing...")
      val rhinoProject = this::class.java.getResource("/project.rhino.js")!!.readText()
      retryOnConnectException(10) {
        robot.runJs(rhinoProject, runInEdt = false)
      }
      println("Rhino project initialized")

      return backgroundRun
    }

    // JVM `-javaagent:` argument that attaches the standalone JaCoCo agent jar to the
    // IDE-under-test JVM. See https://www.jacoco.org/jacoco/trunk/doc/agent.html for the full
    // option reference and the listed defaults.
    private fun jacocoJavaAgentArgument(agentJar: String, execFile: String): String {
      val options = listOf(
        // Output path for the coverage exec. Required - the surrounding `uiTest_<version>`
        // Gradle task uses this exact path when wiring `jacocoUiTestReport`'s `executionData`.
        "destfile=$execFile",
        // Append rather than overwrite if the exec already exists. Also the agent default, but
        // kept explicit as a guard for the moment ide-starter ends up restarting the IDE more
        // than once per `uiTest_*` run - either because a second concrete `*TestSuite` is
        // added (today there's only `UITestSuite`, so `ide.instance-per=class` restarts the
        // IDE once and append is a no-op), or because we switch to `ide.instance-per=method`
        // and the agent dumps after every test method. Without this flag, each restart would
        // clobber the previous one's coverage and the final exec would only cover the last
        // unit run.
        "append=true",
        // Dump the collected data on JVM exit. Agent default; kept explicit for documentation.
        "dumponexit=true",
        // Write the data to a file (alternatives are `tcpserver`/`tcpclient`/`none`). Agent
        // default; kept explicit for documentation.
        "output=file",
        // Don't expose the runtime over JMX. Agent default; we have no consumer for the bean.
        "jmx=false",
        // Required for IntelliJ plugins. IntelliJ's `PluginClassLoader` defines plugin classes
        // without populating `ProtectionDomain.getCodeSource().getLocation()`, and JaCoCo's
        // default (`false`) skips every class whose code source is missing - a guard intended
        // for dynamically-generated bytecode that fires on every class our plugin defines,
        // leaving the exec empty. The flag only gates *whether* a class is instrumented; once
        // it is, the exec records the class's binary name (FQN), which is all the report task
        // needs to map coverage back to a source file via the `sourceDirectories` it's given.
        // The runtime `CodeSource.getLocation()` never enters the source-mapping pipeline.
        "inclnolocationclasses=true",
        // Restrict instrumentation to our own packages. Optional, but without it the exec
        // swells with coverage data for IntelliJ platform classes that we don't own and can't
        // sensibly attribute coverage to, drowning out our actual plugin code in the report.
        "includes=com.virtuslab.*",
      )
      return "-javaagent:$agentJar=${options.joinToString(",")}"
    }
  }

  @BeforeEach
  fun printIntelliJVersion() {
    println("Using IntelliJ $intelliJVersion")
  }

  abstract fun driver(): Driver

  private fun doAndAwait(action: () -> Unit) {
    action()
    println("Waiting for indicators...")
    driver().myWaitForIndicators(2.minutes)
  }

  private fun runJs(@Language("JavaScript") statement: String) {
    println("runJs: executing `$statement`")
    retryOnConnectException(3) {
      robot.runJs("const project = global.get('getSoleOpenProject')(); " + statement, runInEdt = false)
    }
    println("runJs: executed `$statement`")
  }

  private fun <T : java.io.Serializable> callJs(@Language("JavaScript") expression: String): T {
    println("callJs: evaluating `$expression`")
    val result = retryOnConnectException(3) {
      robot.callJs<T>("const project = global.get('getSoleOpenProject')(); " + expression, runInEdt = false)
    }
    val representation = when (result) {
      is IntArray -> result.contentToString()
      is Array<*> -> result.contentDeepToString()
      else -> result.toString()
    }
    println("callJs: evaluated `$expression` to `$representation`")
    return result
  }

  fun assertBranchesAreEqual(branchA: String, branchB: String) {
    val hashA = getHashOfCommitPointedByBranch(branchA)
    val hashB = getHashOfCommitPointedByBranch(branchB)
    assertEquals(hashA, hashB)
  }

  fun assertSyncToParentStatus(branch: String, status: String) {
    val actual = getSyncToParentStatus(branch)
    assertEquals(status, actual)
  }

  fun assertLocalAndRemoteBranchesAreEqual(branch: String) {
    assertBranchesAreEqual(branch, "origin/$branch")
  }

  fun assertNoUncommittedChanges() {
    assertEquals(emptyList<String>(), getDiffOfWorkingTreeToHead())
  }

  fun doesBranchExist(branch: String): Boolean = callJs("project.doesBranchExist('$branch')")
  fun getCurrentBranchName(): String = callJs("project.getCurrentBranchName()")
  fun getDiffOfWorkingTreeToHead(): List<String> = (callJs<java.util.ArrayList<String>>("project.getDiffOfWorkingTreeToHead()")).toList()
  fun getHashOfCommitPointedByBranch(branch: String): String = callJs("project.getHashOfCommitPointedByBranch('$branch')")
  fun getSyncToParentStatus(child: String): String = callJs("project.getSyncToParentStatus('$child')")

  private fun clickButton(visibleText: String) {
    println("clickButton: $visibleText")
    driver().ideFrame {
      x(xQuery { byVisibleText(visibleText) }).click()
    }
  }

  private fun clickToolbarButton(name: String) {
    println("clickToolbarButton: $name")
    driver().ideFrame {
      x(xQuery { byAccessibleName(name) }).click()
    }
  }

  fun acceptBranchDeletionOnSlideOut() = doAndAwait { clickButton("Slide Out & Delete Local Branch") }
  fun acceptSquash() = doAndAwait { clickButton("OK") }
  fun acceptSuggestedBranchLayout() = doAndAwait { clickButton("Yes") }
  fun checkoutBranch(branch: String) = doAndAwait { runJs("project.checkoutBranch('$branch')") }
  fun checkoutFirstChildBranch() = doAndAwait { runJs("project.checkoutFirstChildBranch()") }
  fun checkoutNextBranch() = doAndAwait { runJs("project.checkoutNextBranch()") }
  fun checkoutParentBranch() = doAndAwait { runJs("project.checkoutParentBranch()") }
  fun checkoutPreviousBranch() = doAndAwait { runJs("project.checkoutPreviousBranch()") }
  fun discoverBranchLayout() {
    runJs("project.discoverBranchLayout()")
    doAndAwait { clickButton("Save") }
  }
  fun fastForwardMergeCurrentToParent() = doAndAwait { runJs("project.fastForwardMergeCurrentToParent()") }
  fun fastForwardMergeSelectedToParent(branch: String) = doAndAwait { runJs("project.fastForwardMergeSelectedToParent('$branch')") }
  fun openGitMacheteTab() = runJs("project.openGitMacheteTab()")
  fun pullCurrent() {
    doAndAwait { clickToolbarButton("Pull Current Branch") }
  }
  fun pullSelected(branch: String) = doAndAwait { runJs("project.pullSelected('$branch')") }
  fun refreshModelAndGetManagedBranches(): Array<String> = callJs("project.refreshGraphTableModel(); project.getManagedBranches()")
  fun refreshModelAndGetManagedBranchesAndCommits(): Array<String> = callJs("project.refreshGraphTableModel(); project.getManagedBranchesAndCommits()")
  fun refreshModelAndGetRowCount(): Int = callJs("project.refreshGraphTableModel().getRowCount()")
  fun resetCurrentToRemote() = doAndAwait { runJs("project.resetCurrentToRemote()") }
  fun resetToRemote(branch: String) = doAndAwait { runJs("project.resetToRemote('$branch')") }
  fun slideOutSelected(branch: String) = runJs("project.slideOutSelected('$branch')")
  fun squashCurrent() {
    doAndAwait { clickToolbarButton("Squash\u2026") }
  }
  fun squashSelected(branch: String) = doAndAwait { runJs("project.squashSelected('$branch')") }
  fun syncCurrentToParentByRebase() {
    runJs("project.syncCurrentToParentByRebase()")
    doAndAwait { clickButton("Start Rebasing") }
  }
  fun syncSelectedToParentByMerge(branch: String) = doAndAwait { runJs("project.syncSelectedToParentByMerge('$branch')") }
  fun syncSelectedToParentByRebase(branch: String) {
    runJs("project.syncSelectedToParentByRebase('$branch')")
    doAndAwait { clickButton("Start Rebasing") }
  }
  fun toggleListingCommits() {
    doAndAwait { runJs("project.toggleListingCommits()") }
  }

  val macheteFilePath: Path =
    mainGitDirectoryPath.resolve("machete")

  val machetePreRebaseHookPath: Path =
    mainGitDirectoryPath.resolve("hooks").resolve("machete-pre-rebase")

  val machetePreRebaseHookOutputPath: Path =
    rootDirectoryPath.resolve("machete-pre-rebase-hook-executed")

  val machetePostSlideOutHookPath: Path =
    mainGitDirectoryPath.resolve("hooks").resolve("machete-post-slide-out")
  val machetePostSlideOutHookOutputPath: Path =
    rootDirectoryPath.resolve("machete-post-slide-out-hook-executed")

  fun Path.makeExecutable() {
    val attributes = Files.getPosixFilePermissions(this)
    attributes.add(OWNER_EXECUTE)
    attributes.add(GROUP_EXECUTE)
    attributes.add(OTHERS_EXECUTE)
    Files.setPosixFilePermissions(this, attributes)
  }
}
