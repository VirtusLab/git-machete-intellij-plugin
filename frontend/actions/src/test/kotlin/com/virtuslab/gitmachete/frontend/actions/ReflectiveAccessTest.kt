package com.virtuslab.gitmachete.frontend.actions

import com.intellij.openapi.project.Project
import com.virtuslab.gitmachete.frontend.actions.base.BaseSlideInBelowAction
import com.virtuslab.gitmachete.frontend.actions.dialogs.MyGitNewBranchDialog
import git4idea.repo.GitRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

// Check if build target is still compatible with the reflective access.
class ReflectiveAccessTest {
  @Test
  fun testCreateGit4IdeaOptions() {
    MyGitNewBranchDialog.Companion.createGit4IdeaOptions("name", checkout = true, reset = true, setTracking = true)
  }

  @Test
  fun testCreateGitBranchCheckoutOperation() {
    val project = mock<Project>()
    val gitRepository = mock<GitRepository>()
    BaseSlideInBelowAction.createGitBranchCheckoutOperation(project, gitRepository)
  }
}
