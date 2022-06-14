package com.virtuslab.gitmachete.uitest

import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedIntegrationTestSuite
import com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_WITH_SINGLE_REMOTE
import org.junit._
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.ProbeDriver

import java.io._
import java.nio.file.attribute.FileTime
import java.nio.file.{Files, Path}
import scala.language.postfixOps
import scala.sys.process._

object UITestSuite extends UISuite {}

@RunWith(classOf[JUnit4])
class UITestSuite extends BaseGitRepositoryBackedIntegrationTestSuite(SETUP_WITH_SINGLE_REMOTE) {
  import UITestSuite._

  private val project = intelliJ.project

  private val probe: ProbeDriver = intelliJ.probe

  @Before
  def beforeEach(): Unit = {
    probe.openProject(rootDirectoryPath)
    project.configure()
    probe.await()
  }

  @After
  def afterEach(): Unit = {
    waitAndCloseProject()
  }

  @Test def skipNonExistentBranches_toggleListingCommits_slideOutRoot(): Unit = {
    // TODO (#830): try ... catch block to discover why the SocketTimeoutException occurs
    try {
      project.openGitMacheteTab()
      overwriteMacheteFile(
        """develop
          |  non-existent
          |    allow-ownership-link
          |      update-icons
          |      build-chain
          |    non-existent-leaf
          |  call-ws
          |non-existent-root
          |master
          |  hotfix/add-trigger""".stripMargin
      )
      val managedBranches = project.refreshModelAndGetManagedBranches()
      // Non-existent branches should be skipped while causing no error (only a low-severity notification).
      Assert.assertEquals(
        Seq(
          "allow-ownership-link",
          "build-chain",
          "call-ws",
          "develop",
          "hotfix/add-trigger",
          "master",
          "update-icons"
        ),
        managedBranches.toSeq.sorted
      )
      project.toolbar.toggleListingCommits()
      var branchAndCommitRowsCount = project.refreshModelAndGetRowCount()
      // 7 branch rows + 8 commit rows
      Assert.assertEquals(15, branchAndCommitRowsCount)

      // Let's slide out a root branch now
      project.contextMenu.openContextMenu("develop")
      project.contextMenu.slideOut()
      project.acceptBranchDeletionOnSlideOut()
      branchAndCommitRowsCount = project.refreshModelAndGetRowCount()
      // 5 branch rows (`develop` is no longer there) + 3 commit rows
      // (1 commit of `allow-ownership-link` and 3 commits of `call-ws` are all gone)
      Assert.assertEquals(9, branchAndCommitRowsCount)

      project.contextMenu.openContextMenu("develop")
      project.checkoutBranch("develop")
      project.contextMenu.openContextMenu("call-ws")
      project.contextMenu.slideOut()
      project.rejectBranchDeletionOnSlideOut()
      branchAndCommitRowsCount = project.refreshModelAndGetRowCount()
      // 4 branch rows (`call-ws` is also no longer there) + 3 commit rows
      Assert.assertEquals(8, branchAndCommitRowsCount)
    } catch {
      case e: Exception =>
        saveThreadDumpToFile()
        throw e
    }
  }

  @Test def discoverBranchLayout(): Unit = {
    // When model is refreshed and machete file is has not been modified for a long time, then discover suggestion should occur
    setLastModifiedDateOfMacheteFileToEpochStart()
    project.openGitMacheteTab()
    project.acceptSuggestedBranchLayout()
    probe.await()
    var branchRowsCount = project.refreshModelAndGetRowCount()
    Assert.assertEquals(8, branchRowsCount)
    deleteMacheteFile()
    // When model is refreshed and machete file is empty, then autodiscover should occur
    branchRowsCount = project.refreshModelAndGetRowCount()
    Assert.assertEquals(8, branchRowsCount)
    // This time, wipe out `machete` file (instead of removing it completely)
    overwriteMacheteFile("")
    // Now let's test an explicit discover instead
    project.toolbar.discoverBranchLayout()
    project.saveDiscoveredBranchLayout()
    branchRowsCount = project.refreshModelAndGetRowCount()
    Assert.assertEquals(8, branchRowsCount)
    // In this case a non-existent branch is defined by `machete` file and it should persist (no autodiscover)
    overwriteMacheteFile("non-existent")
    branchRowsCount = project.refreshModelAndGetRowCount()
    Assert.assertEquals(0, branchRowsCount)
  }

