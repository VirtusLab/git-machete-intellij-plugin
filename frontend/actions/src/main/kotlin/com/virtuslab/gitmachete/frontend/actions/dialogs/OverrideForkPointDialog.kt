package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.panel
import com.virtuslab.gitmachete.backend.api.ICommitOfManagedBranch
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot
import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString
import java.awt.Color
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList
import javax.swing.UIManager

class OverrideForkPointDialog(
  project: Project,
  private val parentBranch: IManagedBranchSnapshot,
  private val branch: INonRootManagedBranchSnapshot
) : DialogWrapper(project, /* canBeParent */ true) {

  private var customCommit: ICommitOfManagedBranch? = parentBranch.pointedCommit

  init {
    title =
      getString("action.GitMachete.BaseOverrideForkPointAction.dialog.override-fork-point.title")
    setOKButtonMnemonic('O'.code)
    super.init()
  }

  fun showAndGetSelectedCommit() =
    if (showAndGet()) {
      customCommit!!
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
    row {
      label(
        format(
          getString(
            "action.GitMachete.BaseOverrideForkPointAction.dialog.override-fork-point.radio-button.parent"
          ),
          parentBranch.name
        )
      )
    }

    row {
      label(
        format(
          getString(
            "action.GitMachete.BaseOverrideForkPointAction.dialog.override-fork-point.radio-button.inferred"
          )
        )
      )
    }

    row("The fork point commit:") {
      comboBox<ICommitOfManagedBranch?>(
        (branch.commits.toMutableList() + parentBranch.pointedCommit + branch.forkPoint).filterNotNull(),
        object : DefaultListCellRenderer() {
          private val defaultBackground = UIManager.get("List.background") as Color
          override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
          ): Component {
            val commit: ICommitOfManagedBranch? = value as ICommitOfManagedBranch?
            componentOrientation = list!!.componentOrientation

            var bg: Color? = null
            var fg: Color? = null

            val dropLocation = list.dropLocation
            var customIsSelected = isSelected
            if (dropLocation != null && !dropLocation.isInsert && dropLocation.index == index) {
              bg = list.selectionBackground
              fg = list.selectionForeground
              customIsSelected = true
            }

            if (customIsSelected) {
              background = bg ?: list.selectionBackground
              foreground = fg ?: list.selectionForeground
            } else {
              background = list.background
              foreground = list.foreground
            }

            icon = null

            isEnabled = list.isEnabled
            font = list.font

            text = if (commit != null) {
              var prefix =

                if (parentBranch.pointedCommit.shortHash.equals(commit.shortHash)) {
                  "parent pointed "
                } else if (branch.forkPoint?.shortHash.equals(commit.shortHash)) {
                  "branch fork point "
                } else {
                  ""
                }

              "$prefix[${commit.shortHash}] [${commit.shortMessage}]"
            } else {
              ""
            }

            if (!customIsSelected) {
              setBackground(if (index % 2 == 0) background else defaultBackground)
            }

            return this
          }
        }

      ).bindItem(
        MutableProperty(::customCommit) {
          customCommit = it
        }
      )
    }
  }
}
