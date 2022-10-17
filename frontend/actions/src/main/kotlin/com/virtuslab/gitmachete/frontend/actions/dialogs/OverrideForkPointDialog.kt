package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.panel
import com.virtuslab.gitmachete.backend.api.ICommitOfManagedBranch
import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

enum class OverrideOption {
  PARENT,
  INFERRED,
  CUSTOM
}

class OverrideForkPointDialog(
  project: Project,
  private val branch: INonRootManagedBranchSnapshot
) : DialogWrapper(project, /* canBeParent */ true) {

  private var myOverrideOption = OverrideOption.PARENT

  private var customCommit: ICommitOfManagedBranch? = branch.forkPoint

  private val parent = branch.parent

  init {
    title =
      getString("action.GitMachete.BaseOverrideForkPointAction.dialog.override-fork-point.title")
    setOKButtonMnemonic('O'.code)
    super.init()
  }

  fun showAndGetSelectedCommit() =
    if (showAndGet()) {
      when (myOverrideOption) {
        OverrideOption.PARENT -> parent.pointedCommit
        OverrideOption.INFERRED -> branch.forkPoint
        OverrideOption.CUSTOM -> customCommit
      }
    } else {
      null
    }

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
            parent.name
          ),
          OverrideOption.PARENT
        )
          .comment(parent.pointedCommit.shortMessage)
      }

      val thisRowComment = branch.forkPoint?.shortMessage ?: "cannot resolve commit message"
      val radioButtonComment = branch.forkPoint?.shortHash ?: "cannot resolve commit hash"

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

      row {
        radioButton(
          format(
            getString(
              "action.GitMachete.BaseOverrideForkPointAction.dialog.override-fork-point.radio-button.custom"
            ),
            radioButtonComment
          ),
          OverrideOption.CUSTOM
        )

        val mutableList = branch.commitsUntilParent.toMutableList()
        mutableList.remove(branch.forkPoint)
        mutableList.remove(parent.pointedCommit)
        val items = (listOf(branch.forkPoint, parent.pointedCommit) + mutableList.reversed()).filterNotNull()

        comboBox(
          items,
          object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
              list: JList<*>?,
              value: Any?,
              index: Int,
              isSelected: Boolean,
              cellHasFocus: Boolean
            ): Component {
              val commit: ICommitOfManagedBranch = value as ICommitOfManagedBranch
              text = "<html><tt>[${commit.shortHash}]</tt> ${commit.shortMessage}</html>"
              return this
            }
          }
        ).bindItem(
          MutableProperty(::customCommit) { customCommit = it }
        )
      }
    }
      .bind(
        MutableProperty(::myOverrideOption) { myOverrideOption = it },
        OverrideOption::class.java
      )
  }
}
