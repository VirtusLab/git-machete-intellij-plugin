package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.*
import com.virtuslab.gitmachete.backend.api.ICommitOfManagedBranch
import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString
import org.apache.commons.text.StringEscapeUtils.escapeHtml4
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
    row {
      if (branch.name.escapeHtml4() != branch.name) {
        label(
          format(
            getString(
              "action.GitMachete.BaseOverrideForkPointAction.dialog.override-fork-point.label"
            ),
            branch.name
          )
        )
      } else {
        label(
          format(
            getString(
              "action.GitMachete.BaseOverrideForkPointAction.dialog.override-fork-point.label.HTML"
            ),
            branch.name
          )
        )
      }
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
          .comment(escapeHtml4(parent.pointedCommit.shortMessage))
      }

      row {
        radioButton(
          format(
            getString(
              "action.GitMachete.BaseOverrideForkPointAction.dialog.override-fork-point.radio-button.inferred"
            ),
            branch.forkPoint?.shortHash ?: "cannot resolve commit hash"
          ),
          OverrideOption.INFERRED
        )
          .comment(escapeHtml4(branch.forkPoint?.shortMessage) ?: "cannot resolve commit message")
      }

      row {
        val rb: Cell<JBRadioButton> = radioButton(
          format(
            getString(
              "action.GitMachete.BaseOverrideForkPointAction.dialog.override-fork-point.radio-button.custom"
            ),
            branch.forkPoint?.shortHash ?: "cannot resolve commit hash"
          ),
          OverrideOption.CUSTOM
        )

        val list = branch.commitsUntilParent + listOf(branch.forkPoint, parent.pointedCommit)
        val items = list.filterNotNull().distinct().reversed()

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
              text = "<html><tt>[${commit.shortHash}]</tt> ${escapeHtml4(commit.shortMessage)}</html>"
              return this
            }
          }
        ).enabledIf(rb.selected).bindItem(
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
