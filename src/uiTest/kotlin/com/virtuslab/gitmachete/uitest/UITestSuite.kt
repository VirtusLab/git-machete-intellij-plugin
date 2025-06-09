package com.virtuslab.gitmachete.uitest

import com.intellij.util.io.delete
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import kotlin.io.path.readText
import kotlin.io.path.writeText

class UITestSuite : BaseUITestSuite() {

  @Test
  fun skipNonExistentBranches_toggleListingCommits_slideOutRoot() = uiTest {
    machetePostSlideOutHookPath.writeText(""" echo "$@" >> "$machetePostSlideOutHookOutputPath" """)
    machetePostSlideOutHookPath.makeExecutable()

    macheteFilePath.writeText(
      """develop
        |  non-existent
        |    allow-ownership-link
        |      build-chain
        |      update-icons
        |    non-existent-leaf
        |  call-ws
        |non-existent-root
        |master
        |  hotfix/add-trigger
      """.trimMargin(),
    )
    openGitMacheteTab()
    val managedBranches = refreshModelAndGetManagedBranches()
    // Non-existent branches should be skipped while causing no error (only a low-severity notification).
    assertEquals(
      listOf(
        "develop",
        "allow-ownership-link",
        "build-chain",
        "update-icons",
        "call-ws",
        "master",
        "hotfix/add-trigger",
      ),
      managedBranches.toList(),
    )
    toggleListingCommits()
    var branchAndCommitRowsCount = refreshModelAndGetRowCount()
    // 7 branch rows + 11 commit rows
    assertEquals(18, branchAndCommitRowsCount)

    checkoutBranch("allow-ownership-link")
    checkoutFirstChildBranch()
    assertEquals("build-chain", getCurrentBranchName())
    checkoutNextBranch()
    assertEquals("update-icons", getCurrentBranchName())
    checkoutPreviousBranch()
    assertEquals("build-chain", getCurrentBranchName())
    checkoutParentBranch()
    assertEquals("allow-ownership-link", getCurrentBranchName())

    // Let's slide out a root branch now
    slideOutSelected("develop")
    // There shouldn't be a branch deletion dialog as `develop` has children (so it will be slid out but not deleted)
    assertTrue(doesBranchExist("develop"))
    branchAndCommitRowsCount = refreshModelAndGetRowCount()
    // 6 branch rows (`develop` is no longer there) + 7 commit rows
    // (1 commit of `allow-ownership-link` and 3 commits of `call-ws` are all gone)
    assertEquals(13, branchAndCommitRowsCount)

    checkoutBranch("master")
    assertTrue(doesBranchExist("call-ws"))
    slideOutSelected("call-ws")
    acceptBranchDeletionOnSlideOut()
    assertFalse(doesBranchExist("call-ws"))

    val managedBranchesAfterSlideOut = refreshModelAndGetManagedBranches()
    // Non-existent branches should be skipped while causing no error (only a low-severity notification).
    assertEquals(
      listOf(
        "allow-ownership-link",
        "build-chain",
        "update-icons",
        "master",
        "hotfix/add-trigger",
      ),
      managedBranchesAfterSlideOut.toList(),
    )
    branchAndCommitRowsCount = refreshModelAndGetRowCount()
    // 5 branch rows (`call-ws` is also no longer there) + 7 commit rows
    assertEquals(12, branchAndCommitRowsCount)

    assertEquals(
      " develop non-existent call-ws\n call-ws\n",
      machetePostSlideOutHookOutputPath.readText(),
    )
  }

  @Test
  fun discoverBranchLayout() = uiTest {
    // When model is refreshed and machete file has not been modified for a long time, then discover suggestion should occur
    val epochStart = FileTime.fromMillis(0)
    Files.setLastModifiedTime(macheteFilePath, epochStart)
    openGitMacheteTab()
    acceptSuggestedBranchLayout()
    var branchRowsCount = refreshModelAndGetRowCount()
    assertEquals(8, branchRowsCount)
    macheteFilePath.delete()
    // When model is refreshed and machete file is empty, then autodiscover should occur
    branchRowsCount = refreshModelAndGetRowCount()
    assertEquals(8, branchRowsCount)
    // This time, wipe out `machete` file (instead of removing it completely)
    macheteFilePath.writeText("\n")
    // Now let's test an explicit discover instead
    discoverBranchLayout()
    branchRowsCount = refreshModelAndGetRowCount()
    assertEquals(8, branchRowsCount)
    // In this case a non-existent branch is defined by `machete` file and it should persist (no autodiscover)
    macheteFilePath.writeText("non-existent")
    branchRowsCount = refreshModelAndGetRowCount()
    assertEquals(0, branchRowsCount)
  }

