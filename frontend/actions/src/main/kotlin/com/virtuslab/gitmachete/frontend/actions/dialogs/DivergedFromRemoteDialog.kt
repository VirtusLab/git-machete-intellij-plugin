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

enum class DivergenceResolutionOption {
  DO_NOT_SYNC,
  FORCE_PUSH,
  RESET_TO_REMOTE
}

class DivergedFromRemoteDialog(
  project: Project,
  private val remoteBranch: IRemoteTrackingBranchReference,
  private val branch: IManagedBranchSnapshot,
  private val relationToRemote: SyncToRemoteStatus
) : DialogWrapper(project, /* canBeParent */ true) {

  private var divergenceResolutionOption = DivergenceResolutionOption.DO_NOT_SYNC

  init {
    title =
      getString("action.GitMachete.BaseTraverseAction.dialog.diverged-from-remote.title")
    setOKButtonMnemonic('O'.code)
    setCancelButtonText(getString("action.GitMachete.BaseTraverseAction.dialog.cancel-traverse"))
    super.init()
  }

  fun showAndGetThePreferredAction() =
    if (showAndGet()) {
      divergenceResolutionOption
    } else {
      null
    }

  override fun createCenterPanel() = panel {
    rowCompat {
      label(
        fmt(
          getString(
            "action.GitMachete.BaseTraverseAction.dialog.diverged-from-remote.text.HTML"
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
            "action.GitMachete.BaseTraverseAction.dialog.diverged-from-remote.do-not-resolve-option"
          ),
          DivergenceResolutionOption.DO_NOT_SYNC
        ).commentCompat(getString("action.GitMachete.BaseTraverseAction.dialog.diverged-from-remote.do-not-resolve-option.comment"))
      }
      when (relationToRemote) {
        SyncToRemoteStatus.DivergedFromAndOlderThanRemote ->
          rowCompat {
            radioButton(
              getString(
                "action.GitMachete.BaseTraverseAction.dialog.diverged-from-remote.reset-option"
              ),
              DivergenceResolutionOption.RESET_TO_REMOTE
            ).commentCompat(getString("action.GitMachete.BaseTraverseAction.dialog.diverged-from-remote.reset-on-remote.comment"))
          }
        SyncToRemoteStatus.DivergedFromAndNewerThanRemote ->
          rowCompat {
            radioButton(
              getString(
                "action.GitMachete.BaseTraverseAction.dialog.diverged-from-remote.force-push-option"
              ),
              DivergenceResolutionOption.FORCE_PUSH
            ).commentCompat(getString("action.GitMachete.BaseTraverseAction.dialog.diverged-from-remote.force-push.comment"))
          }

        else -> {}
      }
    }
      .bind(
        MutableProperty(::divergenceResolutionOption) { divergenceResolutionOption = it },
        DivergenceResolutionOption::class.java
      )
  }

  private fun getRelationToRemoteDescription(): String =
    when (relationToRemote) {
      SyncToRemoteStatus.DivergedFromAndNewerThanRemote ->
        getString("action.GitMachete.BaseTraverseAction.dialog.diverged-from-remote.newer-than-remote")
      SyncToRemoteStatus.DivergedFromAndOlderThanRemote ->
        getString("action.GitMachete.BaseTraverseAction.dialog.diverged-from-remote.older-than-remote")
      else -> { "" }
    }
}
