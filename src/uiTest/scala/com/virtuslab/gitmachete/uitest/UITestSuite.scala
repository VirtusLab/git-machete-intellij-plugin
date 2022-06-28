package com.virtuslab.gitmachete.uitest

import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedIntegrationTestSuite
import com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_WITH_SINGLE_REMOTE
import org.junit._
import org.junit.rules.TestWatcher
import org.junit.runner.{Description, RunWith}
import org.junit.runners.JUnit4
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.ProbeDriver

import java.io._
import java.nio.file.{Files, Path, Paths}
import java.nio.file.attribute.FileTime
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

  val _saveThreadDumpWhenTestFailed = new TestWatcher() {
    override protected def failed(e: Throwable, description: Description): Unit = {
      val pid = probe.pid()
      probe.screenshot("exception")
      val threadStackTrace: String = Process("jstack " + pid) !!
      val artifactDirectory =
        System.getProperty("user.home") + "/artifacts/uiTest" + intelliJVersion.build + "/thread-dumps"
      Files.createDirectories(Paths.get(artifactDirectory))
      val file: File = new File(artifactDirectory + "/thread_dump_" + pid + ".txt")
      val pw = new PrintWriter(file)
      pw.write(threadStackTrace)
      pw.close()
    }
  }

  @Rule
  def saveThreadDumpWhenTestFailed: TestWatcher = _saveThreadDumpWhenTestFailed

  @Test def skipNonExistentBranches_toggleListingCommits_slideOutRoot(): Unit = {
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
    // 7 branch rows + 11 commit rows
    Assert.assertEquals(18, branchAndCommitRowsCount)

    // Let's slide out a root branch now
    project.contextMenu.openContextMenu("develop")
    project.contextMenu.slideOut()
    project.acceptBranchDeletionOnSlideOut()
    branchAndCommitRowsCount = project.refreshModelAndGetRowCount()
    // 5 branch rows (`develop` is no longer there) + 7 commit rows
    // (1 commit of `allow-ownership-link` and 3 commits of `call-ws` are all gone)
    Assert.assertEquals(13, branchAndCommitRowsCount)

    project.checkoutBranch("master")
    project.contextMenu.openContextMenu("call-ws")
    project.contextMenu.slideOut()
    project.rejectBranchDeletionOnSlideOut()
    branchAndCommitRowsCount = project.refreshModelAndGetRowCount()
    // 4 branch rows (`call-ws` is also no longer there) + 8 commit rows
    Assert.assertEquals(12, branchAndCommitRowsCount)
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

  @Test def fastForwardParentOfBranch(): Unit = {

    // fastForwardParentOfBranch_parentIsCurrentBranch
    project.openGitMacheteTab()
    project.checkoutBranch("master")
    // `master` is the parent of `hotfix/add-trigger`. Let's fast-forward `master` to match `hotfix/add-trigger`.
    project.contextMenu.openContextMenu("hotfix/add-trigger")
    project.contextMenu.fastForwardMerge()
    project.assertBranchesAreEqual("master", "hotfix/add-trigger")
    project.assertNoUncommittedChanges()

    // fastForwardParentOfBranch_childIsCurrentBranch
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

  @Test def pullBranch(): Unit = {

    // pullCurrentBranch
    project.openGitMacheteTab()
    // Remote tracking data is purposefully NOT set for this branch.
    // Our plugin should infer the remote tracking branch based on its name.
    project.checkoutBranch("allow-ownership-link")
    project.toolbar.pull()
    project.assertLocalAndRemoteBranchesAreEqual("allow-ownership-link")
    project.assertNoUncommittedChanges()

    // pullNonCurrentBranch
    project.openGitMacheteTab()
    project.contextMenu.openContextMenu("update-icons")
    project.contextMenu.pull()
    project.assertLocalAndRemoteBranchesAreEqual("update-icons")
    project.assertNoUncommittedChanges()
  }

  @Test def resetBranchToRemote(): Unit = {

    // resetCurrentBranchToRemote
    project.openGitMacheteTab()
    project.checkoutBranch("hotfix/add-trigger")
    project.toolbar.resetToRemote()
    project.acceptResetToRemote()
    project.assertLocalAndRemoteBranchesAreEqual("hotfix/add-trigger")
    project.assertNoUncommittedChanges()
    var currentBranchName = project.getCurrentBranchName()
    Assert.assertEquals("hotfix/add-trigger", currentBranchName)

    // resetNonCurrentBranchToRemote
    project.openGitMacheteTab()
    project.contextMenu.openContextMenu("update-icons")
    project.contextMenu.resetToRemote()
    project.acceptResetToRemote()
    project.assertLocalAndRemoteBranchesAreEqual("update-icons")
    project.assertNoUncommittedChanges()
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
