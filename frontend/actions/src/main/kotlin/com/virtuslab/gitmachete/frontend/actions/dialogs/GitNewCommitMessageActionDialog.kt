package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.ui.CommitMessage
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil

internal class GitNewCommitMessageActionDialog(
    val project: Project,
    val originMessage: String,
    title: String,
    val dialogLabel: String
) : DialogWrapper(project, true) {
  val commitEditor = createCommitEditor()
  var onOk: (String) -> Unit = {}

  init {
    Disposer.register(disposable, commitEditor)

    init()
    isModal = false
    this.title = title
  }

  fun show(onOk: (newMessage: String) -> Unit) {
    this.onOk = onOk
    show()
  }

  override fun createCenterPanel() =
      JBUI.Panels.simplePanel(UIUtil.DEFAULT_HGAP, UIUtil.DEFAULT_VGAP)
          .addToTop(JBLabel(dialogLabel))
          .addToCenter(commitEditor)

  override fun getPreferredFocusedComponent() = commitEditor.editorField

  override fun getDimensionServiceKey() = "Git.Rebase.Log.Action.NewCommitMessage.Dialog"

  private fun createCommitEditor(): CommitMessage {
    val editor = CommitMessage(project, false, false, true)
    editor.text = originMessage
    editor.editorField.setCaretPosition(0)
    editor.editorField.addSettingsProvider { editorEx ->
      // display at least several rows for one-line messages
      val MIN_ROWS = 3
      if ((editorEx as EditorImpl).visibleLineCount < MIN_ROWS) {
        verticalStretch = 1.5F
      }
    }
    return editor
  }

  override fun doOKAction() {
    super.doOKAction()

    onOk(commitEditor.comment)
  }
}
