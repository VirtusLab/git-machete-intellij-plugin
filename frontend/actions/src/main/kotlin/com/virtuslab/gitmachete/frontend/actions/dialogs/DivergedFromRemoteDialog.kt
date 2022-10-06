package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.panel
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot
import com.virtuslab.gitmachete.backend.api.IRemoteTrackingBranchReference
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString

enum class DivergeResolutionOption {
  FORCE_PUSH,
  RESET_ON_REMOTE,
  MERGE_REMOTE_INTO_LOCAL,
  REBASE_ON_REMOTE
}

class DivergedFromRemoteDialog(
  project: Project,
  private val remoteBranch: IRemoteTrackingBranchReference,
  private val branch: IManagedBranchSnapshot,
  private val relationToRemote: SyncToRemoteStatus
) : DialogWrapper(project, /* canBeParent */ true) {

  private var divergeResolutionOption = DivergeResolutionOption.REBASE_ON_REMOTE

  init {
    title =
      getString("action.GitMachete.TraverseRepositoryAction.dialog.diverged-from-remote.title")
    setOKButtonMnemonic('O'.code)
    super.init()
  }

  val relationToRemoteDescription =
    when (relationToRemote) {
      SyncToRemoteStatus.DivergedFromAndNewerThanRemote ->
        getString("action.GitMachete.TraverseRepositoryAction.dialog.diverged-from-remote.newer-than-remote")
      SyncToRemoteStatus.DivergedFromAndOlderThanRemote ->
        getString("action.GitMachete.TraverseRepositoryAction.dialog.diverged-from-remote.older-than-remote")
      else -> { "" }
    }

  fun showAndGetThePreferredAction() =
    if (showAndGet()) {
      divergeResolutionOption
    } else {
      null
    }

  override fun createCenterPanel() = panel {
    row {
      label(
        format(
          getString(
            "action.GitMachete.TraverseRepositoryAction.dialog.diverged-from-remote.text.HTML"
          ),
          branch.name,
          remoteBranch.fullName,
          relationToRemoteDescription
        )
      )
    }
    buttonsGroup {
      row {
        radioButton(

          getString(
            "action.GitMachete.TraverseRepositoryAction.dialog.diverged-from-remote.rebase-on-remote"
          ),
          DivergeResolutionOption.REBASE_ON_REMOTE
        ).comment("action.GitMachete.TraverseRepositoryAction.dialog.diverged-from-remote.rebase-on-remote.comment")
      }

      row {
        radioButton(

          getString(
            "action.GitMachete.TraverseRepositoryAction.dialog.diverged-from-remote.merge-remote"
          ),
          DivergeResolutionOption.MERGE_REMOTE_INTO_LOCAL
        ).comment(getString("action.GitMachete.TraverseRepositoryAction.dialog.diverged-from-remote.merge-remote.comment"))
      }

      row {
        radioButton(

          getString(
            "action.GitMachete.TraverseRepositoryAction.dialog.diverged-from-remote.force-push"
          ),
          DivergeResolutionOption.FORCE_PUSH
        ).comment(getString("action.GitMachete.TraverseRepositoryAction.dialog.diverged-from-remote.force-push.comment"))
      }

      row {
        radioButton(
          getString(
            "action.GitMachete.TraverseRepositoryAction.dialog.diverged-from-remote.reset-on-remote"
          ),
          DivergeResolutionOption.RESET_ON_REMOTE
        ).comment(getString("action.GitMachete.TraverseRepositoryAction.dialog.diverged-from-remote.reset-on-remote.comment"))
      }
    }
      .bind(
        MutableProperty(::divergeResolutionOption) { divergeResolutionOption = it },
        DivergeResolutionOption::class.java
      )
  }
}
