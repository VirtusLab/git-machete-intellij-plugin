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

trait GitMacheteExtension extends RobotPluginExtension { this: IdeProbeFixture =>

  private val machetePlugin: Plugin = {
    val cwd = Paths.get(System.getProperty("user.dir"))
    val repositoryRoot = cwd.getParent
    val distedPluginsDir = repositoryRoot.resolve("build/distributions")
    val latestFileInDistDir = distedPluginsDir.toAbsolutePath.toFile.listFiles().maxBy(_.lastModified())
    Plugin.Direct(latestFileInDistDir.toURI)
  }

  registerFixtureTransformer(_.withPlugin(machetePlugin))
  registerFixtureTransformer(_.withAfterIntelliJStartup((_, intelliJ) => intelliJ.machete.runJs("ide.configure(/* enableDebugLog */ false)")))

  private val rhinoCodebase = {
    def loadScript(baseName: String) = {
      Paths.get(getClass.getResource(s"/$baseName.rhino.js").toURI).content()
    }

    Seq("common", "ide", "project").map(loadScript).mkString
  }

  implicit class MacheteExtensions(intelliJ: RunningIntelliJFixture) {
    object machete {
      def refreshModelAndGetRowCount(): Int = {
        callJs("project.refreshGraphTableModel().getRowCount()")
      }

      def getHashOfCommitPointedByBranch(branch: String): String = {
        callJs(s"project.getHashOfCommitPointedByBranch('$branch')")
      }

      def getDiffOfWorkingTreeToHead(): Seq[String] = {
        callJs[util.ArrayList[String]]("project.getDiffOfWorkingTreeToHead()").asScala
      }

      def checkoutBranch(branch: String): Unit = {
        runJs(s"project.checkoutBranch('$branch')")
        intelliJ.probe.awaitIdle()
      }

      def fastForwardMergeSelectedBranchToParent(branch: String): Unit = {
        runJs(s"project.fastForwardMergeSelectedBranchToParent('$branch')")
        intelliJ.probe.awaitIdle()
      }

      def fastForwardMergeCurrentBranchToParent(): Unit = {
        runJs(s"project.fastForwardMergeCurrentBranchToParent()")
        intelliJ.probe.awaitIdle()
      }

      def pullCurrentBranch(): Unit = {
        runJs(s"project.pullCurrentBranch()")
        intelliJ.probe.awaitIdle()
      }

      def pullBranch(branch: String): Unit = {
        runJs(s"project.pullBranch('$branch')")
        intelliJ.probe.awaitIdle()
      }

      def resetCurrentBranchToRemote(): Unit = {
        runJs(s"project.resetCurrentBranchToRemote()")
        intelliJ.probe.awaitIdle()
      }

      def resetBranchToRemote(branch: String): Unit = {
        runJs(s"project.resetBranchToRemote('$branch')")
        intelliJ.probe.awaitIdle()
      }

      def getCurrentBranchName(): String = {
        callJs[String]("project.getCurrentBranchName()")
      }

      def assertWorkingTreeIsAtHead(): Unit = {
        Assert.assertEquals(Seq.empty, getDiffOfWorkingTreeToHead())
      }

      def assertLocalAndRemoteBranchesAreEqual(branch: String): Unit = {
        val localBranchHash = intelliJ.machete.getHashOfCommitPointedByBranch(branch)
        val remoteBranchHash = intelliJ.machete.getHashOfCommitPointedByBranch(s"origin/$branch")
        Assert.assertEquals(remoteBranchHash, localBranchHash)
      }

      def assertBranchesAreEqual(branchA: String, branchB: String): Unit = {
        val hashA = intelliJ.machete.getHashOfCommitPointedByBranch(branchA)
        val hashB = intelliJ.machete.getHashOfCommitPointedByBranch(branchB)
        Assert.assertEquals(hashB, hashA)
      }

      def runJs(@Language("JS") statement: String): Unit = {
        intelliJ.probe.withRobot.robot.runJs(rhinoCodebase + statement, /* runInEdt */ false)
      }

      def callJs[T](@Language("JS") expression: String): T = {
        intelliJ.probe.withRobot.robot.callJs(rhinoCodebase + expression, /* runInEdt */ false)
      }
    }
  }
}
