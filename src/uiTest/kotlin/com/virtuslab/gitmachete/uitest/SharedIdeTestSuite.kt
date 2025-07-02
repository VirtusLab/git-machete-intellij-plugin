package com.virtuslab.gitmachete.uitest

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.isProjectOpened
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.NoProject
import com.intellij.ide.starter.runner.Starter
import com.intellij.remoterobot.RemoteRobot
import com.virtuslab.gitmachete.testcommon.SetupScripts
import com.virtuslab.gitmachete.testcommon.TestGitRepository
import org.junit.jupiter.api.*
import java.io.File
import java.nio.file.Path
import kotlin.time.Duration.Companion.minutes

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class SharedIdeTestSuite : TestGitRepository(SetupScripts.SETUP_WITH_SINGLE_REMOTE) {

  companion object {
    lateinit var backgroundRun: BackgroundRun

    val intelliJVersion = System.getProperty("intellij.version")

    private fun testCase(): TestCase<NoProject> {
      val testCase = TestCase(IdeProductProvider.IC, projectInfo = NoProject)
      return if (intelliJVersion.matches("20[0-9][0-9]\\.[0-9].*".toRegex())) {
        testCase.withVersion(intelliJVersion)
      } else {
        testCase.withBuildNumber(intelliJVersion)
      }
    }

    @JvmStatic
    @BeforeAll
    fun startSharedIde() {
      val ideStarter = Starter.newContext(
        testName = "SharedTestSuite",
        testCase = testCase(),
      ).skipIndicesInitialization().apply {
        val pathToBuildPlugin = System.getProperty("path.to.build.plugin")
        val pathToRobotServerPlugin = System.getProperty("path.to.robot.server.plugin")
        PluginConfigurator(this)
          .installPluginFromPath(File(pathToBuildPlugin).toPath())
          .installPluginFromPath(File(pathToRobotServerPlugin).toPath())
      }
      backgroundRun = ideStarter.runIdeWithDriver { }
      println("Shared IDE instance started")
    }

    @JvmStatic
    @AfterAll
    fun closeSharedIde() {
      backgroundRun.closeIdeAndWait()
      println("Shared IDE instance closed")
    }
  }

  fun <T> retryOnConnectException(attempts: Int, block: () -> T): T = try {
    block()
  } catch (e: java.net.ConnectException) {
    if (attempts > 1) {
      println("Retrying due to ${e.message}...")
      Thread.sleep(3000)
      retryOnConnectException(attempts - 1, block)
    } else {
      throw RuntimeException("Retries failed", e)
    }
  }

  val robot = RemoteRobot("http://127.0.0.1:8580")

  fun Driver.waitForProject(attempts: Int) {
    var attemptsLeft = attempts
    while (!isProjectOpened() && attemptsLeft > 0) {
      Thread.sleep(3000)
      attemptsLeft--
    }
    if (!isProjectOpened()) {
      throw IllegalStateException("Project has still not been opened, aborting")
    }
  }

  private fun Driver.openProject(projectPath: Path) {
    retryOnConnectException(3) {
      robot.runJs(
        """
        importClass(java.lang.Runnable);
        importClass(com.intellij.openapi.application.ApplicationManager);
        importClass(com.intellij.openapi.application.ReadAction);
        importClass(com.intellij.openapi.project.ex.ProjectManagerEx);

        const projectManager = ProjectManagerEx.getInstanceEx();
        const currentProject = projectManager.getOpenProjects()[0];
        if (currentProject) {
          ApplicationManager.getApplication().invokeAndWait(() => projectManager.closeAndDispose(currentProject));
        }
        """,
        runInEdt = false,
      )
    }

    retryOnConnectException(3) {
      robot.runJs(
        """
        importClass(java.nio.file.Paths);
        importClass(com.intellij.ide.impl.OpenProjectTask);
        importClass(com.intellij.ide.impl.ProjectUtil);
        importClass(com.intellij.ide.impl.TrustedPathsSettings);
        importClass(com.intellij.openapi.application.ApplicationManager);
        importClass(com.intellij.openapi.components.ServiceManager);
        importClass(com.intellij.openapi.project.ex.ProjectManagerEx);

        const trustedPathsSettings = ServiceManager.getService(TrustedPathsSettings);
        trustedPathsSettings.addTrustedPath("$projectPath");

        const projectManager = ProjectManagerEx.getInstanceEx();
        ApplicationManager.getApplication().invokeAndWait(() => {
          const newProject = projectManager.openProject(Paths.get("$projectPath"), OpenProjectTask.build());
          ProjectUtil.focusProjectWindow(newProject, true);
        });
        """,
        runInEdt = false,
      )
    }
    println("New project opened: $projectPath")

    waitForProject(3)
    waitForIndicators(1.minutes)
  }

  @BeforeEach
  fun initProjectForTest() {
    backgroundRun.driver.openProject(rootDirectoryPath)
  }
}

class MyUITests : SharedIdeTestSuite() {
  @Test
  fun testFeatureA() {
  }

  @Test
  fun testFeatureB() {
  }
}
