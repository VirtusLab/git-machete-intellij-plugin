package com.virtuslab.gitmachete.uitest

import org.virtuslab.ideprobe.dependencies.IntelliJVersion
import org.virtuslab.ideprobe.{Config, IdeProbeFixture, IntelliJFixture}


object UISuite
  extends RunningIntelliJPerSuite
    with IdeProbeFixture
    with RunningIntelliJFixtureExtension {

  lazy val intelliJVersion: IntelliJVersion = {
    val version = sys.props.get("ui-test.intellij.version")
      .filterNot(_.isEmpty).getOrElse(throw new Exception("IntelliJ version is not provided"))
    // We're cheating here a bit since `version` might be either a build number or a release number,
    // while we're always treating it as a build number.
    // Still, as of ide-probe 0.26.0, even when release number like `2020.3` is passed as `build`, UI tests work just fine.
    IntelliJVersion(build = version, release = None)
  }

  override protected def baseFixture: IntelliJFixture = {
    // By default, the config is taken from <class-name>.conf resource, see org.virtuslab.ideprobe.IdeProbeFixture.resolveConfig
    fixtureFromConfig().withVersion(intelliJVersion)
  }

}
