package com.virtuslab.gitmachete.uitest

import java.nio.file.Paths
import java.util
import org.intellij.lang.annotations.Language
import org.junit.Assert
import org.virtuslab.ideprobe.{IdeProbeFixture, ProbeDriver, RunningIntelliJFixture}
import org.virtuslab.ideprobe.Extensions._
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

    private val probe: ProbeDriver = intelliJ.probe

    private def runJs(@Language("JavaScript") statement: String, codebase: String = commonCodebase): Unit = {
      probe.withRobot.robot.runJs(codebase + statement, /* runInEdt */ false)
    }

    private def callJs[T](@Language("JavaScript") expression: String, codebase: String = commonCodebase): T = {
      probe.withRobot.robot.callJs(codebase + expression, /* runInEdt */ false)
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

      def findAndResizeIdeFrame(): Unit = {
        runJs("project.findAndResizeIdeFrame()")
      }
    }

    object project {
      def acceptBranchDeletionOnSlideOut(): Unit = {
        runJs("project.acceptBranchDeletionOnSlideOut()")
        probe.await()
      }

      def acceptCreateNewBranch(): Unit = {
        runJs("project.acceptCreateNewBranch()")
        probe.await()
      }

      def acceptPush(): Unit = {
        runJs("project.acceptPush()")
        probe.await()
      }

      def acceptForcePush(): Unit = {
        runJs("project.acceptForcePush()")
        probe.await()
      }

      def acceptRebase(): Unit = {
        runJs("project.acceptRebase()")
        probe.await()
      }

      def acceptSlideIn(): Unit = {
        runJs("project.acceptSlideIn()")
        probe.await()
      }

      def acceptSuggestedBranchLayout(): Unit = {
        runJs("project.acceptSuggestedBranchLayout()")
        probe.await()
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
        probe.await()
      }

      def configure(): Unit = {
        runJs("project.configure()")
      }

      def moveMouseToTheMiddleAndWait(secondsToWait: Int): Unit = {
        runJs(s"project.moveMouseToTheMiddleAndWait('$secondsToWait')");
      }

      def switchRepo(indexInComboBox: Int): Unit = {
        runJs(s"project.findComboBoxAndSwitchRepo('$indexInComboBox')")
      }

      object contextMenu {

        def openContextMenu(branch: String): Unit = {
          runJs(s"project.findCellAndRightClick('$branch')")
          probe.await()
        }

        def checkout(): Unit = {
          runJs(s"project.contextMenu.checkout()")
          probe.await()
        }

        def checkoutAndSyncByRebase(): Unit = {
          runJs(s"project.contextMenu.checkoutAndSyncByRebase()")
          // Deliberately not calling probe.await() after triggering rebase action. The method freezes UI
          // and makes impossible to proceed with actions (e.g. findAndClickButton('Start Rebasing')).
          // This comment applies here, below and to toolbar counterparts.
        }

        def syncByRebase(): Unit = {
          runJs(s"project.contextMenu.syncByRebase()")
        }

        def checkoutAndSyncByMerge(): Unit = {
          runJs(s"project.contextMenu.checkoutAndSyncByMerge()")
          probe.await()
        }

        def syncByMerge(): Unit = {
          runJs(s"project.contextMenu.syncByMerge()")
          probe.await()
        }

        def overrideForkPoint(): Unit = {
          runJs(s"project.contextMenu.overrideForkPoint()")
          probe.await()
        }

        def push(): Unit = {
          runJs(s"project.contextMenu.push()")
          probe.await()
        }

        def pull(): Unit = {
          runJs(s"project.contextMenu.pull()")
          probe.await()
        }

        def resetToRemote(): Unit = {
          runJs(s"project.contextMenu.resetToRemote()")
          probe.await()
        }

        def fastForwardMerge(): Unit = {
          runJs(s"project.contextMenu.fastForwardMerge()")
          probe.await()
        }

        def slideIn(): Unit = {
          runJs(s"project.contextMenu.slideIn()")
          probe.await()
        }

        def slideOut(): Unit = {
          runJs(s"project.contextMenu.slideOut()")
          probe.await()
        }

        def showInGitLog(): Unit = {
          runJs(s"project.contextMenu.showInGitLog()")
          probe.await()
        }
      }

      object toolbar {

        def toggleListingCommits(): Unit = {
          runJs(s"project.toolbar.toggleListingCommits()")
          probe.await()
        }

        def fetchAll(): Unit = {
          runJs(s"project.toolbar.fetchAll()")
          probe.await()
        }
      }

      def discoverBranchLayout(): Unit = {
        runJs("project.discoverBranchLayout()")
        probe.await()
      }

      def fastForwardMergeCurrentBranchToParent(): Unit = {
        runJs(s"project.fastForwardMergeCurrentBranchToParent()")
        probe.await()
      }

      def fastForwardMergeSelectedBranchToParent(branch: String): Unit = {
        runJs(s"project.fastForwardMergeSelectedBranchToParent('$branch')")
        probe.await()
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

      def syncSelectedToParentByRebaseAction(branch: String): Unit = {
        runJs(s"project.syncSelectedToParentByRebaseAction('$branch')")
      }

      def syncCurrentToParentByRebaseAction(): Unit = {
        runJs(s"project.syncCurrentToParentByRebaseAction()")
      }

      def syncSelectedToParentByMergeAction(branch: String): Unit = {
        runJs(s"project.syncSelectedToParentByMergeAction('$branch')")
        probe.await()
      }

      def syncCurrentToParentByMergeAction(): Unit = {
        runJs(s"project.syncCurrentToParentByMergeAction()")
        probe.await()
      }

      def openGitMacheteTab(): Unit = {
        runJs("project.openGitMacheteTab()")
      }

      def pullSelectedBranch(branch: String): Unit = {
        runJs(s"project.pullSelectedBranch('$branch')")
        probe.await()
      }

      def pullCurrentBranch(): Unit = {
        runJs(s"project.pullCurrentBranch()")
        probe.await()
      }

      def refreshModelAndGetManagedBranches(): Array[String] = {
        callJs("project.refreshGraphTableModel(); project.getManagedBranches()")
      }

      def refreshModelAndGetRowCount(): Int = {
        callJs("project.refreshGraphTableModel().getRowCount()")
      }

      def rejectBranchDeletionOnSlideOut(): Unit = {
        runJs("project.rejectBranchDeletionOnSlideOut()")
        probe.await()
      }

      def resetCurrentBranchToRemote(): Unit = {
        runJs(s"project.resetCurrentBranchToRemote()")
        probe.await()
      }

      def resetBranchToRemote(branch: String): Unit = {
        runJs(s"project.resetBranchToRemote('$branch')")
        probe.await()
      }

      def slideOutBranch(branch: String): Unit = {
        runJs(s"project.slideOutBranch('$branch')")
        probe.await()
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
