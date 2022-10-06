package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.panel
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot
import com.virtuslab.gitmachete.backend.api.IRemoteTrackingBranchReference
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus
import com.virtuslab.gitmachete.frontend.actions.compat.buttonsGroupCompat
import com.virtuslab.gitmachete.frontend.actions.compat.commentCompat
import com.virtuslab.gitmachete.frontend.actions.compat.rowCompat
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.fmt
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString

enum class DivergeResolutionOption {
  FORCE_PUSH,
  RESET_ON_REMOTE
}

class DivergedFromRemoteDialog(
  project: Project,
  private val remoteBranch: IRemoteTrackingBranchReference,
  private val branch: IManagedBranchSnapshot,
  private val relationToRemote: SyncToRemoteStatus
) : DialogWrapper(project, /* canBeParent */ true) {

  private var divergeResolutionOption = DivergeResolutionOption.FORCE_PUSH

  init {
    title =
      getString("action.GitMachete.TraverseAction.dialog.diverged-from-remote.title")
    setOKButtonMnemonic('O'.code)
    super.init()
  }

  fun showAndGetThePreferredAction() =
    if (showAndGet()) {
      divergeResolutionOption
    } else {
      null
    }

  override fun createCenterPanel() = panel {
    rowCompat {
      label(
        fmt(
          getString(
            "action.GitMachete.TraverseAction.dialog.diverged-from-remote.text.HTML"
          ),
          branch.name,
          remoteBranch.name,
          getRelationToRemoteDescription()
        )
      )
    }
    buttonsGroupCompat {
      rowCompat {
        radioButton(
          getString(
            "action.GitMachete.TraverseAction.dialog.diverged-from-remote.force-push-option"
          ),
          DivergeResolutionOption.FORCE_PUSH
        ).commentCompat(getString("action.GitMachete.TraverseAction.dialog.diverged-from-remote.force-push.comment"))
      }

      rowCompat {
        radioButton(
          getString(
            "action.GitMachete.TraverseAction.dialog.diverged-from-remote.reset-option"
          ),
          DivergeResolutionOption.RESET_ON_REMOTE
        ).commentCompat(getString("action.GitMachete.TraverseAction.dialog.diverged-from-remote.reset-on-remote.comment"))
      }
    }
      .bind(
        MutableProperty(::divergeResolutionOption) { divergeResolutionOption = it },
        DivergeResolutionOption::class.java
      )
  }

  private fun getRelationToRemoteDescription(): String =
    when (relationToRemote) {
      SyncToRemoteStatus.DivergedFromAndNewerThanRemote ->
        getString("action.GitMachete.TraverseAction.dialog.diverged-from-remote.newer-than-remote")
      SyncToRemoteStatus.DivergedFromAndOlderThanRemote ->
        getString("action.GitMachete.TraverseAction.dialog.diverged-from-remote.older-than-remote")
      else -> { "" }
    }
}
