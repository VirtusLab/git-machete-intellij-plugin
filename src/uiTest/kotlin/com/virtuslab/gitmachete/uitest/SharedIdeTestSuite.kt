package com.virtuslab.gitmachete.uitest

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.NoProject
import com.intellij.ide.starter.runner.Starter
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.io.File
import java.nio.file.Path
import kotlin.time.Duration.Companion.minutes

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DriverResolver::class)
abstract class SharedIdeTestSuite : BaseUITestSuite() {

  companion object {
    private lateinit var ideStarter: IDETestContext
    lateinit var backgroundRun: BackgroundRun

    @JvmStatic
    @BeforeAll
    fun startSharedIde() {
      ideStarter = Starter.newContext(
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

  fun Driver.openProject(projectPath: Path) {
    // Close current project if exists
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

    // Open new project
    retryOnConnectException(3) {
      robot.runJs(
        """
        importClass(java.nio.file.Paths);
        importClass(com.intellij.ide.impl.OpenProjectTask);
        importClass(com.intellij.ide.impl.ProjectUtil);
        importClass(com.intellij.openapi.project.ex.ProjectManagerEx);
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

    // Wait for project initialization
    waitForProject(3)
  }

  @BeforeEach
  fun initProjectForTest(driver: Driver) {
    // Open new project in existing IDE instance
    driver.openProject(rootDirectoryPath)
    println("New project opened: $rootDirectoryPath")

    // Project initialization sequence
    println("Waiting for project to open...")
    driver.waitForProject(3)

    println("Waiting for indicators...")
    driver.waitForIndicators(1.minutes)
  }
}

// Resolves Driver instance for test methods
class DriverResolver : ParameterResolver {
  override fun supportsParameter(
    parameterContext: ParameterContext,
    extensionContext: ExtensionContext,
  ) = parameterContext.parameter.type == Driver::class.java

  override fun resolveParameter(
    parameterContext: ParameterContext,
    extensionContext: ExtensionContext,
  ) = SharedIdeTestSuite.backgroundRun.driver
}

// Example test suite implementation
class MyUITests : SharedIdeTestSuite() {
  @Test
  fun testFeatureA() {
  }
}
