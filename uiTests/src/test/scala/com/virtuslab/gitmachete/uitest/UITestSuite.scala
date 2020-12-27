package com.virtuslab.gitmachete.uitest

import java.nio.file.Files
import java.nio.file.attribute.FileTime

import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedIntegrationTestSuite
import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedIntegrationTestSuite.SETUP_WITH_SINGLE_REMOTE
import org.junit.{After, Assert, Before, Test}
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe._
import org.virtuslab.ideprobe.config._
import org.virtuslab.ideprobe.dependencies._
import org.virtuslab.ideprobe.ide.intellij.IntelliJFactory
import org.virtuslab.ideprobe.junit4.RunningIntelliJPerSuite

object UITestSuite
  extends RunningIntelliJPerSuite
    with IdeProbeFixture
    with GitMacheteExtension {

  override protected def baseFixture: IntelliJFixture = transformFixture {
    val version = sys.props.get("intellij.version").filterNot(_.isEmpty).map(IntelliJVersion(_, None))
      .getOrElse(throw new Exception("IntelliJ version is not provided"))
    val driverConfig = DriverConfig(
      vmOptions = Seq("-Xmx1G"),
      check = CheckConfig(errors = true)
    )
    val config = Config.fromString(
      """
        |probe.endpoints.awaitIdle {
        |    initialWait = "1 second"
        |    newTaskWait = "2 seconds"
        |    checkFrequency = "250 millis"
        |}
        |""".stripMargin)

    IntelliJFixture(
      version = version,
      factory = IntelliJFactory.Default.withConfig(driverConfig),
      config = config
    )
  }

}

@RunWith(classOf[JUnit4])
class UITestSuite extends BaseGitRepositoryBackedIntegrationTestSuite(SETUP_WITH_SINGLE_REMOTE) {

  import UITestSuite._

  @Before
  def beforeEach(): Unit = {
    intelliJ.probe.openProject(repositoryMainDir)
    intelliJ.machete.runJs("project.configure()")
    intelliJ.probe.awaitIdle()
  }

  @After
  def afterEach(): Unit = {
    intelliJ.probe.awaitIdle()
    // Note that we shouldn't wait for a response here (so we shouldn't call org.virtuslab.ideprobe.ProbeDriver#send),
    // since the response sometimes never comes (due to the project being closed), depending on the specific timing.
    intelliJ.machete.runJs("ide.closeOpenedProjects()")
  }

  @Test def skipNonExistentBranches_toggleListingCommits_slideOutRoot(): Unit = {
    intelliJ.machete.runJs("project.openGitMacheteTab()")
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
    val branchRowsCount = intelliJ.machete.refreshModelAndGetRowCount()
    // There should be exactly 6 rows in the graph table, since there are 6 existing branches in machete file
    // non-existent branches should be skipped while causing no error (only a low-severity notification).
    Assert.assertEquals(6, branchRowsCount)
    intelliJ.machete.runJs("project.toggleListingCommits()")
    var branchAndCommitRowsCount = intelliJ.machete.refreshModelAndGetRowCount()
    // 6 branch rows + 7 commit rows
    Assert.assertEquals(13, branchAndCommitRowsCount)

    // Let's slide out a root branch now
    intelliJ.machete.runJs("project.slideOutBranch('develop')")
    intelliJ.probe.awaitIdle()
    branchAndCommitRowsCount = intelliJ.machete.refreshModelAndGetRowCount()
    // 5 branch rows (`develop` is no longer there) + 3 commit rows
    // (1 commit of `allow-ownership-link` and 3 commits of `call-ws` are all gone)
    Assert.assertEquals(8, branchAndCommitRowsCount)
  }

  @Test def discoverBranchLayout(): Unit = {
    // When model is refreshed and machete file is has not been modified for a long time, then discover suggestion should occur
    setLastModifiedDateOfMacheteFileToEpochStart()
    intelliJ.machete.runJs("project.openGitMacheteTab()")
    intelliJ.machete.runJs("project.acceptSuggestedBranchLayout()")
    intelliJ.probe.awaitIdle()
    var branchRowsCount = intelliJ.machete.refreshModelAndGetRowCount()
    Assert.assertEquals(7, branchRowsCount)
    deleteMacheteFile()
    // When model is refreshed and machete file is empty, then autodiscover should occur
    branchRowsCount = intelliJ.machete.refreshModelAndGetRowCount()
    Assert.assertEquals(7, branchRowsCount)
    // This time, wipe out `machete` file (instead of removing it completely)
    overwriteMacheteFile("")
    // Now let's test an explicit discover instead
    intelliJ.machete.runJs("project.discoverBranchLayout()")
    branchRowsCount = intelliJ.machete.refreshModelAndGetRowCount()
    Assert.assertEquals(7, branchRowsCount)
    // In this case a non-existent branch is defined by `machete` file and it should persist (no autodiscover)
    overwriteMacheteFile("non-existent")
    branchRowsCount = intelliJ.machete.refreshModelAndGetRowCount()
    Assert.assertEquals(0, branchRowsCount)
  }

