package com.virtuslab.gitmachete.uitest

import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedIntegrationTestSuite
import com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_WITH_SINGLE_REMOTE

import scala.language.postfixOps
import scala.sys.process._
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit._
import org.virtuslab.ideprobe.Extensions._

import java.io._
import java.nio.file.{Files, Path}
import java.nio.file.attribute.FileTime

@RunWith(classOf[JUnit4])
class UITestSuite extends BaseGitRepositoryBackedIntegrationTestSuite(SETUP_WITH_SINGLE_REMOTE) {

  import UISuite._
  UISuite.setup()

  @Before
  def beforeEach(): Unit = {
    intelliJ.probe.openProject(rootDirectoryPath)
    intelliJ.project.configure()
    intelliJ.probe.await()
  }

  @After
  def afterEach(): Unit = {
    intelliJ.probe.await()
    // Note that we shouldn't wait for a response here (so we shouldn't use org.virtuslab.ideprobe.ProbeDriver#closeProject),
    // since the response sometimes never comes (due to the project being closed), depending on the specific timing.
    intelliJ.ide.closeOpenedProjects()
  }

  @Test def skipNonExistentBranches_toggleListingCommits_slideOutRoot(): Unit = {
    //TODO (#830): try ... catch block to discover why the SocketTimeoutException occurs
    try {
      intelliJ.project.openGitMacheteTab()
      overwriteMacheteFile(
        """develop
          |  non-existent
          |    allow-ownership-link
          |      build-chain
          |    non-existent-leaf
          |  call-ws
          |non-existent-root
          |master
          |  hotfix/add-trigger""".stripMargin
      )
      val managedBranches = intelliJ.project.refreshModelAndGetManagedBranches()
      // Non-existent branches should be skipped while causing no error (only a low-severity notification).
      Assert.assertEquals(
        Seq("allow-ownership-link", "build-chain", "call-ws", "develop", "hotfix/add-trigger", "master"),
        managedBranches.toSeq.sorted)
      intelliJ.project.toggleListingCommits()
      var branchAndCommitRowsCount = intelliJ.project.refreshModelAndGetRowCount()
      // 6 branch rows + 7 commit rows
      Assert.assertEquals(13, branchAndCommitRowsCount)

      // Let's slide out a root branch now
      intelliJ.project.slideOutBranch("develop")
      intelliJ.project.acceptBranchDeletionOnSlideOut()
      branchAndCommitRowsCount = intelliJ.project.refreshModelAndGetRowCount()
      // 5 branch rows (`develop` is no longer there) + 3 commit rows
      // (1 commit of `allow-ownership-link` and 3 commits of `call-ws` are all gone)
      Assert.assertEquals(8, branchAndCommitRowsCount)

      intelliJ.project.checkoutBranch("develop")
      intelliJ.project.slideOutBranch("call-ws")
      intelliJ.project.rejectBranchDeletionOnSlideOut()
      branchAndCommitRowsCount = intelliJ.project.refreshModelAndGetRowCount()
      // 4 branch rows (`call-ws` is also no longer there) + 3 commit rows
      Assert.assertEquals(7, branchAndCommitRowsCount)
    } catch {
      case e: Exception =>
        saveThreadDumpToFile()
        throw e
    }
  }

  @Test def discoverBranchLayout(): Unit = {
    // When model is refreshed and machete file is has not been modified for a long time, then discover suggestion should occur
    setLastModifiedDateOfMacheteFileToEpochStart()
    intelliJ.project.openGitMacheteTab()
    intelliJ.project.acceptSuggestedBranchLayout()
    intelliJ.probe.await()
    var branchRowsCount = intelliJ.project.refreshModelAndGetRowCount()
    Assert.assertEquals(7, branchRowsCount)
    deleteMacheteFile()
    // When model is refreshed and machete file is empty, then autodiscover should occur
    branchRowsCount = intelliJ.project.refreshModelAndGetRowCount()
    Assert.assertEquals(7, branchRowsCount)
    // This time, wipe out `machete` file (instead of removing it completely)
    overwriteMacheteFile("")
    // Now let's test an explicit discover instead
    intelliJ.project.discoverBranchLayout()
    branchRowsCount = intelliJ.project.refreshModelAndGetRowCount()
    Assert.assertEquals(7, branchRowsCount)
    // In this case a non-existent branch is defined by `machete` file and it should persist (no autodiscover)
    overwriteMacheteFile("non-existent")
    branchRowsCount = intelliJ.project.refreshModelAndGetRowCount()
    Assert.assertEquals(0, branchRowsCount)
  }

