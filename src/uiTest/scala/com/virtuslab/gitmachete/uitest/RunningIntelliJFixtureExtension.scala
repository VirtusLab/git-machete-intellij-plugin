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

      def acceptCreateNewBranch(): Unit = {
        runJs("project.acceptCreateNewBranch()")
      }

      def acceptSlideIn(): Unit = {
        runJs("project.acceptSlideIn()")
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

      object contextMenu {

        def openContextMenu(branch: String): Unit = {
          runJs(s"project.findCellAndRightClick('$branch')")
          intelliJ.probe.await()
        }

        def checkout(): Unit = {
          runJs(s"project.contextMenu.checkout()")
          intelliJ.probe.await()
        }

        def checkoutAndSyncByRebase(): Unit = {
          runJs(s"project.contextMenu.checkoutAndSyncByRebase()")
          intelliJ.probe.await()
        }

        def syncByRebase(): Unit = {
          runJs(s"project.contextMenu.syncByRebase()")
          intelliJ.probe.await()
        }

        def checkoutAndSyncByMerge(): Unit = {
          runJs(s"project.contextMenu.checkoutAndSyncByMerge()")
          intelliJ.probe.await()
        }

        def syncByMerge(): Unit = {
          runJs(s"project.contextMenu.syncByMerge()")
          intelliJ.probe.await()
        }

        def overrideForkPoint(): Unit = {
          runJs(s"project.contextMenu.overrideForkPoint()")
          intelliJ.probe.await()
        }

        def push(): Unit = {
          runJs(s"project.contextMenu.push()")
          intelliJ.probe.await()
        }

        def pull(): Unit = {
          runJs(s"project.contextMenu.pull()")
          intelliJ.probe.await()
        }

        def resetToRemote(): Unit = {
          runJs(s"project.contextMenu.resetToRemote()")
          intelliJ.probe.await()
        }

        def fastForwardMerge(): Unit = {
          runJs(s"project.contextMenu.fastForwardMerge()")
          intelliJ.probe.await()
        }

        def slideIn(): Unit = {
          runJs(s"project.contextMenu.slideIn()")
          intelliJ.probe.await()
        }

        def slideOut(): Unit = {
          runJs(s"project.contextMenu.slideOut()")
          intelliJ.probe.await()
        }

        def showInGitLog(): Unit = {
          runJs(s"project.contextMenu.showInGitLog()")
          intelliJ.probe.await()
        }
      }

      object toolbar {

        def fetchAll(): Unit = {
          runJs(s"project.toolbar.fetchAll()")
          intelliJ.probe.await()
        }
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

      def syncSelectedToParentByMergeAction(branch: String): Unit = {
        runJs(s"project.syncSelectedToParentByMergeAction('$branch')")
        intelliJ.probe.await()
      }

      def syncCurrentToParentByMergeAction(): Unit = {
        runJs(s"project.syncCurrentToParentByMergeAction()")
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

      def writeToTextField(text: String): Unit = {
        runJs(s"project.findTextFieldAndWrite('$text', /* instant */ false)")
      }
    }
  }
}
