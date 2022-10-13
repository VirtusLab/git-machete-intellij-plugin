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
import java.awt.Font
import javax.swing.DefaultListCellRenderer
import javax.swing.JList
import javax.swing.UIManager

enum class OverrideOption {
  PARENT,
  INFERRED,
  CUSTOM
}

class OverrideForkPointDialog(
  project: Project,
  private val parentBranch: IManagedBranchSnapshot,
  private val branch: INonRootManagedBranchSnapshot
) : DialogWrapper(project, /* canBeParent */ true) {

  private var myOverrideOption = OverrideOption.PARENT

  private var customCommit: ICommitOfManagedBranch? = branch.forkPoint

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
        OverrideOption.CUSTOM -> customCommit!!
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
            parentBranch.name
          ),
          OverrideOption.PARENT
        )
          .comment(parentBranch.pointedCommit.shortMessage)
      }

      var thisRowComment = "cannot resolve commit message"

      var radioButtonComment = "cannot resolve commit hash"

      if (branch.forkPoint != null) {
        thisRowComment = branch.forkPoint!!.shortMessage
        radioButtonComment = branch.forkPoint!!.shortHash
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

        comboBox<ICommitOfManagedBranch?>(
          (listOf(branch.forkPoint, parentBranch.pointedCommit) + branch.commits.toMutableList()).filterNotNull(),
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
                font = Font(list.font.name, Font.BOLD, list.font.size)
              } else {
                font = list.font
                background = list.background
                foreground = list.foreground
              }

              icon = null

              isEnabled = list.isEnabled

              text = if (commit != null) {
                "[${commit.shortHash}] ${commit.shortMessage}"
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
      .bind(
        MutableProperty(::myOverrideOption) { myOverrideOption = it },
        OverrideOption::class.java
      )
  }
}