  @Test def fastForwardParentOfBranch_parentIsCurrentBranch(): Unit = {
    intelliJ.machete.runJs("project.openGitMacheteTab()")
    intelliJ.machete.checkoutBranch("master")
    // `master` is the parent of `hotfix/add-trigger`. Let's fast-forward `master` to match `hotfix/add-trigger`.
    intelliJ.machete.fastForwardMergeSelectedBranchToParent("hotfix/add-trigger")
    intelliJ.machete.assertBranchesAreEqual("master", "hotfix/add-trigger")
    intelliJ.machete.assertWorkingTreeIsAtHead()
  }

  @Test def fastForwardParentOfBranch_childIsCurrentBranch(): Unit = {
    intelliJ.machete.runJs("project.openGitMacheteTab()")
    intelliJ.machete.checkoutBranch("hotfix/add-trigger")
    intelliJ.machete.fastForwardMergeCurrentBranchToParent()
    intelliJ.machete.assertBranchesAreEqual("master", "hotfix/add-trigger")
    intelliJ.machete.assertWorkingTreeIsAtHead()
  }

  @Test def pullCurrentBranch(): Unit = {
    intelliJ.machete.runJs("project.openGitMacheteTab()")
    // Remote tracking data is purposefully NOT set for this branch.
    // Our plugin should infer the remote tracking branch based on its name.
    intelliJ.machete.checkoutBranch("allow-ownership-link")
    intelliJ.machete.pullCurrentBranch()
    intelliJ.machete.assertLocalAndRemoteBranchesAreEqual("allow-ownership-link")
    intelliJ.machete.assertWorkingTreeIsAtHead()
  }

  @Test def pullNonCurrentBranch(): Unit = {
    intelliJ.machete.runJs("project.openGitMacheteTab()")
    intelliJ.machete.checkoutBranch("develop")
    intelliJ.machete.pullBranch("allow-ownership-link")
    intelliJ.machete.assertLocalAndRemoteBranchesAreEqual("allow-ownership-link")
    intelliJ.machete.assertWorkingTreeIsAtHead()
  }

  @Test def resetCurrentBranchToRemote(): Unit = {
    intelliJ.machete.runJs("project.openGitMacheteTab()")
    intelliJ.machete.checkoutBranch("hotfix/add-trigger")
    intelliJ.machete.resetCurrentBranchToRemote()
    intelliJ.machete.assertLocalAndRemoteBranchesAreEqual("hotfix/add-trigger")
    intelliJ.machete.assertWorkingTreeIsAtHead()
    val currentBranchName = intelliJ.machete.getCurrentBranchName()
    Assert.assertEquals("hotfix/add-trigger", currentBranchName)
  }

  @Test def resetNonCurrentBranchToRemote(): Unit = {
    intelliJ.machete.runJs("project.openGitMacheteTab()")
    intelliJ.machete.checkoutBranch("develop")
    intelliJ.machete.resetBranchToRemote("hotfix/add-trigger")
    intelliJ.machete.assertLocalAndRemoteBranchesAreEqual("hotfix/add-trigger")
    intelliJ.machete.assertWorkingTreeIsAtHead()
    val currentBranchName = intelliJ.machete.getCurrentBranchName()
    Assert.assertEquals("develop", currentBranchName)
  }

  private def deleteMacheteFile(): Unit = {
    repositoryGitDir.resolve("machete").delete()
  }

  private def overwriteMacheteFile(content: String): Unit = {
    repositoryGitDir.resolve("machete").write(content + "\n")
  }

  private def setLastModifiedDateOfMacheteFileToEpochStart(): Unit = {
    val path = repositoryGitDir.resolve("machete")
    Files.setLastModifiedTime(path, FileTime.fromMillis(0))
  }
}
