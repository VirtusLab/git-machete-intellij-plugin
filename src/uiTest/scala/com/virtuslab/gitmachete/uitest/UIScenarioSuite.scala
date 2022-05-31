package com.virtuslab.gitmachete.uitest

import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedIntegrationTestSuite
import com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_README_SCENARIOS
import org.junit._
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.virtuslab.ideprobe.ProbeDriver

@RunWith(classOf[JUnit4])
class UIScenarioSuite extends BaseGitRepositoryBackedIntegrationTestSuite(SETUP_README_SCENARIOS) {

  import UISuite._
  UISuite.setup()

  private val project = intelliJ.project

  private val probe: ProbeDriver = intelliJ.probe

  @Before
  def beforeEach(): Unit = {
    probe.openProject(rootDirectoryPath)
    project.configure()
    probe.await()
    project.openGitMacheteTab()
  }

  @After
  def afterEach(): Unit = {
    probe.await()
    // Note that we shouldn't wait for a response here (so we shouldn't use org.virtuslab.ideprobe.ProbeDriver#closeProject),
    // since the response sometimes never comes (due to the project being closed), depending on the specific timing.
    intelliJ.ide.closeOpenedProjects()
  }

  @Test def scenario_1(): Unit = {
    project.switchRepo(0)
    project.toolbar.toggleListingCommits()
    project.contextMenu.openContextMenu("master")
    project.contextMenu.slideIn()
    project.writeToTextField("common-scripts")
    project.acceptSlideIn()
    project.acceptCreateNewBranch()
    project.contextMenu.openContextMenu("fancy-footer")
    project.contextMenu.checkout()
    project.contextMenu.openContextMenu("common-scripts")
    project.contextMenu.slideOut()
  }

  @Test def scenario_2(): Unit = {
    project.switchRepo(1)
    project.toolbar.toggleListingCommits()
    project.toolbar.fetchAll()
    project.contextMenu.openContextMenu("master")
    project.contextMenu.pull()
    project.contextMenu.openContextMenu("sticky-header")
    project.contextMenu.checkoutAndSyncByRebase()
    project.acceptRebase()
    project.contextMenu.openContextMenu("fancy-footer")
    project.contextMenu.checkoutAndSyncByRebase()
    project.acceptRebase()
    project.contextMenu.openContextMenu("sticky-header")
    project.contextMenu.push()
    project.acceptForcePush()
    project.contextMenu.openContextMenu("fancy-footer")
    project.contextMenu.push()
    project.acceptForcePush()
  }

  @Test def scenario_3(): Unit = {
    project.switchRepo(2)
    project.toolbar.toggleListingCommits()
    project.contextMenu.openContextMenu("fancy-footer")
    project.contextMenu.checkoutAndSyncByRebase() // consider using merge in this example
    project.acceptRebase()
    project.contextMenu.openContextMenu("sticky-header")
    project.contextMenu.push()
    project.acceptPush()
    project.contextMenu.openContextMenu("fancy-footer")
    project.contextMenu.push()
    project.acceptForcePush()
  }

  @Test def scenario_4(): Unit = {
    project.switchRepo(3)
    project.toolbar.toggleListingCommits()
    project.contextMenu.openContextMenu("sticky-header")
    project.contextMenu.fastForwardMerge()
    project.contextMenu.openContextMenu("master")
    project.contextMenu.push()
    project.acceptPush()
    project.contextMenu.openContextMenu("sticky-header")
    project.contextMenu.slideOut()
  }

}
