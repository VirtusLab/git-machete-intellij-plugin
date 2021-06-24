package com.virtuslab.gitmachete.uitest

import java.nio.file.Paths
import java.util
import org.intellij.lang.annotations.Language
import org.junit.Assert
import org.virtuslab.ideprobe.RunningIntelliJFixture
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.IdeProbeFixture
import org.virtuslab.ideprobe.dependencies.Plugin
import org.virtuslab.ideprobe.robot.RobotPluginExtension

trait RunningIntelliJFixtureExtension extends RobotPluginExtension { this: IdeProbeFixture =>

  private val machetePlugin: Plugin = {
    val cwd = Paths.get(System.getProperty("user.dir"))
    val repositoryRoot = cwd.getParent
    val distedPluginsDir = repositoryRoot.resolve("build/distributions")
    val latestFileInDistDir = distedPluginsDir.toAbsolutePath.toFile.listFiles().maxBy(_.lastModified())
    Plugin.Direct(latestFileInDistDir.toURI)
  }

  registerFixtureTransformer(_.withPlugin(machetePlugin))
  registerFixtureTransformer(_.withAfterIntelliJStartup((_, intelliJ) => intelliJ.ide.configure()))

  private val rhinoCodebase = {
    def loadScript(baseName: String) = {
      Paths.get(getClass.getResource(s"/$baseName.rhino.js").toURI).content()
    }

    Seq("common", "ide", "project").map(loadScript).mkString
  }

  implicit class RunningIntelliJFixtureOps(intelliJ: RunningIntelliJFixture) {

    private def runJs(@Language("JavaScript") statement: String): Unit = {
      intelliJ.probe.withRobot.robot.runJs(rhinoCodebase + statement, /* runInEdt */ false)
    }

    private def callJs[T](@Language("JavaScript") expression: String): T = {
      intelliJ.probe.withRobot.robot.callJs(rhinoCodebase + expression, /* runInEdt */ false)
    }

    object ide {

      def configure(): Unit = {
        runJs("""
          importClass(java.util.stream.Collectors);
          importClass(java.util.stream.Stream);

          importClass(com.intellij.diagnostic.DebugLogManager);
          importClass(com.intellij.ide.GeneralSettings);
          importClass(com.intellij.openapi.application.ApplicationInfo);
          importClass(com.intellij.openapi.extensions.PluginId);
          importClass(com.intellij.openapi.progress.ProgressManager);

          const settings = GeneralSettings.getInstance();
          settings.setConfirmExit(false);
          settings.setShowTipsOnStartup(false);

          const enableDebugLog = false;
          if (enableDebugLog) {
            const logCategories = Stream.of(
              'binding',
              'branchlayout',
              'gitcore',
              'gitmachete.backend',
              'gitmachete.frontend.actions',
              'gitmachete.frontend.graph',
              'gitmachete.frontend.ui',
            ).map(function (name) {
              return new DebugLogManager.Category(name, DebugLogManager.DebugLogLevel.DEBUG);
            }).collect(Collectors.toList());

            const debugLogManager = DebugLogManager.getInstance();
            // `applyCategories` is non-persistent (so the categories don't stick for the future IDE runs), unlike `saveCategories`.
            debugLogManager.applyCategories(logCategories);
          }
        """)
      }

      def closeOpenedProjects(): Unit = {
        runJs("ide.closeOpenedProjects()")
      }
    }

    object project {
      def acceptBranchDeletionOnSlideOut(): Unit = {
        runJs("project.findAndClickButton('Slide Out & Delete Local Branch')")
        intelliJ.probe.await()
      }

      def acceptSuggestedBranchLayout(): Unit = {
        runJs("project.findAndClickButton('Yes')")
      }

      def assertBranchesAreEqual(branchA: String, branchB: String): Unit = {
        val hashA = getHashOfCommitPointedByBranch(branchA)
        val hashB = getHashOfCommitPointedByBranch(branchB)
        Assert.assertEquals(hashB, hashA)
      }

      def assertLocalAndRemoteBranchesAreEqual(branch: String): Unit = {
        assertBranchesAreEqual(branch, s"origin/$branch")
      }

      def assertWorkingTreeIsAtHead(): Unit = {
        Assert.assertEquals(Seq.empty, getDiffOfWorkingTreeToHead())
      }

      def checkoutBranch(branch: String): Unit = {
        runJs(s"project.checkoutBranch('$branch')")
        intelliJ.probe.await()
      }

      def configure(): Unit = {
        runJs("project.configure()")
      }

      def discoverBranchLayout(): Unit = {
        runJs("project.discoverBranchLayout()")
        intelliJ.probe.await()
      }

      def getCurrentBranchName(): String = {
        callJs[String]("project.getCurrentBranchName()")
      }

      def getDiffOfWorkingTreeToHead(): Seq[String] = {
        callJs[util.ArrayList[String]]("project.getDiffOfWorkingTreeToHead()").asScala
      }

      def getHashOfCommitPointedByBranch(branch: String): String = {
        callJs(s"project.getHashOfCommitPointedByBranch('$branch')")
      }

      def fastForwardMergeSelectedBranchToParent(branch: String): Unit = {
        runJs(s"project.fastForwardMergeSelectedBranchToParent('$branch')")
        intelliJ.probe.await()
      }

      def fastForwardMergeCurrentBranchToParent(): Unit = {
        runJs(s"project.fastForwardMergeCurrentBranchToParent()")
        intelliJ.probe.await()
      }

      def openGitMacheteTab(): Unit = {
        runJs("project.openTab()")
      }

      def pullBranch(branch: String): Unit = {
        runJs(s"project.pullBranch('$branch')")
        intelliJ.probe.await()
      }

      def pullCurrentBranch(): Unit = {
        runJs(s"project.pullCurrentBranch()")
        intelliJ.probe.await()
      }

      def refreshModelAndGetManagedBranches(): Array[String] = {
        callJs("project.refreshGraphTableModel(); project.getManagedBranches()")
      }

      def refreshModelAndGetRowCount(): Int = {
        callJs("project.refreshGraphTableModel().getRowCount()")
      }

      def rejectBranchDeletionOnSlideOut(): Unit = {
        runJs("project.findAndClickButton('Slide Out & Keep Local Branch')")
        intelliJ.probe.await()
      }

      def resetCurrentBranchToRemote(): Unit = {
        runJs(s"project.resetCurrentBranchToRemote()")
        intelliJ.probe.await()
      }

      def resetBranchToRemote(branch: String): Unit = {
        runJs(s"project.resetBranchToRemote('$branch')")
        intelliJ.probe.await()
      }

      def slideOutBranch(branch: String): Unit = {
        runJs(s"project.slideOutBranch('$branch')")
        intelliJ.probe.await()
      }

      def toggleListingCommits(): Unit = {
        runJs("project.toggleListingCommits()")
      }
    }
  }
}
