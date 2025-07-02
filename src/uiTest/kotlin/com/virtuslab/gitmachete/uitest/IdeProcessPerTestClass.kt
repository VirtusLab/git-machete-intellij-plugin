package com.virtuslab.gitmachete.uitest

import com.intellij.driver.client.Driver
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.ide.starter.project.NoProject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach

abstract class IdeProcessPerTestClass : BaseUITestSuite() {

  companion object {
    lateinit var backgroundRun: BackgroundRun

    @JvmStatic
    @BeforeAll
    fun startSharedIde() {
      backgroundRun = startIde(NoProject)
    }

    @JvmStatic
    @AfterAll
    fun closeSharedIde() {
      backgroundRun.closeIdeAndWait()
    }
  }

  override fun driver(): Driver = backgroundRun.driver

  @BeforeEach
  fun initProjectForTest() {
    println("Opening new project at $rootDirectoryPath...")
    retryOnConnectException(3) {
      robot.runJs(
        "global.get('openProject')('$rootDirectoryPath')",
        runInEdt = false,
      )
    }

    driver().waitForProject()
  }
}
