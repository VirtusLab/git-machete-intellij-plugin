package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.ide.ui.laf.darcula.ui.DarculaComboBoxUI
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.MutableCollectionComboBoxModel
import com.intellij.util.ui.JBUI
import com.virtuslab.branchlayout.api.BranchLayout
import com.virtuslab.branchlayout.api.BranchLayoutEntry
import com.virtuslab.gitmachete.frontend.actions.common.SlideInOptions
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString
import git4idea.branch.GitBranchUtil
import git4idea.merge.GitMergeDialog
import git4idea.repo.GitRepository
import git4idea.ui.ComboBoxWithAutoCompletion
import net.miginfocom.swing.MigLayout
import org.apache.commons.text.StringEscapeUtils.escapeHtml4
import java.awt.Insets
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.apply
import net.miginfocom.layout.AC as AxisConstraint
import net.miginfocom.layout.CC as ComponentConstraint
import net.miginfocom.layout.LC as LayoutConstraint

/**
* This class has been inspired by [git4idea.merge.GitMergeDialog].
* If you see any non-trivial pieces of code,
* please take a look to that class as a reference.
*/
class SlideInDialog(
  private val project: Project,
  private val branchLayout: BranchLayout,
  private val parentName: String,
  private val gitRepository: GitRepository
) : DialogWrapper(project, /* canBeParent */ true) {

  private val rootNames = branchLayout.rootEntries.map { it.name }

  private val reattachCheckbox =
    JCheckBox(
      getString(
        "action.GitMachete.BaseSlideInBelowAction.dialog.slide-in.checkbox.reattach"
      )
    )

  private val branchField = createBranchField()
  private val innerPanel = createInnerPanel()
  private val panel = createPanel()

  init {
    title = getString("action.GitMachete.BaseSlideInBelowAction.dialog.slide-in.title")
    setOKButtonText(
      getString("action.GitMachete.BaseSlideInBelowAction.dialog.slide-in.ok-button")
    )
    updateBranchesField()
    setOKButtonMnemonic('S'.code)
    init()
    rerender()
  }

  override fun createCenterPanel() = panel

  override fun getPreferredFocusedComponent() = branchField

  override fun doValidateAll(): List<ValidationInfo> =
    listOf(::validateBranchName).mapNotNull { it() }

  fun getSlideInOptions(): SlideInOptions {
    val branchName = branchField.getText().orEmpty().trim()
    return SlideInOptions(branchName, reattachCheckbox.isSelected)
  }

  private fun validateBranchName(): ValidationInfo? {
    val insertedText = branchField.getText()

    if (insertedText.isNullOrEmpty()) {
      return ValidationInfo(
        getString(
          "action.GitMachete.BaseSlideInBelowAction.dialog.slide-in.no-branch-selected"
        ),
        branchField
      )
    }

    val errorInfo = git4idea.validators.checkRefName(insertedText)
    if (errorInfo != null) {
      return ValidationInfo(errorInfo.message, branchField)
    } else if (insertedText == parentName) {
      return ValidationInfo(
        getString(
          "action.GitMachete.BaseSlideInBelowAction.dialog.slide-in.error.slide-in-under-itself"
        ),
        branchField
      )
    } else {
      val entryByName = branchLayout.getEntryByName(insertedText)
      if (entryByName != null && isDescendantOf(presumedDescendantName = parentName)(entryByName)) {
        return ValidationInfo(
          getString(
            "action.GitMachete.BaseSlideInBelowAction.dialog.slide-in.error.slide-in-under-its-descendant"
          ),
          branchField
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

  private fun createPanel() =
    JPanel().apply {
      layout = MigLayout(LayoutConstraint().insets("0").hideMode(3), AxisConstraint().grow())

      add(innerPanel, ComponentConstraint().growX())
    }

  private fun createInnerPanel(): JPanel {
    return JPanel().apply {
      layout =
        MigLayout(
          LayoutConstraint().fillX().insets("0").gridGap("0", "0").noVisualPadding(),
          AxisConstraint().grow(100f, 1)
        )

      add(
        JLabel(
          getString(
            "action.GitMachete.BaseSlideInBelowAction.dialog.slide-in.label.parent"
          )
        ),
        ComponentConstraint().gapAfter("0").minWidth("${JBUI.scale(100)}px")
      )

      add(
        JLabel("<html><b>${escapeHtml4(parentName)}</b></html>"),
        ComponentConstraint().minWidth("${JBUI.scale(300)}px").growX().wrap()
      )

      add(
        JLabel(
          getString(
            "action.GitMachete.BaseSlideInBelowAction.dialog.slide-in.label.branch-name"
          )
        ),
        ComponentConstraint().gapAfter("0").minWidth("${JBUI.scale(100)}px")
      )

      add(branchField, ComponentConstraint().minWidth("${JBUI.scale(300)}px").growX().wrap())

      add(reattachCheckbox)
    }
  }

  private fun createBranchField() =
    ComboBoxWithAutoCompletion(MutableCollectionComboBoxModel(mutableListOf<String>()), project)
      .apply {
        prototypeDisplayValue = "origin/long-enough-branch-name"
        setPlaceholder(
          getString(
            "action.GitMachete.BaseSlideInBelowAction.dialog.slide-in.placeholder"
          )
        )
        setUI(DarculaComboBoxUI(/* arc */ 0f, Insets(1, 0, 1, 0), /* paintArrowButton */false))
        addDocumentListener(
          object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
              startTrackingValidation()
            }
          }
        )
      }

  private fun isDescendantOf(presumedDescendantName: String): (BranchLayoutEntry) -> Boolean {
    return fun(presumedAncestorEntry: BranchLayoutEntry): Boolean {
      return if (presumedAncestorEntry.children.exists { it.name == presumedDescendantName }) {
        true
      } else {
        presumedAncestorEntry.children.exists(isDescendantOf(presumedDescendantName))
      }
    }
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
