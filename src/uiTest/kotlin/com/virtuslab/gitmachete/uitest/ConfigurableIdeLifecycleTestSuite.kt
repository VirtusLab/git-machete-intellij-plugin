package com.virtuslab.gitmachete.uitest

import com.intellij.driver.client.Driver
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.ide.starter.project.NoProject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach

abstract class ConfigurableIdeLifecycleTestSuite : BaseUITestSuite() {

  companion object {
    private val ideInstancePer = System.getProperty("ide.instance-per", "class")
    private var sharedBackgroundRun: BackgroundRun? = null

    @JvmStatic
    @BeforeAll
    fun startSharedIdeIfNeeded() {
      if (System.getProperty("os.name") == "Mac OS X") {
        println("WARN: On macOS, make sure that the terminal (e.g. iTerm) where you run the tests has the permissions to move the cursor:")
        println("System Settings > Privacy & Security > Accessibility > (+) > iTerm")
      }

      if (ideInstancePer == "class") {
        println("Starting shared IDE for all tests (class mode)...")
        sharedBackgroundRun = startIde(NoProject)
      }
    }

    @JvmStatic
    @AfterAll
    fun closeSharedIdeIfNeeded() {
      if (ideInstancePer == "class") {
        println("Closing shared IDE (class mode)...")
        sharedBackgroundRun?.closeIdeAndWait()
      }
    }
  }

  private var methodBackgroundRun: BackgroundRun? = null

  override fun driver(): Driver = when (ideInstancePer) {
    "method" -> (methodBackgroundRun ?: throw IllegalStateException("IDE not started for this test method")).driver
    else -> (sharedBackgroundRun ?: throw IllegalStateException("Shared IDE not started")).driver
  }

  @BeforeEach
  fun setupTestMethod() {
    when (ideInstancePer) {
      "method" -> {
        println("Starting IDE for test method (method mode)...")
        methodBackgroundRun = startIde(LocalProjectInfo(rootDirectoryPath))
        driver().waitForProject()
      }

      else -> {
        println("Opening new project at $rootDirectoryPath (class mode)...")
        retryOnConnectException(3) {
          robot.runJs(
            "global.get('openProject')('$rootDirectoryPath')",
            runInEdt = false,
          )
        }
        driver().waitForProject()
      }
    }
  }

  @AfterEach
  fun teardownTestMethod() {
    if (ideInstancePer == "method") {
      println("Closing IDE for test method (method mode)...")
      methodBackgroundRun?.closeIdeAndWait()
      methodBackgroundRun = null
    }
  }
}
