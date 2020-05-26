package com.virtuslab.gitmachete.uitest

import java.io.File

import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedTest
import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedTest.SETUP_WITH_SINGLE_REMOTE
import com.virtuslab.ideprobe.dependencies._
import com.virtuslab.ideprobe.ide.intellij.{ DriverConfig, IntelliJFactory }
import com.virtuslab.ideprobe.protocol.IdeMessage.Level.Info
import com.virtuslab.ideprobe.{ IntegrationTestSuite, IntelliJFixture }
import org.junit.{ Assert, Test }

class OpenTabActionUiTest extends BaseGitRepositoryBackedTest with IntegrationTestSuite {

  implicit class ProjectDirOps(projectDir: File) {

    /** @return the latest file in <projectDir>/build/distributions */
    def pluginFile: File = projectDir.toPath
      .resolve("build").resolve("distributions").toFile
      .listFiles()
      .maxBy(_.lastModified)
  }

  // `user.dir` property resolves to the current subproject path in tests executed by Gradle `test` task.
  val currentSubprojectDir: File = new File(System.getProperty("user.dir"))
  val uiTestActionsPlugin: File = currentSubprojectDir.pluginFile
  val rootProjectDir: File = currentSubprojectDir.getParentFile
  val mainPlugin: File = rootProjectDir.pluginFile

  private def testForIntelliJVersion(version: String): Unit = {
    init(SETUP_WITH_SINGLE_REMOTE)

    // Apparently, testing our plugin in headless mode does NOT make sense:
    // `com.intellij.openapi.wm.ToolWindowManager.getToolWindow` always returns null in headless mode,
    // which for us means that we can't get the access to the VCS Tool Window, let alone Git Machete tab.
    // Hence, `.withDisplay()` is necessary.
    IntelliJFixture(
      version = IntelliJVersion(version),
      intelliJFactory = IntelliJFactory.Default.withConfig(DriverConfig(vmOptions = Seq("-Xmx1G"))),
      plugins = Seq(mainPlugin, uiTestActionsPlugin).map(p => Plugin.Direct(p.toURI))
    ).withDisplay().run { ij =>
      ij.probe.openProject(repositoryMainDir)
      // The action we're launching here comes from `uiTestActionsPlugin`,
      // but it's in turn opening (and peeking into the contents of) the tab that is spawned/controlled by `mainPlugin`.
      ij.probe.invokeAction("GitMachete.UITest.OpenTabAction")
      ij.probe.awaitIdle()

      val infoMessages = ij.probe.messages.filter(_.level == Info)
      Assert.assertTrue(infoMessages.exists(_.content.endsWith("Opened Git Machete tab: Git Machete")))
      // There should be exactly 6 rows in the graph table, since there are 6 branches in machete file,
      // as set up via `init(SETUP_WITH_SINGLE_REMOTE)`.
      Assert.assertTrue(infoMessages.exists(_.content.endsWith("Row count: 6")))
      Assert.assertArrayEquals(ij.probe.errors.toArray[Object], Array.empty[Object])
    }
  }

  @Test def test_2019_3(): Unit = testForIntelliJVersion("193.7288.26")

  @Test def test_2020_1(): Unit = testForIntelliJVersion("201.6668.121")

}
