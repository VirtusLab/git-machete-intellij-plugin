package com.virtuslab.gitmachete.uitest

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.NoProject
import com.intellij.ide.starter.runner.Starter
import org.junit.jupiter.api.*
import java.io.File
import java.nio.file.Path
import kotlin.time.Duration.Companion.minutes

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class SharedIdeTestSuite : BaseUITestSuite() {

  companion object {
    lateinit var backgroundRun: BackgroundRun

    @JvmStatic
    @BeforeAll
    fun startSharedIde() {
      val ideStarter = Starter.newContext(
        testName = "SharedTestSuite",
        testCase = TestCase(IdeProductProvider.IC, projectInfo = NoProject).withVersion("2024.3"),
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

  private fun Driver.openProject(projectPath: Path) {
    retryOnConnectException(3) {
      robot.runJs(
        """
        importClass(com.intellij.openapi.project.ex.ProjectManagerEx);
        const projectManager = ProjectManagerEx.getInstanceEx();
        const currentProject = projectManager.getOpenProjects()[0];
        if (currentProject) {
            ProjectManagerEx.getInstance().closeAndDispose(currentProject);
        }
    """,
        runInEdt = true,
      )
    }

    retryOnConnectException(3) {
      robot.runJs(
        """
        importClass(java.nio.file.Paths);
        importClass(com.intellij.ide.impl.OpenProjectTask);
        importClass(com.intellij.ide.impl.ProjectUtil);
        importClass(com.intellij.ide.impl.TrustedPathsSettings);
        importClass(com.intellij.openapi.components.ServiceManager);
        importClass(com.intellij.openapi.project.ex.ProjectManagerEx);

        const trustedPathsSettings = ServiceManager.getService(TrustedPathsSettings);
        trustedPathsSettings.addTrustedPath("$projectPath");

        const projectManager = ProjectManagerEx.getInstanceEx();
        const newProject = projectManager.openProject(
            Paths.get("$projectPath"),
            OpenProjectTask.build()
        );
        ProjectUtil.focusProjectWindow(newProject, true);
    """,
        runInEdt = true,
      )
    }

    waitForProject(3)
  }

  @BeforeEach
  fun initProjectForTest() {
    val driver = backgroundRun.driver
    driver.openProject(rootDirectoryPath)
    println("New project opened: $rootDirectoryPath")

    println("Waiting for project to open...")
    driver.waitForProject(3)

    println("Waiting for indicators...")
    driver.waitForIndicators(1.minutes)
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
