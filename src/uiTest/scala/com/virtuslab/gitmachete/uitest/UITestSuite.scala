package com.virtuslab.gitmachete.uitest

import com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_WITH_SINGLE_REMOTE
import com.virtuslab.gitmachete.testcommon.TestGitRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.extension.{ExtendWith, ExtensionContext, TestWatcher}
import org.junit.jupiter.api.{AfterEach, BeforeEach, Test}
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.ProbeDriver

import java.io._
import java.nio.file.{Files, Path, Paths}
import java.nio.file.attribute.FileTime
import scala.language.postfixOps
import scala.sys.process._

class ScreenshotAndThreadDumpExtension extends TestWatcher {
  override def testFailed(context: ExtensionContext, cause: Throwable): Unit = {
    val suite = context.getTestInstance.get().asInstanceOf[UITestSuite]

    val pid = suite.probe.pid()
    suite.probe.screenshot("exception")
    val threadStackTrace: String = Process("jstack " + pid) !!
    val homeDirectory = System.getProperty("user.home")
    val artifactDirectory = s"${homeDirectory}/.ideprobe-uitests/artifacts/uiTest${suite.intelliJVersion}/thread-dumps"
    Files.createDirectories(Paths.get(artifactDirectory))
    val file: File = new File(artifactDirectory + "/thread_dump_" + pid + ".txt")
    val pw = new PrintWriter(file)
    pw.write(threadStackTrace)
    pw.close()
  }
}

object UITestSuite extends UISuite {
  // We need to declare a companion object so that JUnit sees @BeforeAll/@AfterAll static methods.
}

@ExtendWith(Array(classOf[ScreenshotAndThreadDumpExtension]))
class UITestSuite extends TestGitRepository(SETUP_WITH_SINGLE_REMOTE) {

  import UITestSuite._

  val project = intelliJ.project
  val intelliJVersion = intelliJ.config.apply[String]("probe.intellij.version.build")

  val probe: ProbeDriver = intelliJ.probe

  @BeforeEach
  def beforeEach(): Unit = {
    println("IntelliJ build number is " + intelliJVersion)
    intelliJ.doAndAwait {
      probe.openProject(rootDirectoryPath)
      project.configure()
    }
  }

  @AfterEach
  def afterEach(): Unit = {
    waitAndCloseProject()
  }

  @Test def skipNonExistentBranches_toggleListingCommits_slideOutRoot(): Unit = {
    overwriteMacheteFile(
      """develop
        |  non-existent
        |    allow-ownership-link
        |      build-chain
        |      update-icons
        |    non-existent-leaf
        |  call-ws
        |non-existent-root
        |master
        |  hotfix/add-trigger""".stripMargin
    )
    project.openGitMacheteTab()
    val managedBranches = project.refreshModelAndGetManagedBranches()
    // Non-existent branches should be skipped while causing no error (only a low-severity notification).
    assertEquals(
      Seq(
        "develop",
        "allow-ownership-link",
        "build-chain",
        "update-icons",
        "call-ws",
        "master",
        "hotfix/add-trigger"
      ),
      managedBranches.toSeq
    )
    project.toggleListingCommits()
    var branchAndCommitRowsCount = project.refreshModelAndGetRowCount()
    // 7 branch rows + 11 commit rows
    assertEquals(18, branchAndCommitRowsCount)

    project.checkoutBranch("allow-ownership-link")
    project.checkoutFirstChildBranch()
    assertEquals("build-chain", project.getCurrentBranchName())
    project.checkoutNextBranch()
    assertEquals("update-icons", project.getCurrentBranchName())
    project.checkoutPreviousBranch()
    assertEquals("build-chain", project.getCurrentBranchName())
    project.checkoutParentBranch()
    assertEquals("allow-ownership-link", project.getCurrentBranchName())

    // Let's slide out a root branch now
    project.slideOutSelected("develop")
    project.acceptBranchDeletionOnSlideOut()
    branchAndCommitRowsCount = project.refreshModelAndGetRowCount()
    // 6 branch rows (`develop` is no longer there) + 7 commit rows
    // (1 commit of `allow-ownership-link` and 3 commits of `call-ws` are all gone)
    assertEquals(13, branchAndCommitRowsCount)

    project.checkoutBranch("master")
    project.slideOutSelected("call-ws")
    project.rejectBranchDeletionOnSlideOut()
    val managedBranchesAfterSlideOut = project.refreshModelAndGetManagedBranches()
    // Non-existent branches should be skipped while causing no error (only a low-severity notification).
    assertEquals(
      Seq(
        "allow-ownership-link",
        "build-chain",
        "update-icons",
        "master",
        "hotfix/add-trigger"
      ),
      managedBranchesAfterSlideOut.toSeq
    )
    branchAndCommitRowsCount = project.refreshModelAndGetRowCount()
    // 5 branch rows (`call-ws` is also no longer there) + 7 commit rows
    assertEquals(12, branchAndCommitRowsCount)
  }

