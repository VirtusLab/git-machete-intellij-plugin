package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.*
import com.virtuslab.gitmachete.backend.api.ICommitOfManagedBranch
import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot
import com.virtuslab.gitmachete.frontend.actions.compat.buttonsGroupCompat
import com.virtuslab.gitmachete.frontend.actions.compat.commentCompat
import com.virtuslab.gitmachete.frontend.actions.compat.rowCompat
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.fmt
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString
import org.apache.commons.text.StringEscapeUtils.escapeHtml4
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

enum class OverrideOption {
  PARENT,
  INFERRED,
  CUSTOM,
}

class OverrideForkPointDialog(
  project: Project,
  private val branch: INonRootManagedBranchSnapshot,
) : DialogWrapper(project, /* canBeParent */ true) {

  private var myOverrideOption = OverrideOption.PARENT
  private var customCommit: ICommitOfManagedBranch? = branch.forkPoint

  private val parent = branch.parent

  private fun String.escapeHtml4(): String = escapeHtml4(this)

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
    rowCompat {
      if (branch.name.escapeHtml4() != branch.name) {
        label(
          fmt(
            getString(
              "action.GitMachete.BaseOverrideForkPointAction.dialog.override-fork-point.label",
            ),
            branch.name,
          ),
        )
      } else {
        label(
          fmt(
            getString(
              "action.GitMachete.BaseOverrideForkPointAction.dialog.override-fork-point.label.HTML",
            ),
            branch.name,
          ),
        )
      }
    }

    val radioButtonsGroup = buttonsGroupCompat {
      rowCompat {
        radioButton(
          fmt(
            getString(
              "action.GitMachete.BaseOverrideForkPointAction.dialog.override-fork-point.radio-button.parent",
            ),
            parent.name,
          ),
          OverrideOption.PARENT,
        )
          .commentCompat(escapeHtml4(parent.pointedCommit.shortMessage))
      }

      val forkPoint = branch.forkPoint
      if (forkPoint != null) {
        rowCompat {
          radioButton(
            fmt(
              getString(
                "action.GitMachete.BaseOverrideForkPointAction.dialog.override-fork-point.radio-button.inferred",
              ),
              forkPoint.shortHash,
            ),
            OverrideOption.INFERRED,
          )
            .commentCompat(escapeHtml4(forkPoint.shortMessage))
        }
      }

      val commits = branch.commitsUntilParent + listOf(branch.forkPoint, parent.pointedCommit)
      val comboItems = commits.filterNotNull().distinct().reversed()
      if (comboItems.isNotEmpty()) {
        rowCompat {
          val customCommitRadioButton: Cell<JBRadioButton> = radioButton(
            getString(
              "action.GitMachete.BaseOverrideForkPointAction.dialog.override-fork-point.radio-button.custom",
            ),
            OverrideOption.CUSTOM,
          )

          val customCommitComboBox = comboBox(
            comboItems,
            object : DefaultListCellRenderer() {
              override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean,
              ): Component {
                val commit = value as? ICommitOfManagedBranch
                if (commit != null) {
                  text = "<html><tt>[${commit.shortHash}]</tt> ${escapeHtml4(commit.shortMessage)}</html>"
                }
                return this
              }
            },
          )

          customCommitComboBox.enabledIf(customCommitRadioButton.selected).bindItem(
            MutableProperty(::customCommit) { customCommit = it },
          )
        }
      }
    }

    radioButtonsGroup.bind(
      MutableProperty(::myOverrideOption) { myOverrideOption = it },
      OverrideOption::class.java,
    )
  }
}
