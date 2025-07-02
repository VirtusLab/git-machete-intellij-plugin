package com.virtuslab.gitmachete.uitest

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.isProjectOpened
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.remoterobot.RemoteRobot
import com.virtuslab.gitmachete.testcommon.SetupScripts
import com.virtuslab.gitmachete.testcommon.TestGitRepository
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission.*
import kotlin.time.Duration.Companion.minutes

abstract class BaseUITestSuite : TestGitRepository(SetupScripts.SETUP_WITH_SINGLE_REMOTE) {
  companion object {
    val robot = RemoteRobot("http://127.0.0.1:8580")
    val intelliJVersion = System.getProperty("intellij.version")

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
  }

  abstract fun driver(): Driver

  private fun doAndAwait(action: () -> Unit) {
    action()
    println("waiting for indicators...")
    driver().waitForIndicators(1.minutes)
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

  fun acceptBranchDeletionOnSlideOut() = doAndAwait { runJs("project.acceptBranchDeletionOnSlideOut()") }
  fun acceptSquash() = doAndAwait { runJs("project.acceptSquash()") }
  fun acceptSuggestedBranchLayout() = doAndAwait { runJs("project.acceptSuggestedBranchLayout()") }
  fun checkoutBranch(branch: String) = doAndAwait { runJs("project.checkoutBranch('$branch')") }
  fun checkoutFirstChildBranch() = doAndAwait { runJs("project.checkoutFirstChildBranch()") }
  fun checkoutNextBranch() = doAndAwait { runJs("project.checkoutNextBranch()") }
  fun checkoutParentBranch() = doAndAwait { runJs("project.checkoutParentBranch()") }
  fun checkoutPreviousBranch() = doAndAwait { runJs("project.checkoutPreviousBranch()") }
  fun discoverBranchLayout() = doAndAwait { runJs("project.discoverBranchLayout()") }
  fun fastForwardMergeCurrentToParent() = doAndAwait { runJs("project.fastForwardMergeCurrentToParent()") }
  fun fastForwardMergeSelectedToParent(branch: String) = doAndAwait { runJs("project.fastForwardMergeSelectedToParent('$branch')") }
  fun openGitMacheteTab() = runJs("project.openGitMacheteTab()")
  fun pullCurrent() = doAndAwait { runJs("project.pullCurrent()") }
  fun pullSelected(branch: String) = doAndAwait { runJs("project.pullSelected('$branch')") }
  fun refreshModelAndGetManagedBranches(): Array<String> = callJs("project.refreshGraphTableModel(); project.getManagedBranches()")
  fun refreshModelAndGetManagedBranchesAndCommits(): Array<String> = callJs("project.refreshGraphTableModel(); project.getManagedBranchesAndCommits()")
  fun refreshModelAndGetRowCount(): Int = callJs("project.refreshGraphTableModel().getRowCount()")
  fun resetCurrentToRemote() = doAndAwait { runJs("project.resetCurrentToRemote()") }
  fun resetToRemote(branch: String) = doAndAwait { runJs("project.resetToRemote('$branch')") }
  fun slideOutSelected(branch: String) = runJs("project.slideOutSelected('$branch')")
  fun squashCurrent() = doAndAwait { runJs("project.squashCurrent()") }
  fun squashSelected(branch: String) = doAndAwait { runJs("project.squashSelected('$branch')") }
  fun syncCurrentToParentByRebase() = doAndAwait { runJs("project.syncCurrentToParentByRebase()") }
  fun syncSelectedToParentByMerge(branch: String) = doAndAwait { runJs("project.syncSelectedToParentByMerge('$branch')") }
  fun syncSelectedToParentByRebase(branch: String) = doAndAwait { runJs("project.syncSelectedToParentByRebase('$branch')") }
  fun toggleListingCommits() = doAndAwait { runJs("project.toggleListingCommits()") }

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
