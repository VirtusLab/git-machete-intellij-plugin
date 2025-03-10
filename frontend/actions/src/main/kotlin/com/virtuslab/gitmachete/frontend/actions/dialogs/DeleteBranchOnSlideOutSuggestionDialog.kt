package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.virtuslab.gitmachete.frontend.defs.GitConfigKeys.DELETE_LOCAL_BRANCH_ON_SLIDE_OUT
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.fmt
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString
import org.apache.commons.text.StringEscapeUtils
import org.checkerframework.checker.tainting.qual.Untainted
import java.awt.event.KeyEvent
import javax.swing.Action
import javax.swing.JComponent

data class SlideOutOptions(
  @get:JvmName("shouldRemember") val remember: Boolean = false,
  @get:JvmName("shouldDelete") val delete: Boolean = false,
)
class DeleteBranchOnSlideOutSuggestionDialog(project: Project, private val branchName: @Untainted String) : DialogWrapper(project, /* canBeParent */ true) {

  private var remember = false
  private var delete = false

  private fun String.escapeHtml4(): String = StringEscapeUtils.escapeHtml4(this)

  init {
    title = getString("action.GitMachete.BaseSlideOutAction.deletion-suggestion-dialog.title")
    isResizable = false
    super.init()
  }

  fun showAndGetSlideOutOptions() = if (showAndGet()) SlideOutOptions(remember, delete) else null

  override fun createActions(): Array<Action?> = emptyArray()

  override fun createCenterPanel() = panel {
    indent {
      row {
        if (branchName.escapeHtml4() != branchName) {
          label(
            fmt(
              getString(
                "action.GitMachete.BaseSlideOutAction.deletion-suggestion-dialog.note-1",
              ),
              branchName,
            ),
          )
        } else {
          label(
            fmt(
              getString(
                "action.GitMachete.BaseSlideOutAction.deletion-suggestion-dialog.note-1.HTML",
              ),
              branchName,
            ),
          )
        }
      }
      row {
        label(
          fmt(
            getString(
              "action.GitMachete.BaseSlideOutAction.deletion-suggestion-dialog.note-2",
            ),
          ),
        )
      }
    }
    row {
      button(
        getString(
          "action.GitMachete.BaseSlideOutAction.deletion-suggestion-dialog.delete-text",
        ),
      ) {
        delete = true
        doOKAction()
      }
        .applyToComponent { mnemonic = KeyEvent.VK_D }
      button(
        getString(
          "action.GitMachete.BaseSlideOutAction.deletion-suggestion-dialog.keep-text",
        ),
      ) {
        delete = false
        doOKAction()
      }
        .applyToComponent { mnemonic = KeyEvent.VK_K }
      button(
        getString(
          "action.GitMachete.BaseSlideOutAction.deletion-suggestion-dialog.cancel-text",
        ),
      ) {
        delete = true
        close(CANCEL_EXIT_CODE)
      }
        .applyToComponent { mnemonic = KeyEvent.VK_C }
    }
    row {
      checkBox(
        fmt(
          getString(
            "action.GitMachete.BaseSlideOutAction.deletion-suggestion-dialog.remember-choice.HTML",
          ),
          DELETE_LOCAL_BRANCH_ON_SLIDE_OUT,
        ),
      )
        .bindSelected(::remember)
        .applyToComponent {
          mnemonic = KeyEvent.VK_R
          isSelected = false
        }
    }
  }

  // this will lower the gap between the last row and the bottom border
  override fun createSouthPanel(): JComponent? = null
}
