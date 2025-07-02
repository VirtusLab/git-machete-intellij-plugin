package com.virtuslab.gitmachete.uitest

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
import kotlin.time.Duration.Companion.minutes

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class IdeProcessPerTestClass : BaseUITestSuite() {

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

      println("rhino project initializing...")
      val rhinoProject = this::class.java.getResource("/project.rhino.js")!!.readText()
      retryOnConnectException(3) {
        robot.runJs(rhinoProject, runInEdt = false)
      }
      println("rhino project initialized")
    }

    @JvmStatic
    @AfterAll
    fun closeSharedIde() {
      backgroundRun.closeIdeAndWait()
      println("Shared IDE instance closed")
    }
  }

  @BeforeEach
  fun initProjectForTest() {
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
        trustedPathsSettings.addTrustedPath("$rootDirectoryPath");

        const projectManager = ProjectManagerEx.getInstanceEx();
        ApplicationManager.getApplication().invokeAndWait(() => {
          const newProject = projectManager.openProject(Paths.get("$rootDirectoryPath"), OpenProjectTask.build());
          ProjectUtil.focusProjectWindow(newProject, true);
        });
        """,
        runInEdt = false,
      )
    }
    println("New project opened: $rootDirectoryPath")

    backgroundRun.driver.waitForProject(3)
    backgroundRun.driver.waitForIndicators(1.minutes)
  }

  override fun doAndAwait(action: () -> Unit) {
    action()
    println("waiting for indicators...")
    backgroundRun.driver.waitForIndicators(1.minutes)
  }
}
