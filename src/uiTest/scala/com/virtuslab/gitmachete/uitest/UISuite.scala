package com.virtuslab.gitmachete.uitest

import org.virtuslab.ideprobe.junit4.RunningIntelliJPerSuite
import org.virtuslab.ideprobe.{IdeProbeFixture, IntelliJFixture}

trait UISuite extends RunningIntelliJPerSuite with IdeProbeFixture with RunningIntelliJFixtureExtension {

  registerFixtureTransformer(_.withAfterIntelliJStartup((_, intelliJ) => intelliJ.ide.configure()))

  override protected def baseFixture: IntelliJFixture = fixtureFromConfig("ideprobe.conf")

  def waitAndCloseProject(): Unit = {
    intelliJ.probe.await()
    // Note that we shouldn't wait for a response here (so we shouldn't use org.virtuslab.ideprobe.ProbeDriver#closeProject),
    // since the response sometimes never comes (due to the project being closed), depending on the specific timing.
    intelliJ.ide.closeOpenedProjects()
  }
}
