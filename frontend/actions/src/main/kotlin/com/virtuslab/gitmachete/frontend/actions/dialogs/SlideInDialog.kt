package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.ide.ui.laf.darcula.ui.DarculaComboBoxUI
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.MutableCollectionComboBoxModel
import com.intellij.util.ui.JBUI
import com.virtuslab.branchlayout.api.IBranchLayout
import com.virtuslab.branchlayout.api.IBranchLayoutEntry
import com.virtuslab.gitmachete.frontend.actions.common.SlideInOptions
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString
import com.virtuslab.qual.guieffect.UIThreadUnsafe
import git4idea.branch.GitBranchUtil
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.merge.GitMergeDialog
import git4idea.repo.GitRepository
import git4idea.ui.ComboBoxWithAutoCompletion
import net.miginfocom.swing.MigLayout
import java.awt.Insets
import java.util.regex.Pattern
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.apply
import net.miginfocom.layout.AC as AxisConstraint
import net.miginfocom.layout.CC as ComponentConstraint
import net.miginfocom.layout.LC as LayoutConstraint

internal const val GIT_REF_PROTOTYPE_VALUE = "origin/long-enough-branch-name"

/**
* This class has been inspired by [git4idea.merge.GitMergeDialog].
* If you see any non-trivial pieces of code,
* please take a look to that class as a reference.
*/
class SlideInDialog(
  val project: Project,
  val branchLayout: IBranchLayout,
  val parentName: String,
  val gitRepository: GitRepository
) : DialogWrapper(project, /* canBeParent */ true) {

  val rootNames = branchLayout.rootEntries.map { it.name }

  var unmergedBranches = emptyList<String>()

  val reattachCheckbox =
    JCheckBox(
      getString(
        "action.GitMachete.BaseSlideInBelowAction.dialog.slide-in.checkbox.reattach"
      )
    )

  val branchField = createBranchField()
  val innerPanel = createInnerPanel()
  val panel = createPanel()

  init {
    loadUnmergedBranchesInBackground()
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

  private fun loadUnmergedBranchesInBackground() =
    ProgressManager.getInstance()
      .run(
        object :
          Task.Backgroundable(
            project,
            getString(
              "action.GitMachete.BaseSlideInBelowAction.dialog.slide-in.progress"
            ),
            true
          ) {

          @UIThreadUnsafe
          override fun run(indicator: ProgressIndicator) {
            val root = (gitRepository.root)
            loadUnmergedBranchesForRoot(root)?.let { branches -> unmergedBranches = branches }
          }
        }
      )

  /**
   * ```
   * $ git branch --all
   * |  master
   * |  feature
   * |* checked-out
   * |+ checked-out-by-worktree
   * |  remotes/origin/master
   * |  remotes/origin/feature
   * |  remotes/origin/HEAD -> origin/master
   * ```
   */
  @UIThreadUnsafe
  private fun loadUnmergedBranchesForRoot(root: VirtualFile): List<String>? {
    var result: List<String>? = null

    val handler =
      GitLineHandler(project, root, GitCommand.BRANCH).apply {
        addParameters("--no-color", "-a", "--no-merged")
      }
    try {
      result =
        Git.getInstance()
          .runCommand(handler)
          .getOutputOrThrow()
          .lines()
          .filter { line -> !LINK_REF_REGEX.matcher(line).matches() }
          .mapNotNull { line ->
            val matcher = BRANCH_NAME_REGEX.matcher(line)
            when {
              matcher.matches() -> matcher.group(1)
              else -> null
            }
          }
    } catch (e: Exception) {
      LOG.warn("Failed to load unmerged branches for root: $root", e)
    }

    return result
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
      val entryByName = branchLayout.findEntryByName(insertedText)
      if (entryByName.map(isDescendantOf(presumedDescendantName = parentName)).getOrElse(false)) {
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
          val existsAndHasAChild = entryByName.orNull?.children?.nonEmpty() ?: false
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

    val model = branchField.model as MutableCollectionComboBoxModel
    model.update(branches)

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
        JLabel("<html><b>$parentName</b></html>"),
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
        prototypeDisplayValue = GIT_REF_PROTOTYPE_VALUE
        setPlaceholder(
          getString(
            "action.GitMachete.BaseSlideInBelowAction.dialog.slide-in.placeholder"
          )
        )
        setUI(DarculaComboBoxUI(0f, Insets(1, 0, 1, 0), false))
        addDocumentListener(
          object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
              startTrackingValidation()
            }
          }
        )
      }

  private fun isDescendantOf(presumedDescendantName: String): (IBranchLayoutEntry) -> Boolean {
    return fun(presumedAncestorEntry: IBranchLayoutEntry): Boolean {
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
    val LINK_REF_REGEX = Pattern.compile(".+\\s->\\s.+") // aka 'symrefs'
    val BRANCH_NAME_REGEX = Pattern.compile(". (\\S+)\\s*")
  }
}
