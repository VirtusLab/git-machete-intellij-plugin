package com.virtuslab.gitmachete.uitest

import java.nio.file.Files
import java.nio.file.attribute.FileTime
import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedIntegrationTestSuite
import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedIntegrationTestSuite.SETUP_WITH_SINGLE_REMOTE
import org.junit.{ After, AfterClass, Assert, Before, BeforeClass, Test }
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe._
import org.virtuslab.ideprobe.config._
import org.virtuslab.ideprobe.dependencies._
import org.virtuslab.ideprobe.ide.intellij.IntelliJFactory

trait RunningIntelliJPerSuite extends RunningIntelliJPerSuiteBase {
  @BeforeClass override final def setup(): Unit = super.setup()

  private val ignoredErrorMessages = Seq(
    // Spurious errors in the IDEA itself (probably some race conditions)
    "com.intellij.diagnostic.PluginException: Cannot create class com.intellij.uast.UastMetaLanguage",
    "com.intellij.serviceContainer.AlreadyDisposedException: Already disposed: Project",
    // https://github.com/VirtusLab/ide-probe/issues/95
    "scala.MatchError: IDE_UPDATE (of class com.intellij.notification.NotificationType)"
  )

  @AfterClass override final def teardown(): Unit = {
    try {
      super.teardown()
    } catch {
      case e: Exception =>
        val filteredSuppressed = e.getSuppressed.filterNot { s =>
          ignoredErrorMessages.exists(s.getMessage.contains)
        }
        if (filteredSuppressed.nonEmpty) {
          // Following the approach taken by org.virtuslab.ideprobe.reporting.AfterTestChecks.apply
          val e = new Exception("Test failed due to postcondition failures")
          filteredSuppressed.foreach(e.addSuppressed)
          throw e
        }
    }
  }
}

object UITestSuite
  extends RunningIntelliJPerSuite
    with IdeProbeFixture
    with RunningIntelliJFixtureExtension {

  lazy val intelliJVersion: IntelliJVersion = {
    val version = sys.props.get("intellij.version")
      .filterNot(_.isEmpty).getOrElse(throw new Exception("IntelliJ version is not provided"))
    // We're cheating here a bit since `version` might be either a build number or a release number,
    // while we're always treating it as a build number.
    // Still, as of ide-probe 0.3.0, even when release number like `2020.3` is passed as `build`, UI tests work just fine.
    IntelliJVersion(build = version, release = None)
  }

  override protected def baseFixture: IntelliJFixture = {
    val config = Config.fromString(
      """
        |probe {
        |  driver {
        |    vmOptions = ["-Xmx1G"]
        |    check {
        |      errors = true
        |    }
        |  }
        |
        |  waitLogic.default {
        |    type = "EmptyNamedBackgroundTasks"
        |    basicCheckFrequency = "1s"
        |    ensurePeriod = "1s"
        |    ensureFrequency = "250ms"
        |    atMost = "2 minutes"
        |  }
        |}
        |""".stripMargin)

    fixtureFromConfig(config).withVersion(intelliJVersion)
  }

}

@RunWith(classOf[JUnit4])
class UITestSuite extends BaseGitRepositoryBackedIntegrationTestSuite(SETUP_WITH_SINGLE_REMOTE) {

  import UITestSuite._

  @Before
  def beforeEach(): Unit = {
    intelliJ.probe.openProject(repositoryMainDir)
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
    val branchRowsCount = intelliJ.project.refreshModelAndGetRowCount()
    // There should be exactly 6 rows in the graph table, since there are 6 existing branches in machete file
    // non-existent branches should be skipped while causing no error (only a low-severity notification).
    Assert.assertEquals(6, branchRowsCount)
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

  @Test def fastForwardParentOfBranch_parentIsCurrentBranch(): Unit = {
    intelliJ.project.openGitMacheteTab()
    intelliJ.project.checkoutBranch("master")
    // `master` is the parent of `hotfix/add-trigger`. Let's fast-forward `master` to match `hotfix/add-trigger`.
    intelliJ.project.fastForwardMergeSelectedBranchToParent("hotfix/add-trigger")
    intelliJ.project.assertBranchesAreEqual("master", "hotfix/add-trigger")
    intelliJ.project.assertWorkingTreeIsAtHead()
  }

  @Test def fastForwardParentOfBranch_childIsCurrentBranch(): Unit = {
    intelliJ.project.openGitMacheteTab()
    intelliJ.project.checkoutBranch("hotfix/add-trigger")
    intelliJ.project.fastForwardMergeCurrentBranchToParent()
    intelliJ.project.assertBranchesAreEqual("master", "hotfix/add-trigger")
    intelliJ.project.assertWorkingTreeIsAtHead()
  }

  @Test def pullCurrentBranch(): Unit = {
    intelliJ.project.openGitMacheteTab()
    // Remote tracking data is purposefully NOT set for this branch.
    // Our plugin should infer the remote tracking branch based on its name.
    intelliJ.project.checkoutBranch("allow-ownership-link")
    intelliJ.project.pullCurrentBranch()
    intelliJ.project.assertLocalAndRemoteBranchesAreEqual("allow-ownership-link")
    intelliJ.project.assertWorkingTreeIsAtHead()
  }

  @Test def pullNonCurrentBranch(): Unit = {
    intelliJ.project.openGitMacheteTab()
    intelliJ.project.checkoutBranch("develop")
    intelliJ.project.pullBranch("allow-ownership-link")
    intelliJ.project.assertLocalAndRemoteBranchesAreEqual("allow-ownership-link")
    intelliJ.project.assertWorkingTreeIsAtHead()
  }

  @Test def resetCurrentBranchToRemote(): Unit = {
    intelliJ.project.openGitMacheteTab()
    intelliJ.project.checkoutBranch("hotfix/add-trigger")
    intelliJ.project.resetCurrentBranchToRemote()
    intelliJ.project.assertLocalAndRemoteBranchesAreEqual("hotfix/add-trigger")
    intelliJ.project.assertWorkingTreeIsAtHead()
    val currentBranchName = intelliJ.project.getCurrentBranchName()
    Assert.assertEquals("hotfix/add-trigger", currentBranchName)
  }

  @Test def resetNonCurrentBranchToRemote(): Unit = {
    intelliJ.project.openGitMacheteTab()
    intelliJ.project.checkoutBranch("develop")
    intelliJ.project.resetBranchToRemote("hotfix/add-trigger")
    intelliJ.project.assertLocalAndRemoteBranchesAreEqual("hotfix/add-trigger")
    intelliJ.project.assertWorkingTreeIsAtHead()
    val currentBranchName = intelliJ.project.getCurrentBranchName()
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
