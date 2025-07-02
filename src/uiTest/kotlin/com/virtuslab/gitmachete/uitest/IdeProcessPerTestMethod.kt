package com.virtuslab.gitmachete.uitest

import com.intellij.driver.client.Driver
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.ide.starter.project.LocalProjectInfo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class IdeProcessPerTestMethod : BaseUITestSuite() {

  private lateinit var backgroundRun: BackgroundRun
  override fun driver(): Driver = backgroundRun.driver

  @BeforeEach
  fun setup() {
    backgroundRun = startIde(LocalProjectInfo(rootDirectoryPath))
    driver().waitForProject()
  }

  @AfterEach
  fun teardown() {
    backgroundRun.closeIdeAndWait()
  }
}
