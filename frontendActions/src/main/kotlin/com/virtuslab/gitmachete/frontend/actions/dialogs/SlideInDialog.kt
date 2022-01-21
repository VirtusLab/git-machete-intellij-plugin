package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.ui.layout.panel
import com.virtuslab.branchlayout.api.IBranchLayout
import com.virtuslab.branchlayout.api.IBranchLayoutEntry
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString
import java.awt.event.KeyEvent
import javax.swing.JCheckBox
import kotlin.apply
import kotlin.text.isEmpty
import kotlin.text.trim

data class SlideInOptions(
    val name: String,
    @get:JvmName("shouldReattach") val reattach: Boolean = true
)

class SlideInDialog(project: Project, val branchLayout: IBranchLayout, val parentName: String) :
    DialogWrapper(project, /* canBeParent */ true) {

  // this field is only ever meant to be written on UI thread
  var branchName = ""
  var reattach = false
  var reattachCheckbox: JCheckBox? = null
  val rootNames = branchLayout.rootEntries.map { it.name }

  init {
    title = getString("action.GitMachete.BaseSlideInBranchBelowAction.dialog.slide-in.title")
    setOKButtonText(
        getString("action.GitMachete.BaseSlideInBranchBelowAction.dialog.slide-in.ok-button"))
    setOKButtonMnemonic('S'.code)
    super.init()
  }

  fun showAndGetBranchName() =
      if (showAndGet()) SlideInOptions(branchName.trim(), reattach) else null

  override fun createCenterPanel() = panel {
    row(getString("action.GitMachete.BaseSlideInBranchBelowAction.dialog.slide-in.label.parent")) {
      label(parentName, bold = true)
    }
    row {
      label(
          getString(
              "action.GitMachete.BaseSlideInBranchBelowAction.dialog.slide-in.label.branch-name"))
    }
    row {
      textField(::branchName, { branchName = it })
          .focused()
          .withValidationOnApply(validateBranchName())
          .apply { startTrackingValidationIfNeeded() }
    }
    row {
      reattachCheckbox =
          checkBox(
                  getString(
                      "action.GitMachete.BaseSlideInBranchBelowAction.dialog.slide-in.checkbox.reattach"),
                  ::reattach)
              .component
              .apply {
                mnemonic = KeyEvent.VK_R
                isEnabled = false
                isSelected = false
              }
    }
  }

  private fun validateBranchName():
      ValidationInfoBuilder.(javax.swing.JTextField) -> ValidationInfo? = {
    val insertedText = it.text
    val errorInfo = git4idea.validators.checkRefName(insertedText)
    if (errorInfo != null) error(errorInfo.message)
    else if (insertedText == parentName) {
      error(
          getString(
              "action.GitMachete.BaseSlideInBranchBelowAction.dialog.slide-in.error.slide-in-under-itself"))
    } else {
      val entryByName = branchLayout.findEntryByName(insertedText)
      if (entryByName.map(isDescendantOf(presumedDescendantName = parentName)).getOrElse(false)) {
        error(
            getString(
                "action.GitMachete.BaseSlideInBranchBelowAction.dialog.slide-in.error.slide-in-under-its-descendant"))
      } else {
        if (insertedText in rootNames) { // the provided branch name refers to the root entry
          reattachCheckbox?.isEnabled = false
          reattachCheckbox?.isSelected = true
        } else {
          val existsAndHasAChild = entryByName.orNull?.children?.nonEmpty() ?: false
          reattachCheckbox?.isEnabled = existsAndHasAChild
          reattachCheckbox?.isSelected =
              (reattachCheckbox?.isSelected ?: false) && existsAndHasAChild
        }

        null
      }
    }
  }

  private fun CellBuilder<javax.swing.JTextField>.startTrackingValidationIfNeeded() {
    if (branchName.isEmpty()) {
      component.document.addDocumentListener(
          object : DocumentAdapter() {
            override fun textChanged(e: javax.swing.event.DocumentEvent) {
              startTrackingValidation()
              component.document.removeDocumentListener(this)
            }
          })
    } else {
      startTrackingValidation()
    }
  }

  private fun isDescendantOf(presumedDescendantName: String): (IBranchLayoutEntry) -> Boolean {
    return fun(presumedAncestorEntry: IBranchLayoutEntry): Boolean {
      if (presumedAncestorEntry.children.exists { it.name == presumedDescendantName }) {
        return true
      } else {
        return presumedAncestorEntry.children.exists(isDescendantOf(presumedDescendantName))
      }
    }
  }
}
