package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.panel
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot
import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString

enum class OverrideOption {
  PARENT,
  INFERRED
}

class OverrideForkPointDialog(
  project: Project,
  val parentBranch: IManagedBranchSnapshot,
  val branch: INonRootManagedBranchSnapshot
) : DialogWrapper(project, /* canBeParent */ true) {

  var myOverrideOption = OverrideOption.PARENT

  init {
    title =
      getString("action.GitMachete.BaseOverrideForkPointAction.dialog.override-fork-point.title")
    setOKButtonMnemonic('O'.code)
    super.init()
  }

  fun showAndGetSelectedCommit() =
    if (showAndGet()) {
      when (myOverrideOption) {
        OverrideOption.PARENT -> parentBranch.pointedCommit
        OverrideOption.INFERRED -> branch.forkPoint
      }
    } else null

  override fun createCenterPanel() = panel {
    row {
      label(
        format(
          getString(
            "action.GitMachete.BaseOverrideForkPointAction.dialog.override-fork-point.label.HTML"
          ),
          branch.name
        )
      )
    }
    buttonsGroup {
      row {
        radioButton(
          format(
            getString(
              "action.GitMachete.BaseOverrideForkPointAction.dialog.override-fork-point.radio-button.parent"
            ),
            parentBranch.name
          ),
          OverrideOption.PARENT
        )
          .comment(parentBranch.pointedCommit.shortMessage)
      }

      var thisRowComment = "cannot resolve commit message";


      var radioButtonComment = "cannot resolve commit hash";

      if( branch.forkPoint != null) {
        thisRowComment = branch.forkPoint!!.shortMessage;
        radioButtonComment = branch.forkPoint!!.shortHash;
      }


      row {
        radioButton(
          format(
            getString(
              "action.GitMachete.BaseOverrideForkPointAction.dialog.override-fork-point.radio-button.inferred"
            ),
            radioButtonComment
          ),
          OverrideOption.INFERRED
        )
          .comment(
            thisRowComment
          )
      }
    }
      .bind(
        MutableProperty(::myOverrideOption) { myOverrideOption = it },
        OverrideOption::class.java
      )
  }
}
