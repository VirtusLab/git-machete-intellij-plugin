package com.virtuslab.gitmachete.uitest

import com.virtuslab.gitmachete.testcommon.GitRepositoryBackedIntegrationTestSuiteInitializer
import com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_README_SCENARIOS
import org.junit._
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.virtuslab.ideprobe.ProbeDriver

object UIScenarioSuite extends UISuite {}

@RunWith(classOf[JUnit4])
class UIScenarioSuite extends GitRepositoryBackedIntegrationTestSuiteInitializer(SETUP_README_SCENARIOS) {
  import UIScenarioSuite._

  private val project = intelliJ.project

  private val probe: ProbeDriver = intelliJ.probe

  @Before
  def beforeEach(): Unit = {
    probe.openProject(rootDirectoryPath)
    project.configure()
    probe.await()
    project.usePrettyClick()
    project.openGitMacheteTab()
    project.toolbar.toggleListingCommits()
    intelliJ.ide.findAndResizeIdeFrame()
  }

  @After
  def afterEach(): Unit = {
    waitAndCloseProject()
  }

  @Ignore
  @Test def scenarios(): Unit = {
    // scenario 1
    project.switchRepo(0)
    project.moveMouseToTheMiddleAndWait(15)
    project.contextMenu.openContextMenu("master")
    project.contextMenu.slideIn()
    project.writeToTextField("common-scripts")
    project.acceptSlideIn()
    project.acceptCreateNewBranch()
    project.moveMouseToTheMiddleAndWait(0)
    project.contextMenu.openContextMenu("fancy-footer")
    project.contextMenu.checkout()
    project.contextMenu.openContextMenu("common-scripts")
    project.contextMenu.slideOut()
    project.moveMouseToTheMiddleAndWait(15)

    // scenario 2
    project.switchRepo(1)
    project.moveMouseToTheMiddleAndWait(0)
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
    project.moveMouseToTheMiddleAndWait(15)

    // scenario 3
    project.switchRepo(2)
    project.moveMouseToTheMiddleAndWait(0)
    project.contextMenu.openContextMenu("fancy-footer")
    project.contextMenu.checkoutAndSyncByRebase() // consider using merge in this example
    project.acceptRebase()
    project.contextMenu.openContextMenu("sticky-header")
    project.contextMenu.push()
    project.acceptPush()
    project.contextMenu.openContextMenu("fancy-footer")
    project.contextMenu.push()
    project.acceptForcePush()
    project.moveMouseToTheMiddleAndWait(15)

    // scenario 4
    project.switchRepo(3)
    project.moveMouseToTheMiddleAndWait(0)
    project.contextMenu.openContextMenu("sticky-header")
    project.contextMenu.fastForwardMerge()
    project.contextMenu.openContextMenu("master")
    project.contextMenu.push()
    project.acceptPush()
    project.contextMenu.openContextMenu("sticky-header")
    project.contextMenu.slideOut()
    project.moveMouseToTheMiddleAndWait(15)
  }

}
