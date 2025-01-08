package com.virtuslab.gitmachete.uitest

import java.nio.file.Paths
import java.util
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.virtuslab.ideprobe.{IdeProbeFixture, ProbeDriver, RunningIntelliJFixture}
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.robot.RobotPluginExtension

trait RunningIntelliJFixtureExtension extends RobotPluginExtension { this: IdeProbeFixture =>

  private def loadScript(baseName: String) = {
    Paths.get(getClass.getResource(s"/$baseName.rhino.js").toURI).content()
  }

  private val initialCodebase = Seq("ide", "project", "common").map(loadScript).mkString

  private val commonCodebase = loadScript("common")

  implicit class RunningIntelliJFixtureOps(intelliJ: RunningIntelliJFixture) {

    private val probe: ProbeDriver = intelliJ.probe

    def doAndAwait(action: => Unit): Unit = {
      action
      probe.await()
    }

    private def runJs(
        @Language("JavaScript") statement: String,
        codebase: String = commonCodebase
    ): Unit = {
      println(s"runJs: executing `$statement`")
      probe.withRobot.robot.runJs(codebase + statement, /* runInEdt */ false)
      println(s"runJs: executed `$statement`")
    }

    private def callJs[T <: java.io.Serializable](
        @Language("JavaScript") expression: String,
        codebase: String = commonCodebase
    ): T = {
      println(s"callJs: evaluating `$expression`")
      val result = probe.withRobot.robot.callJs[T](codebase + expression, /* runInEdt */ false)

      val representation = result match {
        case array: Array[Int]    => java.util.Arrays.toString(array)
        case array: Array[AnyRef] => java.util.Arrays.deepToString(array)
        case _                    => result.toString
      }
      println(s"callJs: evaluated `$expression` to `$representation`")

      result
    }

    object ide {
      def configure(): Unit = {
        runJs("ide.configure()", codebase = initialCodebase)
      }

      def closeOpenedProjects(): Unit = {
        runJs("ide.closeOpenedProjects()")
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

      def acceptSquash(): Unit = doAndAwait {
        runJs("project.acceptSquash()")
      }

      def acceptSlideIn(): Unit = doAndAwait {
        runJs("project.acceptSlideIn()")
      }

      def acceptSuggestedBranchLayout(): Unit = doAndAwait {
        runJs("project.acceptSuggestedBranchLayout()")
      }

      def discoverBranchLayout(): Unit = doAndAwait {
        runJs("project.discoverBranchLayout()")
      }

      def assertBranchesAreEqual(branchA: String, branchB: String): Unit = {
        val hashA = getHashOfCommitPointedByBranch(branchA)
        val hashB = getHashOfCommitPointedByBranch(branchB)
        assertEquals(hashA, hashB)
      }

      def assertSyncToParentStatus(branch: String, status: String): Unit = {
        val actual = getSyncToParentStatus(branch)
        assertEquals(status, actual)
      }

      def assertLocalAndRemoteBranchesAreEqual(branch: String): Unit = {
        assertBranchesAreEqual(branch, s"origin/$branch")
      }

      def assertNoUncommittedChanges(): Unit = {
        assertEquals(Seq.empty, getDiffOfWorkingTreeToHead())
      }

      def checkoutBranch(branch: String): Unit = doAndAwait {
        runJs(s"project.checkoutBranch('$branch')")
      }

      def checkoutFirstChildBranch(): Unit = doAndAwait {
        runJs("project.checkoutFirstChildBranch()")
      }

      def checkoutNextBranch(): Unit = doAndAwait {
        runJs("project.checkoutNextBranch()")
      }

      def checkoutPreviousBranch(): Unit = doAndAwait {
        runJs("project.checkoutPreviousBranch()")
      }

      def checkoutParentBranch(): Unit = doAndAwait {
        runJs("project.checkoutParentBranch()")
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
          runJs("project.contextMenu.checkout()")
        }

        def checkoutAndSyncByRebase(): Unit = {
          runJs("project.contextMenu.checkoutAndSyncByRebase()")
          // Deliberately not calling probe.await() after triggering rebase action. The method freezes UI
          // and makes impossible to proceed with actions (e.g. findAndClickButton('Start Rebasing')).
          // This comment applies here, below and to toolbar counterparts.
        }

        def push(): Unit = doAndAwait {
          runJs("project.contextMenu.push()")
        }

        def pull(): Unit = doAndAwait {
          runJs("project.contextMenu.pull()")
        }

        def resetToRemote(): Unit = doAndAwait {
          runJs("project.contextMenu.resetToRemote()")
        }

        def fastForwardMerge(): Unit = doAndAwait {
          runJs("project.contextMenu.fastForwardMerge()")
        }

        def slideIn(): Unit = doAndAwait {
          runJs("project.contextMenu.slideIn()")
        }

        def slideOut(): Unit = doAndAwait {
          runJs("project.contextMenu.slideOut()")
        }

      }

      object toolbar {

        def pull(): Unit = doAndAwait {
          runJs("project.toolbar.pull()")
        }

        def resetToRemote(): Unit = doAndAwait {
          runJs("project.toolbar.resetToRemote()")
        }

        def fastForwardMerge(): Unit = doAndAwait {
          runJs("project.toolbar.fastForwardMerge()")
        }

        def discoverBranchLayout(): Unit = doAndAwait {
          runJs("project.toolbar.discoverBranchLayout()")
        }

        def toggleListingCommits(): Unit = doAndAwait {
          runJs("project.toolbar.toggleListingCommits()")
        }

        def fetchAll(): Unit = doAndAwait {
          runJs("project.toolbar.fetchAll()")
        }
      }

      def doesBranchExist(branch: String): Boolean = {
        callJs[java.lang.Boolean](s"project.doesBranchExist('$branch')")
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

      def fastForwardMergeCurrentToParent(): Unit = doAndAwait {
        runJs("project.fastForwardMergeCurrentToParent()")
      }

      def fastForwardMergeSelectedToParent(branch: String): Unit = doAndAwait {
        runJs(s"project.fastForwardMergeSelectedToParent('$branch')")
      }

      def syncSelectedToParentByRebase(branch: String): Unit = doAndAwait {
        runJs(s"project.syncSelectedToParentByRebase('$branch')")
      }

      def syncCurrentToParentByRebase(): Unit = doAndAwait {
        runJs(s"project.syncCurrentToParentByRebase()")
      }

      def syncSelectedToParentByMerge(branch: String): Unit = doAndAwait {
        runJs(s"project.syncSelectedToParentByMerge('$branch')")
      }

      def squashSelected(branch: String): Unit = doAndAwait {
        runJs(s"project.squashSelected('$branch')")
      }

      def squashCurrent(): Unit = doAndAwait {
        runJs(s"project.squashCurrent()")
      }

      def openGitMacheteTab(): Unit = {
        runJs("project.openGitMacheteTab()")
      }

      def usePrettyClick(): Unit = {
        runJs("project.usePrettyClick()")
      }

      def pullSelected(branch: String): Unit = doAndAwait {
        runJs(s"project.pullSelected('$branch')")
      }

      def pullCurrent(): Unit = doAndAwait {
        runJs("project.pullCurrent()")
      }

      def refreshModelAndGetManagedBranches(): Array[String] = {
        callJs("project.refreshGraphTableModel(); project.getManagedBranches()")
      }

      def refreshModelAndGetManagedBranchesAndCommits(): Array[String] = {
        callJs("project.refreshGraphTableModel(); project.getManagedBranchesAndCommits()")
      }

      def refreshModelAndGetRowCount(): Int = {
        callJs("project.refreshGraphTableModel().getRowCount()")
      }

      def resetCurrentToRemote(): Unit = doAndAwait {
        runJs(s"project.resetCurrentToRemote()")
      }

      def resetToRemote(branch: String): Unit = doAndAwait {
        runJs(s"project.resetToRemote('$branch')")
      }

      def slideOutSelected(branch: String): Unit = {
        runJs(s"project.slideOutSelected('$branch')")
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
