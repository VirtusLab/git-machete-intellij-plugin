package com.virtuslab.gitmachete.uitest

import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedIntegrationTestSuite
import com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_README_SCENARIOS
import org.junit._
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.virtuslab.ideprobe._
import org.virtuslab.ideprobe.dependencies.IntelliJVersion

object UIScenarioSuite
  extends RunningIntelliJPerSuite
    with IdeProbeFixture
    with RunningIntelliJFixtureExtension {

  lazy val intelliJVersion: IntelliJVersion = {
    val version = sys.props.get("ui-test.intellij.version")
      .filterNot(_.isEmpty).getOrElse(throw new Exception("IntelliJ version is not provided"))
    // We're cheating here a bit since `version` might be either a build number or a release number,
    // while we're always treating it as a build number.
    // Still, as of ide-probe 0.26.0, even when release number like `2020.3` is passed as `build`, UI tests work just fine.
    IntelliJVersion(build = version, release = None)
  }

  override protected def baseFixture: IntelliJFixture = {
    // By default, the config is taken from <class-name>.conf resource, see org.virtuslab.ideprobe.IdeProbeFixture.resolveConfig
    fixtureFromConfig().withVersion(intelliJVersion)
  }

}

@RunWith(classOf[JUnit4])
class UIScenarioSuite extends BaseGitRepositoryBackedIntegrationTestSuite(SETUP_README_SCENARIOS) {

  import UIScenarioSuite._

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

  @Test def scenario_1(): Unit = {
    intelliJ.project.openGitMacheteTab()
    intelliJ.project.toggleListingCommits()
    var branchAndCommitRowsCount = intelliJ.project.refreshModelAndGetRowCount()
    intelliJ.project.contextMenu.openContextMenu("master")
    intelliJ.project.contextMenu.slideIn()
    intelliJ.project.writeToTextField("common-scripts")
    intelliJ.project.acceptSlideIn()
    intelliJ.project.acceptCreateNewBranch()
    intelliJ.project.contextMenu.openContextMenu("fancy-footer")
    intelliJ.project.contextMenu.checkout()
    intelliJ.project.contextMenu.openContextMenu("common-scripts")
    intelliJ.project.contextMenu.slideOut()
    // consider setting up the git config to
    intelliJ.project.acceptBranchDeletionOnSlideOut()
  }

  // todo: merge scenarios? or select from combobox?
  @Test def scenario_2(): Unit = {
    intelliJ.project.toolbar.fetchAll()
    intelliJ.project.contextMenu.openContextMenu("master")
    intelliJ.project.contextMenu.pull()
    intelliJ.project.contextMenu.openContextMenu("sticky-header")
    intelliJ.project.contextMenu.checkoutAndSyncByRebase()
    intelliJ.project.contextMenu.openContextMenu("fancy-footer")
    intelliJ.project.contextMenu.checkoutAndSyncByRebase()
    intelliJ.project.contextMenu.openContextMenu("sticky-header")
    intelliJ.project.contextMenu.push()
    intelliJ.project.contextMenu.openContextMenu("fancy-footer")
    intelliJ.project.contextMenu.push()
  }

  @Test def scenario_3(): Unit = {
    intelliJ.project.contextMenu.openContextMenu("fancy-footer")
    intelliJ.project.contextMenu.syncByRebase() // todo: consider using merge in this example
    intelliJ.project.contextMenu.openContextMenu("sticky-header")
    intelliJ.project.contextMenu.push()
    intelliJ.project.contextMenu.openContextMenu("fancy-footer")
    intelliJ.project.contextMenu.push()
  }

  @Test def scenario_4(): Unit = {
    intelliJ.project.contextMenu.openContextMenu("sticky-header")
    intelliJ.project.contextMenu.fastForwardMerge()
    intelliJ.project.contextMenu.openContextMenu("master")
    intelliJ.project.contextMenu.push()
    intelliJ.project.contextMenu.openContextMenu("sticky-header")
    intelliJ.project.contextMenu.slideOut()
  }

}
