package com.virtuslab.gitmachete.uitest

import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedIntegrationTestSuite
import com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_README_SCENARIOS
import org.junit._
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(classOf[JUnit4])
class UIScenarioSuite extends BaseGitRepositoryBackedIntegrationTestSuite(SETUP_README_SCENARIOS) {

  import UISuite._
  UISuite.setup()

  @Before
  def beforeEach(): Unit = {
    intelliJ.probe.openProject(rootDirectoryPath)
    intelliJ.project.configure()
    intelliJ.probe.await()
    intelliJ.project.openGitMacheteTab()
    intelliJ.project.toggleListingCommits()
  }

  @After
  def afterEach(): Unit = {
    intelliJ.probe.await()
    // Note that we shouldn't wait for a response here (so we shouldn't use org.virtuslab.ideprobe.ProbeDriver#closeProject),
    // since the response sometimes never comes (due to the project being closed), depending on the specific timing.
    intelliJ.ide.closeOpenedProjects()
  }

  @Test def scenario_1(): Unit = {
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
    // todo: switch repo (in the other scenarios too!)
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
