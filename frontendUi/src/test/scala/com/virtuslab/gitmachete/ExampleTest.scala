package com.virtuslab.gitmachete

import java.net.URI
import java.nio.file.Path

import com.virtuslab.ideprobe.{ IntegrationTestSuite, IntelliJFixture, WorkspaceConfig, WorkspaceTemplate }
import com.virtuslab.ideprobe.dependencies.{ DependencyProvider, IntelliJResolver, IntelliJVersion, Plugin, PluginResolver, Resource, ResourceProvider }
import com.virtuslab.ideprobe.ide.intellij.{ DriverConfig, IntelliJFactory }
import org.junit.Assert
import org.junit.Test

class ExampleTest extends IntegrationTestSuite {
  @Test def test(): Unit = {
    val uri = new URI("/home/pawellipski/git-machete-intellij-plugin/frontendUi/build/distributions/git-machete-intellij-plugin-0.0.64-SNAPSHOT.zip")
    IntelliJFixture(
      version = IntelliJVersion.V2019_3_1,
      plugins = Seq(Plugin.Direct(uri)),
      workspaceTemplate = {
        val repoUri = new URI("/home/pawellipski/git-machete-intellij-plugin")
        WorkspaceTemplate.from(WorkspaceConfig.GitBranch(Resource.from(repoUri), "master"))
      },
      intelliJFactory = new IntelliJFactory(
        new DependencyProvider(IntelliJResolver.Official, PluginResolver.Official, ResourceProvider.Default),
        DriverConfig(headless = false)
      )
    ).run { ij =>
      val project = ij.probe.openProject(Path.of("/home/pawellipski/git-machete-intellij-plugin"))
      for (t <- ij.probe.vcsTabNames) {
        if (t.contains("vcs") || t.contains("Vcs")) {
          println(t)
        }
      }
      println("errors = " + ij.probe.errors)
      println("messages = " + ij.probe.messages)
      val plugins = ij.probe.plugins
      Assert.assertEquals(plugins, Nil)
    }
  }
}