  // TODO (#843): merge current/non-current branch ui test cases (for the other actions too!)
  @Test def fastForwardParentOfBranch_parentIsCurrentBranch(): Unit = {
    project.openGitMacheteTab()
    project.checkoutBranch("master")
    // `master` is the parent of `hotfix/add-trigger`. Let's fast-forward `master` to match `hotfix/add-trigger`.
    project.contextMenu.openContextMenu("hotfix/add-trigger")
    project.contextMenu.fastForwardMerge()
    project.assertBranchesAreEqual("master", "hotfix/add-trigger")
    project.assertNoUncommittedChanges()
  }

  @Test def fastForwardParentOfBranch_childIsCurrentBranch(): Unit = {
    project.openGitMacheteTab()
    project.checkoutBranch("call-ws")
    project.toolbar.fastForwardMerge()
    project.assertBranchesAreEqual("develop", "call-ws")
    project.assertNoUncommittedChanges()
  }

  @Test def syncToParentByRebaseAction(): Unit = {

    // syncCurrentToParentByRebase
    project.openGitMacheteTab()
    project.checkoutBranch("allow-ownership-link")
    project.toolbar.syncByRebase()
    project.acceptRebase()
    project.assertSyncToParentStatus("allow-ownership-link", "InSync")

    // syncSelectedToParentByRebase
    project.contextMenu.openContextMenu("build-chain")
    project.contextMenu.checkoutAndSyncByRebase()
    project.acceptRebase()
    project.assertSyncToParentStatus("build-chain", "InSync")
  }

  @Test def syncToParentByMergeAction(): Unit = {

    // syncCurrentToParentByMerge
    project.openGitMacheteTab()
    project.checkoutBranch("allow-ownership-link")
    project.toolbar.syncByMerge()
    project.assertSyncToParentStatus("allow-ownership-link", "InSync")

    // syncSelectedToParentByMerge
    project.contextMenu.openContextMenu("build-chain")
    project.contextMenu.checkoutAndSyncByMerge()
    project.assertSyncToParentStatus("build-chain", "InSync")
  }

  @Test def pullCurrentBranch(): Unit = {
    project.openGitMacheteTab()
    // Remote tracking data is purposefully NOT set for this branch.
    // Our plugin should infer the remote tracking branch based on its name.
    project.checkoutBranch("allow-ownership-link")
    project.toolbar.pull()
    project.assertLocalAndRemoteBranchesAreEqual("allow-ownership-link")
    project.assertNoUncommittedChanges()
  }

  @Test def pullNonCurrentBranch(): Unit = {
    project.openGitMacheteTab()
    project.checkoutBranch("develop")
    project.contextMenu.openContextMenu("update-icons")
    project.contextMenu.pull()
    project.assertLocalAndRemoteBranchesAreEqual("update-icons")
    project.assertNoUncommittedChanges()
  }

  @Test def resetCurrentBranchToRemote(): Unit = {
    project.openGitMacheteTab()
    project.checkoutBranch("hotfix/add-trigger")
    project.toolbar.resetToRemote()
    project.acceptResetToRemote()
    project.assertLocalAndRemoteBranchesAreEqual("hotfix/add-trigger")
    project.assertNoUncommittedChanges()
    val currentBranchName = project.getCurrentBranchName()
    Assert.assertEquals("hotfix/add-trigger", currentBranchName)
  }

  @Test def resetNonCurrentBranchToRemote(): Unit = {
    project.openGitMacheteTab()
    project.checkoutBranch("develop")
    project.contextMenu.openContextMenu("update-icons")
    project.contextMenu.resetToRemote()
    project.acceptResetToRemote()
    project.assertLocalAndRemoteBranchesAreEqual("update-icons")
    project.assertNoUncommittedChanges()
    val currentBranchName = project.getCurrentBranchName()
    Assert.assertEquals("develop", currentBranchName)
  }

  private def saveThreadDumpToFile(): Unit = {
    val pid = probe.pid()
    val threadStackTrace: String = Process("jstack " + pid) !!
    val file: File = new File("build/thread-dump/thread_dump_" + pid + ".txt")
    if (file.getParentFile.mkdirs()) {
      val pw = new PrintWriter(file)
      pw.write(threadStackTrace)
      pw.close()
    }
  }

  private def macheteFilePath: Path = mainGitDirectoryPath.resolve("machete")

  private def deleteMacheteFile(): Unit = {
    macheteFilePath.delete()
  }

  private def overwriteMacheteFile(content: String): Unit = {
    macheteFilePath.write(content + "\n")
  }

  private def setLastModifiedDateOfMacheteFileToEpochStart(): Unit = {
    Files.setLastModifiedTime(macheteFilePath, FileTime.fromMillis(0))
  }
}
