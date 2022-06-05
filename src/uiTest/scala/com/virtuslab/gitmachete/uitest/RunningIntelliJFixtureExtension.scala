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
    val pluginPath = sys.props.get("ui-test.plugin.path")
      .filterNot(_.isEmpty).getOrElse(throw new Exception("Plugin path is not provided"))
    Plugin.Direct(Paths.get(pluginPath).toUri)
  }

  registerFixtureTransformer(_.withPlugin(machetePlugin))
  registerFixtureTransformer(_.withAfterIntelliJStartup((_, intelliJ) => intelliJ.ide.configure()))

  private def loadScript(baseName: String) = {
    Paths.get(getClass.getResource(s"/$baseName.rhino.js").toURI).content()
  }

  private val initialCodebase = Seq("ide", "project", "common").map(loadScript).mkString

  private val commonCodebase = loadScript("common")

  implicit class RunningIntelliJFixtureOps(intelliJ: RunningIntelliJFixture) {

    private def runJs(@Language("JavaScript") statement: String, codebase: String = commonCodebase): Unit = {
      intelliJ.probe.withRobot.robot.runJs(codebase + statement, /* runInEdt */ false)
    }

    private def callJs[T](@Language("JavaScript") expression: String, codebase: String = commonCodebase): T = {
      intelliJ.probe.withRobot.robot.callJs(codebase + expression, /* runInEdt */ false)
    }

    object ide {
      def configure(): Unit = {
        runJs("ide.configure(/* enableDebugLog */ false)", codebase = initialCodebase)
      }

      def closeOpenedProjects(): Unit = {
        runJs("ide.closeOpenedProjects()")
      }

      def getMajorVersion(): String = {
        callJs("ide.getMajorVersion()")
      }
    }

    object project {
      def acceptBranchDeletionOnSlideOut(): Unit = {
        runJs("project.acceptBranchDeletionOnSlideOut()")
        intelliJ.probe.await()
      }

      def acceptSuggestedBranchLayout(): Unit = {
        runJs("project.acceptSuggestedBranchLayout()")
      }

      def assertBranchesAreEqual(branchA: String, branchB: String): Unit = {
        val hashA = getHashOfCommitPointedByBranch(branchA)
        val hashB = getHashOfCommitPointedByBranch(branchB)
        Assert.assertEquals(hashB, hashA)
      }

      def assertSyncToParentStatus(branch: String, status: String): Unit = {
        val actual = getSyncToParentStatus(branch)
        Assert.assertEquals(actual, status)
      }

      def assertLocalAndRemoteBranchesAreEqual(branch: String): Unit = {
        assertBranchesAreEqual(branch, s"origin/$branch")
      }

      def assertNoUncommittedChanges(): Unit = {
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

      def fastForwardMergeCurrentBranchToParent(): Unit = {
        runJs(s"project.fastForwardMergeCurrentBranchToParent()")
        intelliJ.probe.await()
      }

      def fastForwardMergeSelectedBranchToParent(branch: String): Unit = {
        runJs(s"project.fastForwardMergeSelectedBranchToParent('$branch')")
        intelliJ.probe.await()
      }

      def getCurrentBranchName(): String = {
        callJs[String]("project.getCurrentBranchName()")
      }

      def getDiffOfWorkingTreeToHead(): Seq[String] = {
        callJs[util.ArrayList[String]]("project.getDiffOfWorkingTreeToHead()").asScala.toSeq
      }

      def getHashOfCommitPointedByBranch(branch: String): String = {
        callJs(s"project.getHashOfCommitPointedByBranch('$branch')")
      }

      def getSyncToParentStatus(child: String): String = {
        callJs(s"project.getSyncToParentStatus('$child')")
      }

      def mergeParentIntoSelectedBranchAction(branch: String): Unit = {
        runJs(s"project.mergeParentIntoSelectedBranchAction('$branch')")
        intelliJ.probe.await()
      }

      def mergeParentIntoCurrentBranchAction(): Unit = {
        runJs(s"project.mergeParentIntoCurrentBranchAction()")
        intelliJ.probe.await()
      }

      def openGitMacheteTab(): Unit = {
        runJs("project.openGitMacheteTab()")
      }

      def pullSelectedBranch(branch: String): Unit = {
        runJs(s"project.pullSelectedBranch('$branch')")
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
        runJs("project.rejectBranchDeletionOnSlideOut()")
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