  // TODO (#843): merge current/non-current branch ui test cases (for the other actions too!)
  @Test def fastForwardParentOfBranch_parentIsCurrentBranch(): Unit = {
    intelliJ.project.openGitMacheteTab()
    intelliJ.project.checkoutBranch("master")
    // `master` is the parent of `hotfix/add-trigger`. Let's fast-forward `master` to match `hotfix/add-trigger`.
    intelliJ.project.fastForwardMergeSelectedBranchToParent("hotfix/add-trigger")
    intelliJ.project.assertBranchesAreEqual("master", "hotfix/add-trigger")
    intelliJ.project.assertNoUncommittedChanges()
  }

  @Test def fastForwardParentOfBranch_childIsCurrentBranch(): Unit = {
    intelliJ.project.openGitMacheteTab()
    intelliJ.project.checkoutBranch("hotfix/add-trigger")
    intelliJ.project.fastForwardMergeCurrentBranchToParent()
    intelliJ.project.assertBranchesAreEqual("master", "hotfix/add-trigger")
    intelliJ.project.assertNoUncommittedChanges()
  }

  @Ignore("UI tests are time-consuming and sync by rebase is used so frequently that any issues will be found quickly anyway")
  @Test def syncToParentByRebaseAction(): Unit = {

    // syncCurrentToParentByRebase
    intelliJ.project.openGitMacheteTab()
    intelliJ.project.checkoutBranch("allow-ownership-link")
    intelliJ.project.syncCurrentToParentByRebaseAction()
    intelliJ.project.acceptRebase()
    intelliJ.project.assertSyncToParentStatus("allow-ownership-link", "InSync")

    // syncSelectedToParentByRebase
    intelliJ.project.syncSelectedToParentByRebaseAction("build-chain")
    intelliJ.project.acceptRebase()
    intelliJ.project.assertSyncToParentStatus("build-chain", "InSync")
  }

  @Test def syncToParentByMergeAction(): Unit = {

    // syncCurrentToParentByMerge
    intelliJ.project.openGitMacheteTab()
    intelliJ.project.checkoutBranch("allow-ownership-link")
    intelliJ.project.syncCurrentToParentByMergeAction()
    intelliJ.project.assertSyncToParentStatus("allow-ownership-link", "InSync")

    // syncSelectedToParentByMerge
    intelliJ.project.syncSelectedToParentByMergeAction("build-chain")
    intelliJ.project.assertSyncToParentStatus("build-chain", "InSync")
  }

  @Test def pullCurrentBranch(): Unit = {
    intelliJ.project.openGitMacheteTab()
    // Remote tracking data is purposefully NOT set for this branch.
    // Our plugin should infer the remote tracking branch based on its name.
    intelliJ.project.checkoutBranch("allow-ownership-link")
    intelliJ.project.pullCurrentBranch()
    intelliJ.project.assertLocalAndRemoteBranchesAreEqual("allow-ownership-link")
    intelliJ.project.assertNoUncommittedChanges()
  }

  @Test def pullNonCurrentBranch(): Unit = {
    intelliJ.project.openGitMacheteTab()
    intelliJ.project.checkoutBranch("develop")
    intelliJ.project.pullSelectedBranch("allow-ownership-link")
    intelliJ.project.assertLocalAndRemoteBranchesAreEqual("allow-ownership-link")
    intelliJ.project.assertNoUncommittedChanges()
  }

  @Test def resetCurrentBranchToRemote(): Unit = {
    intelliJ.project.openGitMacheteTab()
    intelliJ.project.checkoutBranch("hotfix/add-trigger")
    intelliJ.project.resetCurrentBranchToRemote()
    intelliJ.project.assertLocalAndRemoteBranchesAreEqual("hotfix/add-trigger")
    intelliJ.project.assertNoUncommittedChanges()
    val currentBranchName = intelliJ.project.getCurrentBranchName()
    Assert.assertEquals("hotfix/add-trigger", currentBranchName)
  }

  @Test def resetNonCurrentBranchToRemote(): Unit = {
    intelliJ.project.openGitMacheteTab()
    intelliJ.project.checkoutBranch("develop")
    intelliJ.project.resetBranchToRemote("hotfix/add-trigger")
    intelliJ.project.assertLocalAndRemoteBranchesAreEqual("hotfix/add-trigger")
    intelliJ.project.assertNoUncommittedChanges()
    val currentBranchName = intelliJ.project.getCurrentBranchName()
    Assert.assertEquals("develop", currentBranchName)
  }

  private def saveThreadDumpToFile(): Unit = {
    val pid = intelliJ.probe.pid()
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
