package com.virtuslab.gitmachete.uitest

import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedIntegrationTestSuite
import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedIntegrationTestSuite.SETUP_WITH_SINGLE_REMOTE
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe._
import org.virtuslab.ideprobe.config._
import org.virtuslab.ideprobe.dependencies._
import org.virtuslab.ideprobe.ide.intellij.IntelliJFactory
import org.virtuslab.ideprobe.junit4.RunningIntelliJPerSuite

object UITestSuite
  extends BaseGitRepositoryBackedIntegrationTestSuite(SETUP_WITH_SINGLE_REMOTE)
    with RunningIntelliJPerSuite
    with IdeProbeFixture
    with GitMacheteExtension {

  override protected def baseFixture: IntelliJFixture = transformFixture {
    val version = sys.props.get("intellij.version").filterNot(_.isEmpty).map(IntelliJVersion(_)).getOrElse(IntelliJVersion.Latest)
    val driverConfig = DriverConfig(
      vmOptions = Seq("-Xmx1G"),
      check = CheckConfig(errors = true)
    )
    IntelliJFixture(
      workspaceProvider = ExistingWorkspace(repositoryMainDir),
      version = version,
      factory = IntelliJFactory.Default.withConfig(driverConfig)
    )
  }

  def deleteMacheteFile(): Unit = {
    repositoryGitDir.resolve("machete").delete()
  }

  def overwriteMacheteFile(content: String): Unit = {
    repositoryGitDir.resolve("machete").write(content)
  }


}

@RunWith(classOf[JUnit4])
class UITestSuite {

  import UITestSuite._

  @Before
  def beforeEach(): Unit = {
    intelliJ.probe.openProject(intelliJ.workspace)
    intelliJ.machete.runJs("project.openTab()")
  }

  @After
  def afterEach(): Unit = {
    intelliJ.probe.closeProject()
    intelliJ.probe.awaitIdle()
  }

  @Test def skipNonExistentBranchesAndToggleListingCommits(): Unit = {
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
    // There should be exactly 6 rows in the graph table, since there are 6 existing branches in machete file;
    // non-existent branches should be skipped while causing no error (only a low-severity notification).
    Assert.assertEquals(6, branchRowsCount)
    intelliJ.machete.runJs("project.toggleListingCommits()")
    val branchAndCommitRowsCount = intelliJ.machete.refreshModelAndGetRowCount()
    // 6 branch rows + 7 commit rows
    Assert.assertEquals(13, branchAndCommitRowsCount)
  }

  @Test def discoverBranchLayout(): Unit = {
    deleteMacheteFile()
    // When model is refreshed and machete file is empty, then autodiscover should occur
    var branchRowsCount = intelliJ.machete.refreshModelAndGetRowCount()
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
    intelliJ.machete.checkoutBranch("master")
    // `master` is the parent of `hotfix/add-trigger`. Let's fast-forward `master` to match `hotfix/add-trigger`.
    intelliJ.machete.fastForwardParentToMatchBranch("hotfix/add-trigger")
    intelliJ.machete.assertBranchesAreEqual("master", "hotfix/add-trigger")
    intelliJ.machete.assertWorkingTreeIsAtHead()
  }

  @Test def fastForwardParentOfBranch_childIsCurrentBranch(): Unit = {
    intelliJ.machete.checkoutBranch("hotfix/add-trigger")
    intelliJ.machete.fastForwardParentToMatchCurrentBranch()
    intelliJ.machete.assertBranchesAreEqual("master", "hotfix/add-trigger")
    intelliJ.machete.assertWorkingTreeIsAtHead()
  }

  @Test def pullCurrentBranch(): Unit = {
    // Remote tracking data is purposefully NOT set for this branch.
    // Our plugin should infer the remote tracking branch based on its name.
    intelliJ.machete.checkoutBranch("allow-ownership-link")
    intelliJ.machete.pullCurrentBranch()
    intelliJ.machete.assertLocalAndRemoteBranchesAreEqual("allow-ownership-link")
    intelliJ.machete.assertWorkingTreeIsAtHead()
  }

  @Test def pullNonCurrentBranch(): Unit = {
    intelliJ.machete.checkoutBranch("develop")
    intelliJ.machete.pullBranch("allow-ownership-link")
    intelliJ.machete.assertLocalAndRemoteBranchesAreEqual("allow-ownership-link")
    intelliJ.machete.assertWorkingTreeIsAtHead()
  }

  @Test def resetCurrentBranchToRemote(): Unit = {
    intelliJ.machete.checkoutBranch("hotfix/add-trigger")
    intelliJ.machete.resetCurrentBranchToRemote()
    intelliJ.machete.assertLocalAndRemoteBranchesAreEqual("hotfix/add-trigger")
    intelliJ.machete.assertWorkingTreeIsAtHead()
    val currentBranchName = intelliJ.machete.getCurrentBranchName()
    Assert.assertEquals("hotfix/add-trigger", currentBranchName)
  }

  @Test def resetNonCurrentBranchToRemote(): Unit = {
    intelliJ.machete.checkoutBranch("develop")
    intelliJ.machete.resetBranchToRemote("hotfix/add-trigger")
    intelliJ.machete.assertLocalAndRemoteBranchesAreEqual("hotfix/add-trigger")
    intelliJ.machete.assertWorkingTreeIsAtHead()
    val currentBranchName = intelliJ.machete.getCurrentBranchName()
    Assert.assertEquals("develop", currentBranchName)
  }

}
