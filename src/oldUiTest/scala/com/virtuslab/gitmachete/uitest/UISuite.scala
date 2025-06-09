package com.virtuslab.gitmachete.uitest

import org.junit.jupiter.api.{AfterAll, BeforeAll}
import org.virtuslab.ideprobe.{IdeProbeFixture, IntelliJFixture, RunningIntelliJPerSuiteBase}

trait UISuite extends RunningIntelliJPerSuiteBase with IdeProbeFixture with RunningIntelliJFixtureExtension {

  registerFixtureTransformer(_.withAfterIntelliJStartup((_, intelliJ) => intelliJ.ide.configure()))

  override protected def baseFixture: IntelliJFixture = {
    val osName = System.getProperty("os.name").toLowerCase
    val isMacOs = osName.startsWith("mac os x")
    val configFile = if (isMacOs) "ideprobe-macos.conf" else "ideprobe-linux.conf"
    fixtureFromConfig(configFile)
  }

  // Don't call it `beforeAll()`, such method already exists in `RunningIntelliJPerSuiteBase`
  @BeforeAll override final def setup(): Unit = super.setup()

  // Don't call it `afterAll()`, such method already exists in `RunningIntelliJPerSuiteBase`
  @AfterAll override final def teardown(): Unit = super.teardown()

  def waitAndCloseProject(): Unit = {
    intelliJ.probe.await()
    // Note that we shouldn't wait for a response here (so we shouldn't use org.virtuslab.ideprobe.ProbeDriver#closeProject),
    // since the response sometimes never comes (due to the project being closed), depending on the specific timing.
    intelliJ.ide.closeOpenedProjects()
  }
}
