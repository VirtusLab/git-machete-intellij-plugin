package com.virtuslab.gitmachete.uitest

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.isProjectOpened
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.ci.CIServer
import com.intellij.ide.starter.ci.NoCIServer
import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.junit5.displayName
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.ide.starter.runner.CurrentTestMethod
import com.intellij.ide.starter.runner.Starter
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
import kotlin.jvm.javaClass
import kotlin.time.Duration.Companion.minutes

abstract class BaseUITestSuite : TestGitRepository(SetupScripts.SETUP_WITH_SINGLE_REMOTE) {

  init {
    di = DI {
      extend(di)
      bindSingleton<CIServer>(overrides = true) {
        object : CIServer by NoCIServer {
          override fun reportTestFailure(
            testName: String,
            message: String,
            details: String,
            linkToLogs: String?,
          ) {
            fail { "$testName fails: $message. \n$details" }
          }
        }
      }
    }
  }

  val intelliJVersion = System.getProperty("intellij.version")
  val robot = RemoteRobot("http://127.0.0.1:8580")

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

  @BeforeEach
  fun beforeEach() {
    println("IntelliJ build number is $intelliJVersion")
  }

  private fun testCase(): TestCase<LocalProjectInfo> {
    val testCase = TestCase(IdeProductProvider.IC, projectInfo = LocalProjectInfo(rootDirectoryPath))
    return if (intelliJVersion.matches("20[0-9][0-9]\\.[0-9].*".toRegex())) {
      testCase.withVersion(intelliJVersion)
    } else {
      testCase.withBuildNumber(intelliJVersion)
    }
  }

  fun uiTest(block: Driver.() -> Unit) {
    Starter.newContext(
      testName = CurrentTestMethod.displayName(),
      testCase = testCase(),
    ).skipIndicesInitialization().apply {
      val pathToBuildPlugin = System.getProperty("path.to.build.plugin")
      val pathToRobotServerPlugin = System.getProperty("path.to.robot.server.plugin")
      PluginConfigurator(this)
        .installPluginFromPath(File(pathToBuildPlugin).toPath())
        .installPluginFromPath(File(pathToRobotServerPlugin).toPath())
    }.runIdeWithDriver().useDriverAndCloseIde {
      println("waiting for project to open...")
      waitForProject(3)

      println("rhino project initializing...")
      val rhinoProject = this.javaClass.getResource("/project.rhino.js")!!.readText()
      retryOnConnectException(3) {
        robot.runJs(rhinoProject, runInEdt = false)
      }
      println("rhino project initialized")

      println("waiting for indicators...")
      waitForIndicators(1.minutes)
      block()
    }
  }

  fun Path.makeExecutable() {
    val attributes = Files.getPosixFilePermissions(this)
    attributes.add(OWNER_EXECUTE)
    attributes.add(GROUP_EXECUTE)
    attributes.add(OTHERS_EXECUTE)
    Files.setPosixFilePermissions(this, attributes)
  }

  private fun Driver.doAndAwait(action: () -> Unit) {
    action()
    println("waiting for indicators...")
    waitForIndicators(1.minutes)
  }

  fun Driver.waitForProject(attempts: Int) {
    var attemptsLeft = attempts
    while (!isProjectOpened() && attemptsLeft > 0) {
      Thread.sleep(3000)
      attemptsLeft--
    }
    if (!isProjectOpened()) {
      throw IllegalStateException("Project has still not been opened, aborting")
    }
  }

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

  private fun runJs(@Language("JavaScript") statement: String) {
    println("runJs: executing `$statement`")
    retryOnConnectException(3) {
      robot.runJs("const project = global.get('project');\n" + statement, runInEdt = false)
    }
    println("runJs: executed `$statement`")
  }

  private fun <T : java.io.Serializable> callJs(@Language("JavaScript") expression: String): T {
    println("callJs: evaluating `$expression`")
    val result = retryOnConnectException(3) {
      robot.callJs<T>("const project = global.get('project');\n" + expression, runInEdt = false)
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

  fun Driver.acceptBranchDeletionOnSlideOut() = doAndAwait { runJs("project.acceptBranchDeletionOnSlideOut()") }
  fun Driver.acceptSquash() = doAndAwait { runJs("project.acceptSquash()") }
  fun Driver.acceptSuggestedBranchLayout() = doAndAwait { runJs("project.acceptSuggestedBranchLayout()") }
  fun Driver.checkoutBranch(branch: String) = doAndAwait { runJs("project.checkoutBranch('$branch')") }
  fun Driver.checkoutFirstChildBranch() = doAndAwait { runJs("project.checkoutFirstChildBranch()") }
  fun Driver.checkoutNextBranch() = doAndAwait { runJs("project.checkoutNextBranch()") }
  fun Driver.checkoutParentBranch() = doAndAwait { runJs("project.checkoutParentBranch()") }
  fun Driver.checkoutPreviousBranch() = doAndAwait { runJs("project.checkoutPreviousBranch()") }
  fun Driver.discoverBranchLayout() = doAndAwait { runJs("project.discoverBranchLayout()") }
  fun Driver.fastForwardMergeCurrentToParent() = doAndAwait { runJs("project.fastForwardMergeCurrentToParent()") }
  fun Driver.fastForwardMergeSelectedToParent(branch: String) = doAndAwait { runJs("project.fastForwardMergeSelectedToParent('$branch')") }
  fun Driver.openGitMacheteTab() = runJs("project.openGitMacheteTab()")
  fun Driver.pullCurrent() = doAndAwait { runJs("project.pullCurrent()") }
  fun Driver.pullSelected(branch: String) = doAndAwait { runJs("project.pullSelected('$branch')") }
  fun Driver.refreshModelAndGetManagedBranches(): Array<String> = callJs("project.refreshGraphTableModel(); project.getManagedBranches()")
  fun Driver.refreshModelAndGetManagedBranchesAndCommits(): Array<String> = callJs("project.refreshGraphTableModel(); project.getManagedBranchesAndCommits()")
  fun Driver.refreshModelAndGetRowCount(): Int = callJs("project.refreshGraphTableModel().getRowCount()")
  fun Driver.resetCurrentToRemote() = doAndAwait { runJs("project.resetCurrentToRemote()") }
  fun Driver.resetToRemote(branch: String) = doAndAwait { runJs("project.resetToRemote('$branch')") }
  fun Driver.slideOutSelected(branch: String) = runJs("project.slideOutSelected('$branch')")
  fun Driver.squashCurrent() = doAndAwait { runJs("project.squashCurrent()") }
  fun Driver.squashSelected(branch: String) = doAndAwait { runJs("project.squashSelected('$branch')") }
  fun Driver.syncCurrentToParentByRebase() = doAndAwait { runJs("project.syncCurrentToParentByRebase()") }
  fun Driver.syncSelectedToParentByMerge(branch: String) = doAndAwait { runJs("project.syncSelectedToParentByMerge('$branch')") }
  fun Driver.syncSelectedToParentByRebase(branch: String) = doAndAwait { runJs("project.syncSelectedToParentByRebase('$branch')") }
  fun Driver.toggleListingCommits() = doAndAwait { runJs("project.toggleListingCommits()") }
}
