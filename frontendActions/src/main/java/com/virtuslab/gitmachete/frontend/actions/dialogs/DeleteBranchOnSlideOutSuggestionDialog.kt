package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.layout.panel
import com.virtuslab.gitmachete.frontend.actions.base.BaseSlideOutBranchAction.DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString
import java.awt.event.KeyEvent
import javax.swing.Action
import javax.swing.JTextPane

data class SlideOutOptions(
    @get:JvmName("shouldRemember")
    val remember: Boolean = false,
    @get:JvmName("shouldDelete")
    val delete: Boolean = false)

class DeleteBranchOnSlideOutSuggestionDialog(project: Project) :
    DialogWrapper(project, /* canBeParent */ true) {

  private var remember = false
  private var delete = false

  init {
    title = getString("action.GitMachete.BaseSlideOutBranchAction.deletion-suggestion-dialog.title")
    setResizable(false)
    super.init()
  }

  fun showAndGetSlideOutOptions() = if (showAndGet()) SlideOutOptions(remember, delete) else null

  override fun createCenterPanel() =
      panel {
        row {
          createNoteOrCommentRow(
              Messages.configureMessagePaneUi(
                  JTextPane(),
                  format(
                      getString(
                          "action.GitMachete.BaseSlideOutBranchAction.deletion-suggestion-dialog.note"),
                      DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY)))
        }
      }

  override fun createActions(): Array<Action?> = emptyArray()

  override fun createSouthPanel() =
      panel {
        row {
          checkBox(
                  getString(
                      "action.GitMachete.BaseSlideOutBranchAction.deletion-suggestion-dialog.remember-choice"),
                  ::remember)
              .component
              .apply {
            mnemonic = KeyEvent.VK_R
            isEnabled = true
            isSelected = false
          }(pushX)
          cell {
            button(
                    getString(
                        "action.GitMachete.BaseSlideOutBranchAction.deletion-suggestion-dialog.delete-text")) {
                  delete = true
                  close(OK_EXIT_CODE)
                }
                .component
                .apply { mnemonic = KeyEvent.VK_D }
            button(
                    getString(
                        "action.GitMachete.BaseSlideOutBranchAction.deletion-suggestion-dialog.keep-text")) {
                  delete = false
                  close(OK_EXIT_CODE)
                }
                .component
                .apply { mnemonic = KeyEvent.VK_K }
          }
        }
      }
}
