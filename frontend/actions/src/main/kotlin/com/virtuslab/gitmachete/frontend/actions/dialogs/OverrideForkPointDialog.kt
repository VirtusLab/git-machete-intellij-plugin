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
        OverrideOption.INFERRED -> branch.forkPoint.orNull
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
      row {
        radioButton(
          format(
            getString(
              "action.GitMachete.BaseOverrideForkPointAction.dialog.override-fork-point.radio-button.inferred"
            ),
            branch.forkPoint
              .map { it.shortHash }
              .getOrElse { "cannot resolve commit hash" }
          ),
          OverrideOption.INFERRED
        )
          .comment(
            branch.forkPoint
              .map { it.shortMessage }
              .getOrElse { "cannot resolve commit message" }
          )
      }
    }
      .bind(
        MutableProperty(::myOverrideOption) { myOverrideOption = it },
        OverrideOption::class.java
      )
  }
}