  @Test def discoverBranchLayout(): Unit = {
    // When model is refreshed and machete file is has not been modified for a long time, then discover suggestion should occur
    setLastModifiedDateOfMacheteFileToEpochStart()
    project.openGitMacheteTab()
    project.acceptSuggestedBranchLayout()
    probe.await()
    var branchRowsCount = project.refreshModelAndGetRowCount()
    assertEquals(8, branchRowsCount)
    deleteMacheteFile()
    // When model is refreshed and machete file is empty, then autodiscover should occur
    branchRowsCount = project.refreshModelAndGetRowCount()
    assertEquals(8, branchRowsCount)
    // This time, wipe out `machete` file (instead of removing it completely)
    overwriteMacheteFile("")
    // Now let's test an explicit discover instead
    project.discoverBranchLayout()
    branchRowsCount = project.refreshModelAndGetRowCount()
    assertEquals(8, branchRowsCount)
    // In this case a non-existent branch is defined by `machete` file and it should persist (no autodiscover)
    overwriteMacheteFile("non-existent")
    branchRowsCount = project.refreshModelAndGetRowCount()
    assertEquals(0, branchRowsCount)
  }

  @Test def fastForwardParentOfBranch(): Unit = {

    // fastForwardParentOfBranch_parentIsCurrentBranch
    project.openGitMacheteTab()
    project.checkoutBranch("master")
    // `master` is the parent of `hotfix/add-trigger`. Let's fast-forward `master` to match `hotfix/add-trigger`.
    project.fastForwardMergeSelectedToParent("hotfix/add-trigger")
    project.assertBranchesAreEqual("master", "hotfix/add-trigger")
    project.assertNoUncommittedChanges()

    // fastForwardParentOfBranch_childIsCurrentBranch
    project.checkoutBranch("call-ws")
    project.fastForwardMergeCurrentToParent()
    project.assertBranchesAreEqual("develop", "call-ws")
    project.assertNoUncommittedChanges()
  }

  @Test def syncToParentByRebaseAction(): Unit = {

    // syncCurrentToParentByRebase
    project.openGitMacheteTab()
    project.checkoutBranch("allow-ownership-link")
    project.syncCurrentToParentByRebase()
    project.assertSyncToParentStatus("allow-ownership-link", "InSync")

    // syncSelectedToParentByRebase
    project.syncSelectedToParentByRebase("build-chain")
    project.assertSyncToParentStatus("build-chain", "InSync")
  }

  @Test def syncToParentByMergeAction(): Unit = {

    // syncSelectedToParentByMerge
    project.openGitMacheteTab()
    project.syncSelectedToParentByMerge("call-ws")
    project.assertSyncToParentStatus("call-ws", "InSync")
  }

  @Test def pullBranch(): Unit = {

    // pullCurrentBranch
    project.openGitMacheteTab()
    // Remote tracking data is purposefully NOT set for this branch.
    // Our plugin should infer the remote tracking branch based on its name.
    project.checkoutBranch("allow-ownership-link")
    project.pullCurrent()
    project.assertLocalAndRemoteBranchesAreEqual("allow-ownership-link")
    project.assertNoUncommittedChanges()

    // pullNonCurrentBranch
    project.openGitMacheteTab()
    project.pullSelected("update-icons")
    project.assertLocalAndRemoteBranchesAreEqual("update-icons")
    project.assertNoUncommittedChanges()
  }

  @Test def resetBranchToRemote(): Unit = {

    // resetCurrentBranchToRemote
    project.openGitMacheteTab()
    project.checkoutBranch("hotfix/add-trigger")
    project.resetCurrentToRemote()
    project.assertLocalAndRemoteBranchesAreEqual("hotfix/add-trigger")
    project.assertNoUncommittedChanges()
    val currentBranchName = project.getCurrentBranchName()
    assertEquals("hotfix/add-trigger", currentBranchName)

    // resetNonCurrentBranchToRemote
    project.openGitMacheteTab()
    project.resetToRemote("update-icons")
    project.assertLocalAndRemoteBranchesAreEqual("update-icons")
    project.assertNoUncommittedChanges()
  }

  @Test def squashBranch(): Unit = {

    // squashCurrentBranch
    project.openGitMacheteTab()
    project.toggleListingCommits()
    val branchRowsCount = project.refreshModelAndGetRowCount()
    assertEquals(18, branchRowsCount)
    project.checkoutBranch("call-ws")
    project.squashCurrent()
    project.acceptSquash()

    // call-ws had 3 commits before the squash
    var managedBranches = project.refreshModelAndGetManagedBranchesAndCommits()
    assertEquals(
      Seq(
        "develop",
        "Allow ownership links",
        "allow-ownership-link",
        "Use new icons",
        "1st round of fixes",
        "update-icons",
        "Build arbitrarily long chains",
        "Use new icons",
        "1st round of fixes",
        "build-chain",
        "Call web service",
        "call-ws",
        "master",
        "HOTFIX Add the trigger - fixes",
        "HOTFIX Add the trigger",
        "hotfix/add-trigger"
      ),
      managedBranches.toSeq
    )

    assertEquals(16, managedBranches.length)

    // squashNonCurrentBranch
    project.squashSelected("hotfix/add-trigger")
    project.acceptSquash()
    // call-ws had 3 commits before the squash
    managedBranches = project.refreshModelAndGetManagedBranchesAndCommits()
    assertEquals(
      Seq(
        "develop",
        "Allow ownership links",
        "allow-ownership-link",
        "Use new icons",
        "1st round of fixes",
        "update-icons",
        "Build arbitrarily long chains",
        "Use new icons",
        "1st round of fixes",
        "build-chain",
        "Call web service",
        "call-ws",
        "master",
        "HOTFIX Add the trigger",
        "hotfix/add-trigger"
      ),
      managedBranches.toSeq
    )

    assertEquals(15, managedBranches.length)
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
