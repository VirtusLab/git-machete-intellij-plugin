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
    val pluginPath = sys.props
      .get("ui-test.plugin.path")
      .filterNot(_.isEmpty)
      .getOrElse(throw new Exception("Plugin path is not provided"))
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

    private def doAndAwait(action: => Unit): Unit = {
      action
      probe.await()
    }

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
      def acceptBranchDeletionOnSlideOut(): Unit = doAndAwait {
        runJs("project.acceptBranchDeletionOnSlideOut()")
      }

      def acceptCreateNewBranch(): Unit = doAndAwait {
        runJs("project.acceptCreateNewBranch()")
      }

      def acceptPush(): Unit = doAndAwait {
        runJs("project.acceptPush()")
      }

      def acceptForcePush(): Unit = doAndAwait {
        runJs("project.acceptForcePush()")
      }

      def acceptRebase(): Unit = doAndAwait {
        runJs("project.acceptRebase()")
      }

      def acceptResetToRemote(): Unit = doAndAwait {
        runJs("project.acceptResetToRemote()")
      }

      def acceptSlideIn(): Unit = doAndAwait {
        runJs("project.acceptSlideIn()")
      }

      def acceptSuggestedBranchLayout(): Unit = doAndAwait {
        runJs("project.acceptSuggestedBranchLayout()")
      }

      def saveDiscoveredBranchLayout(): Unit = doAndAwait {
        runJs("project.saveDiscoveredBranchLayout()")
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

      def checkoutBranch(branch: String): Unit = doAndAwait {
        runJs(s"project.checkoutBranch('$branch')")
      }

      def configure(): Unit = {
        runJs("project.configure()")
      }

      def moveMouseToTheMiddleAndWait(secondsToWait: Int): Unit = {
        runJs(s"project.moveMouseToTheMiddleAndWait('$secondsToWait')")
      }

      def switchRepo(indexInComboBox: Int): Unit = {
        runJs(s"project.findComboBoxAndSwitchRepo('$indexInComboBox')")
      }

      object contextMenu {

        def openContextMenu(branch: String): Unit = doAndAwait {
          runJs(s"project.findCellAndRightClick('$branch')")
        }

        def checkout(): Unit = doAndAwait {
          runJs(s"project.contextMenu.checkout()")
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

        def checkoutAndSyncByMerge(): Unit = doAndAwait {
          runJs(s"project.contextMenu.checkoutAndSyncByMerge()")
        }

        def syncByMerge(): Unit = doAndAwait {
          runJs(s"project.contextMenu.syncByMerge()")
        }

        def overrideForkPoint(): Unit = doAndAwait {
          runJs(s"project.contextMenu.overrideForkPoint()")
        }

        def push(): Unit = doAndAwait {
          runJs(s"project.contextMenu.push()")
        }

        def pull(): Unit = doAndAwait {
          runJs(s"project.contextMenu.pull()")
        }

        def resetToRemote(): Unit = doAndAwait {
          runJs(s"project.contextMenu.resetToRemote()")
        }

        def fastForwardMerge(): Unit = doAndAwait {
          runJs(s"project.contextMenu.fastForwardMerge()")
        }

        def slideIn(): Unit = doAndAwait {
          runJs(s"project.contextMenu.slideIn()")
        }

        def slideOut(): Unit = doAndAwait {
          runJs(s"project.contextMenu.slideOut()")
        }

        def showInGitLog(): Unit = doAndAwait {
          runJs(s"project.contextMenu.showInGitLog()")
        }
      }

      object toolbar {

        def syncByRebase(): Unit = {
          runJs(s"project.toolbar.syncByRebase()")
        }

        def syncByMerge(): Unit = doAndAwait {
          runJs(s"project.toolbar.syncByMerge()")
        }

        def pull(): Unit = doAndAwait {
          runJs(s"project.toolbar.pull()")
        }

        def resetToRemote(): Unit = doAndAwait {
          runJs(s"project.toolbar.resetToRemote()")
        }

        def fastForwardMerge(): Unit = doAndAwait {
          runJs(s"project.toolbar.fastForwardMerge()")
        }

        def discoverBranchLayout(): Unit = doAndAwait {
          runJs(s"project.toolbar.discoverBranchLayout()")
        }

        def toggleListingCommits(): Unit = doAndAwait {
          runJs(s"project.toolbar.toggleListingCommits()")
        }

        def fetchAll(): Unit = doAndAwait {
          runJs(s"project.toolbar.fetchAll()")
        }
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

      def openGitMacheteTab(): Unit = {
        runJs("project.openGitMacheteTab()")
      }

      def usePrettyClick(): Unit = {
        runJs("project.usePrettyClick()")
      }

      def refreshModelAndGetManagedBranches(): Array[String] = {
        callJs("project.refreshGraphTableModel(); project.getManagedBranches()")
      }

      def refreshModelAndGetRowCount(): Int = {
        callJs("project.refreshGraphTableModel().getRowCount()")
      }

      def rejectBranchDeletionOnSlideOut(): Unit = doAndAwait {
        runJs("project.rejectBranchDeletionOnSlideOut()")
      }

      def writeToTextField(text: String): Unit = {
        runJs(s"project.findTextFieldAndWrite('$text', /* instant */ false)")
      }
    }
  }
}