  @Test
  fun fastForwardParentOfBranch() = uiTest {
    // fastForwardParentOfBranch_parentIsCurrentBranch
    openGitMacheteTab()
    checkoutBranch("master")
    // `master` is the parent of `hotfix/add-trigger`. Let's fast-forward `master` to match `hotfix/add-trigger`.
    fastForwardMergeSelectedToParent("hotfix/add-trigger")
    assertBranchesAreEqual("master", "hotfix/add-trigger")
    assertNoUncommittedChanges()

    // fastForwardParentOfBranch_childIsCurrentBranch
    checkoutBranch("call-ws")
    fastForwardMergeCurrentToParent()
    assertBranchesAreEqual("develop", "call-ws")
    assertNoUncommittedChanges()
  }

  @Test
  fun syncToParentByRebaseAction() = uiTest {
    // Skip the fork point ($2) as it's a commit hash and it'll differ between test invocations.
    machetePreRebaseHookPath.writeText(""" echo "$1 $3" >> "$machetePreRebaseHookOutputPath" """)
    machetePreRebaseHookPath.makeExecutable()

    // syncCurrentToParentByRebase
    openGitMacheteTab()
    checkoutBranch("allow-ownership-link")
    syncCurrentToParentByRebase()
    assertSyncToParentStatus("allow-ownership-link", "InSync")

    // syncSelectedToParentByRebase
    syncSelectedToParentByRebase("build-chain")
    assertSyncToParentStatus("build-chain", "InSync")

    assertEquals(
      "refs/heads/develop allow-ownership-link\nrefs/heads/allow-ownership-link build-chain\n",
      machetePreRebaseHookOutputPath.readText(),
    )
  }

  @Test
  fun pullBranch() = uiTest {
    // pullCurrentBranch
    openGitMacheteTab()
    // Remote tracking data is purposefully NOT set for this branch.
    // Our plugin should infer the remote tracking branch based on its name.
    checkoutBranch("allow-ownership-link")
    pullCurrent()
    assertLocalAndRemoteBranchesAreEqual("allow-ownership-link")
    assertNoUncommittedChanges()

    // pullNonCurrentBranch
    openGitMacheteTab()
    pullSelected("update-icons")
    assertLocalAndRemoteBranchesAreEqual("update-icons")
    assertNoUncommittedChanges()

    // syncSelectedToParentByMerge
    openGitMacheteTab()
    syncSelectedToParentByMerge("call-ws")
    assertSyncToParentStatus("call-ws", "InSync")
  }

  @Test fun resetBranchToRemote() = uiTest {
    // resetCurrentBranchToRemote
    openGitMacheteTab()
    checkoutBranch("hotfix/add-trigger")
    resetCurrentToRemote()
    assertLocalAndRemoteBranchesAreEqual("hotfix/add-trigger")
    assertNoUncommittedChanges()
    val currentBranchName = getCurrentBranchName()
    assertEquals("hotfix/add-trigger", currentBranchName)

    // resetNonCurrentBranchToRemote
    openGitMacheteTab()
    resetToRemote("update-icons")
    assertLocalAndRemoteBranchesAreEqual("update-icons")
    assertNoUncommittedChanges()
  }

  @Test fun squashBranch() = uiTest {
    // squashCurrentBranch
    openGitMacheteTab()
    toggleListingCommits()
    val branchRowsCount = refreshModelAndGetRowCount()
    assertEquals(18, branchRowsCount)
    checkoutBranch("call-ws")
    squashCurrent()
    acceptSquash()

    // call-ws had 3 commits before the squash
    var managedBranchesAndCommits = refreshModelAndGetManagedBranchesAndCommits()
    assertEquals(
      listOf(
        "develop",
        "Allow ownership links",
        "allow-ownership-link",
        "Use new icons",
        "1st round of fixes",
        "update-icons",
        "Build arbitrarily long chains",
        "Use new icons",
        "1st round of fixes",
        "build-chain",
        "Call web service",
        "call-ws",
        "master",
        "HOTFIX Add the trigger - fixes",
        "HOTFIX Add the trigger",
        "hotfix/add-trigger",
      ),
      managedBranchesAndCommits.toList(),
    )

    assertEquals(16, managedBranchesAndCommits.size)

    // squashNonCurrentBranch
    squashSelected("hotfix/add-trigger")
    acceptSquash()
    Thread.sleep(1000) // to prevent #1079, mostly happening locally (rather on CI)

    managedBranchesAndCommits = refreshModelAndGetManagedBranchesAndCommits()
    assertEquals(
      listOf(
        "develop",
        "Allow ownership links",
        "allow-ownership-link",
        "Use new icons",
        "1st round of fixes",
        "update-icons",
        "Build arbitrarily long chains",
        "Use new icons",
        "1st round of fixes",
        "build-chain",
        "Call web service",
        "call-ws",
        "master",
        // hotfix/add-trigger had 2 commits before the squash
        "HOTFIX Add the trigger",
        "hotfix/add-trigger",
      ),
      managedBranchesAndCommits.toList(),
    )
  }
}
