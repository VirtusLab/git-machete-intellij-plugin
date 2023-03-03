package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.validation.DialogValidationRequestor
import com.intellij.openapi.util.Disposer
import com.intellij.ui.EditorTextComponent
import com.intellij.ui.EditorTextField
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle

// TODO (#1604): remove this class and replace with com.intellij.openapi.ui.validation.WHEN_TEXT_FIELD_TEXT_CHANGED
object GitNewBranchDialogCompat {

  fun conflictsWithLocalBranchDirectory(directories: Set<String>, inputString: String): ValidationInfo? {
    if (directories.contains(inputString)) {
      return ValidationInfo(GitMacheteBundle.fmt("Branch name {0} clashes with local branch directory with the same name", inputString))
    }
    return null
  }

  val WHEN_TEXT_FIELD_TEXT_CHANGED = DialogValidationRequestor.WithParameter<EditorTextField> { textComponent ->
    DialogValidationRequestor { parentDisposable, validate ->
      textComponent.whenDocumentChanged(parentDisposable) {
        validate()
      }
    }
  }

  private fun EditorTextComponent.whenDocumentChanged(parentDisposable: Disposable? = null, listener: (DocumentEvent) -> Unit) {
    addDocumentListener(
      parentDisposable,
      object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
          listener(event)
        }
      },
    )
  }

  private fun EditorTextComponent.addDocumentListener(parentDisposable: Disposable? = null, listener: DocumentListener) {
    addDocumentListener(listener)
    parentDisposable?.also {
      Disposer.register(it, { removeDocumentListener(listener) })
    }
  }
}
