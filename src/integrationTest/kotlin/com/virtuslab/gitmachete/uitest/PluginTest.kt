package com.virtuslab.gitmachete.uitest

import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.ci.CIServer
import com.intellij.ide.starter.ci.NoCIServer
import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.ide.starter.runner.Starter
import com.intellij.remoterobot.RemoteRobot
import com.virtuslab.gitmachete.testcommon.SetupScripts
import com.virtuslab.gitmachete.testcommon.TestGitRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import java.io.File
import kotlin.time.Duration.Companion.minutes

class PluginTest : TestGitRepository(SetupScripts.SETUP_WITH_SINGLE_REMOTE) {
  init {
    di = DI {
      extend(di)
      bindSingleton<CIServer>(overrides = true) {
        object : CIServer by NoCIServer {
          override fun reportTestFailure(
            testName: String,
            message: String,
            details: String,
            linkToLogs: String?,
          ) {
            fail { "$testName fails: $message. \n$details" }
          }
        }
      }
    }
  }

  private fun loadScript(baseName: String) = {
    this.javaClass.getResource("/$baseName.rhino.js")!!.readText()
  }


  @Test
  fun simpleTestWithoutProject() {
    Starter.newContext(
      testName = "testExample",
      TestCase(IdeProductProvider.IC, projectInfo = LocalProjectInfo(rootDirectoryPath))
        .withVersion("2024.3"),
    ).skipIndicesInitialization().apply {
      val pathToPlugin = System.getProperty("path.to.build.plugin")
      println("**** Path: $pathToPlugin lolxd ****")
      PluginConfigurator(this)
        .installPluginFromPath(File(pathToPlugin).toPath())
        .installPluginFromURL("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies/com/intellij/remoterobot/robot-server-plugin/0.11.23/robot-server-plugin-0.11.23.zip")
    }.runIdeWithDriver().useDriverAndCloseIde {
      val robot = RemoteRobot("http://127.0.0.1:8580")
      println("*** project count = " + robot.callJs<Int>("com.intellij.ide.impl.ProjectUtil.getOpenProjects().length"))
      waitForIndicators(5.minutes)
    }
  }
}
