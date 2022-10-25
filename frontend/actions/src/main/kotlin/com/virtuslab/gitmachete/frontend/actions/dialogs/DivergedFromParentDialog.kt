package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.panel
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot
import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString

enum class ParentDivergenceResolutionOption {
  REBASE_ON_PARENT
}

class DivergedFromParentDialog(
  project: Project,
  private val parentBranch: IManagedBranchSnapshot,
  private val branch: INonRootManagedBranchSnapshot
) : DialogWrapper(project, /* canBeParent */ true) {

  private var divergenceResolutionOption = ParentDivergenceResolutionOption.REBASE_ON_PARENT

  init {
    title =
      getString("action.GitMachete.TraverseAction.dialog.diverged-from-parent.title")
    setOKButtonMnemonic('O'.code)
    super.init()
  }

  fun showAndGetThePreferredAction() =
    if (showAndGet()) {
      divergenceResolutionOption
    } else {
      null
    }

  override fun createCenterPanel() = panel {
    row {
      label(
        format(
          getString(
            "action.GitMachete.TraverseAction.dialog.diverged-from-parent.text.HTML"
          ),
          branch.name,
          parentBranch.name
        )
      )
    }
    buttonsGroup {
      row {
        radioButton(
          getString(
            "action.GitMachete.TraverseAction.dialog.diverged-from-parent.rebase-on-parent"
          ),
          ParentDivergenceResolutionOption.REBASE_ON_PARENT
        )
      }
    }
      .bind(
        MutableProperty(::divergenceResolutionOption) { divergenceResolutionOption = it },
        ParentDivergenceResolutionOption::class.java
      )
  }
}
