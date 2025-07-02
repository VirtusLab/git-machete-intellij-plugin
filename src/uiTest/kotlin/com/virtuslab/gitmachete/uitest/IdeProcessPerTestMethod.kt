package com.virtuslab.gitmachete.uitest

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.ci.CIServer
import com.intellij.ide.starter.ci.NoCIServer
import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.junit5.displayName
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.ide.starter.runner.CurrentTestMethod
import com.intellij.ide.starter.runner.Starter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.fail
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import java.io.File
import java.nio.file.attribute.PosixFilePermission.*
import kotlin.jvm.javaClass
import kotlin.time.Duration.Companion.minutes

abstract class IdeProcessPerTestMethod : BaseUITestSuite() {

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

  lateinit var backgroundRun: BackgroundRun
  lateinit var driver: Driver

  private fun testCase(): TestCase<LocalProjectInfo> {
    val testCase = TestCase(IdeProductProvider.IC, projectInfo = LocalProjectInfo(rootDirectoryPath))
    return if (intelliJVersion.matches("20[0-9][0-9]\\.[0-9].*".toRegex())) {
      testCase.withVersion(intelliJVersion)
    } else {
      testCase.withBuildNumber(intelliJVersion)
    }
  }

  @BeforeEach
  fun setup() {
    println("IntelliJ build number is $intelliJVersion")

    backgroundRun = Starter.newContext(
      testName = CurrentTestMethod.displayName(),
      testCase = testCase(),
    ).skipIndicesInitialization().apply {
      val pathToBuildPlugin = System.getProperty("path.to.build.plugin")
      val pathToRobotServerPlugin = System.getProperty("path.to.robot.server.plugin")
      PluginConfigurator(this)
        .installPluginFromPath(File(pathToBuildPlugin).toPath())
        .installPluginFromPath(File(pathToRobotServerPlugin).toPath())
    }.runIdeWithDriver { }

    driver = backgroundRun.driver
    println("waiting for project to open...")
    driver.waitForProject(3)

    println("rhino project initializing...")
    val rhinoProject = this.javaClass.getResource("/project.rhino.js")!!.readText()
    retryOnConnectException(3) {
      robot.runJs(rhinoProject, runInEdt = false)
    }
    println("rhino project initialized")

    println("waiting for indicators...")
    driver.waitForIndicators(1.minutes)
  }

  @AfterEach
  fun teardown() {
    backgroundRun.closeIdeAndWait()
  }

  override fun doAndAwait(action: () -> Unit) {
    action()
    println("waiting for indicators...")
    driver.waitForIndicators(1.minutes)
  }
}
