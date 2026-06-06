package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.ide.ui.laf.darcula.ui.DarculaComboBoxUI
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.MutableCollectionComboBoxModel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.virtuslab.branchlayout.api.BranchLayout
import com.virtuslab.branchlayout.api.BranchLayoutEntry
import com.virtuslab.gitmachete.frontend.actions.common.SlideInOptions
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString
import git4idea.branch.GitBranchUtil
import git4idea.merge.GitMergeDialog
import git4idea.repo.GitRepository
import git4idea.ui.ComboBoxWithAutoCompletion
import org.apache.commons.text.StringEscapeUtils.escapeHtml4
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JTextField
import kotlin.apply

/**
* This class has been inspired by [git4idea.merge.GitMergeDialog].
* If you see any non-trivial pieces of code,
* please take a look to that class as a reference.
*/
class SlideInDialog(
  private val project: Project,
  private val branchLayout: BranchLayout,
  private val parentName: String,
  private val gitRepository: GitRepository,
) : DialogWrapper(project, /* canBeParent */ true) {

  private val rootNames = branchLayout.rootEntries.map { it.name }

  private val branchToAnnotation = branchLayout.rootEntries.flatMap(::collectEntries).map { it.name to it.customAnnotation.orEmpty() }.toMap()
  private fun collectEntries(entry: BranchLayoutEntry): List<BranchLayoutEntry> = listOf(entry) + entry.children.flatMap(::collectEntries)

  private val reattachCheckbox =
    JCheckBox(
      getString(
        "action.GitMachete.BaseSlideInBelowAction.dialog.slide-in.checkbox.reattach",
      ),
    )

  private val branchField = createBranchField()
  private val customAnnotationField = JTextField()
  private val dialogPanel = panel {
    row(getString("action.GitMachete.BaseSlideInBelowAction.dialog.slide-in.label.parent")) {
      cell(JLabel("<html><b>${escapeHtml4(parentName)}</b></html>"))
    }
    row(getString("action.GitMachete.BaseSlideInBelowAction.dialog.slide-in.label.branch-name")) {
      cell(branchField).align(AlignX.FILL)
    }
    row(getString("action.GitMachete.BaseSlideInBelowAction.dialog.slide-in.label.custom-annotation")) {
      cell(customAnnotationField).align(AlignX.FILL)
    }
    row("") {
      cell(reattachCheckbox)
    }
  }

  init {
    title = getString("action.GitMachete.BaseSlideInBelowAction.dialog.slide-in.title")
    setOKButtonText(
      getString("action.GitMachete.BaseSlideInBelowAction.dialog.slide-in.ok-button"),
    )
    updateBranchesField()
    setOKButtonMnemonic('S'.code)
    init()
    rerender()
  }

  override fun createCenterPanel() = dialogPanel

  override fun getPreferredFocusedComponent() = branchField

  override fun doValidateAll(): List<ValidationInfo> = listOf(::validateBranchName).mapNotNull { it() }

  fun getSlideInOptions(): SlideInOptions {
    val branchName = branchField.getText().orEmpty().trim()
    val customAnnotation = customAnnotationField.text.orEmpty().trim()
    return SlideInOptions(branchName, reattachCheckbox.isSelected, customAnnotation)
  }

  private fun validateBranchName(): ValidationInfo? {
    val insertedText = branchField.getText()

    if (insertedText.isNullOrEmpty()) {
      return ValidationInfo(
        getString(
          "action.GitMachete.BaseSlideInBelowAction.dialog.slide-in.no-branch-selected",
        ),
        branchField,
      )
    }

    val errorInfo = git4idea.validators.checkRefName(insertedText)
    if (errorInfo != null) {
      return ValidationInfo(errorInfo.message, branchField)
    } else if (insertedText == parentName) {
      return ValidationInfo(
        getString(
          "action.GitMachete.BaseSlideInBelowAction.dialog.slide-in.error.slide-in-under-itself",
        ),
        branchField,
      )
    } else {
      val entryByName = branchLayout.getEntryByName(insertedText)
      if (entryByName != null && isDescendantOf(presumedDescendantName = parentName)(entryByName)) {
        return ValidationInfo(
          getString(
            "action.GitMachete.BaseSlideInBelowAction.dialog.slide-in.error.slide-in-under-its-descendant",
          ),
          branchField,
        )
      } else {
        if (insertedText in rootNames) { // the provided branch name refers to the root entry
          reattachCheckbox.isEnabled = false
          reattachCheckbox.isSelected = true
        } else {
          val existsAndHasAChild = entryByName?.children?.nonEmpty() ?: false
          reattachCheckbox.isEnabled = existsAndHasAChild
          reattachCheckbox.isSelected = reattachCheckbox.isSelected && existsAndHasAChild
        }
      }
    }

    return null
  }

  private fun updateBranchesField() {
    val branches =
      GitBranchUtil.sortBranchNames(gitRepository.branches.localBranches.map { it.name })
        .filter { it != parentName }

    val model = branchField.model as? MutableCollectionComboBoxModel
    model?.update(branches)

    branchField.selectAll()
  }

  private fun createBranchField(): ComboBoxWithAutoCompletion<String> = ComboBoxWithAutoCompletion(MutableCollectionComboBoxModel(mutableListOf<String>()), project)
    .apply {
      prototypeDisplayValue = "origin/long-enough-branch-name"
      setPlaceholder(
        getString(
          "action.GitMachete.BaseSlideInBelowAction.dialog.slide-in.placeholder",
        ),
      )
      // The only non-deprecated DarculaComboBoxUI constructor takes no parameters and defaults
      // to painting the arrow button, so we flip that off via the public setter. The custom arc
      // and border-compensation insets from the previous wiring no longer have a public knob;
      // the visual difference is negligible inside this small inline editor.
      setUI(DarculaComboBoxUI().apply { isPaintArrowButton = false })
      addDocumentListener(
        object : DocumentListener {
          override fun documentChanged(event: DocumentEvent) {
            startTrackingValidation()
            branchToAnnotation[branchField.getText()]?.also {
              customAnnotationField.text = it
            }
          }
        },
      )
    }

  private fun isDescendantOf(presumedDescendantName: String): (BranchLayoutEntry) -> Boolean = fun(presumedAncestorEntry: BranchLayoutEntry): Boolean = if (presumedAncestorEntry.children.exists { it.name == presumedDescendantName }) {
    true
  } else {
    presumedAncestorEntry.children.exists(isDescendantOf(presumedDescendantName))
  }

  private fun rerender() {
    window.pack()
    window.revalidate()
    pack()
    repaint()
  }

  companion object {
    val LOG = logger<GitMergeDialog>()
  }
}
