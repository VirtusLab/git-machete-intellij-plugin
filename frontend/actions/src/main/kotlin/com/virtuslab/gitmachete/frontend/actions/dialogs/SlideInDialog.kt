package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.dvcs.DvcsUtil
import com.intellij.ide.ui.laf.darcula.ui.DarculaComboBoxUI
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.MutableCollectionComboBoxModel
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.ui.JBUI
import com.virtuslab.branchlayout.api.IBranchLayout
import com.virtuslab.branchlayout.api.IBranchLayoutEntry
import com.virtuslab.gitmachete.frontend.actions.common.SlideInOptions
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.merge.GitMergeDialog
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import git4idea.ui.ComboBoxWithAutoCompletion
import java.awt.Insets
import java.util.regex.Pattern
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.apply
import net.miginfocom.layout.AC
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout

internal const val GIT_REF_PROTOTYPE_VALUE = "origin/long-enough-branch-name"

class SlideInDialog(
    val project: Project,
    val branchLayout: IBranchLayout,
    val parentName: String,
    val gitRepository: GitRepository
) : DialogWrapper(project, /* canBeParent */ true) {

  val rootNames = branchLayout.rootEntries.map { it.name }

  val repositories =
      DvcsUtil.sortRepositories(GitRepositoryManager.getInstance(project).repositories)

  var unmergedBranches = emptyList<String>()

  val reattachCheckbox =
      JCheckBox(
          getString(
              "action.GitMachete.BaseSlideInBranchBelowAction.dialog.slide-in.checkbox.reattach"))

  val branchField = createBranchField()
  val commandPanel = createCommandPanel()

  val panel = createPanel()

  init {
    loadUnmergedBranchesInBackground()
    title = getString("action.GitMachete.BaseSlideInBranchBelowAction.dialog.slide-in.title")
    setOKButtonText(
        getString("action.GitMachete.BaseSlideInBranchBelowAction.dialog.slide-in.ok-button"))
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
                          "action.GitMachete.BaseSlideInBranchBelowAction.dialog.slide-in.progress"),
                      true) {
                override fun run(indicator: ProgressIndicator) {
                  val root = (gitRepository.root)
                  loadUnmergedBranchesForRoot(root)?.let { branches -> unmergedBranches = branches }
                }
              })

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
  @RequiresBackgroundThread
  private fun loadUnmergedBranchesForRoot(root: VirtualFile): List<String>? {
    var result: List<String>? = null

    val handler =
        GitLineHandler(project, root, GitCommand.BRANCH).apply {
          // t0d0: no merged or merged too?
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

  // t0d0: now validation triggers after failed slide-out attempt - ensure it works since the
  // beginning (slide in root case)
  private fun validateBranchName(): ValidationInfo? {
    val insertedText = branchField.getText()

    if (insertedText.isNullOrEmpty()) {
      return ValidationInfo(
          getString(
              "action.GitMachete.BaseSlideInBranchBelowAction.dialog.slide-in.no-branch-selected"),
          branchField)
    }

    val errorInfo = git4idea.validators.checkRefName(insertedText)
    if (errorInfo != null) {
      return ValidationInfo(errorInfo.message, branchField)
    } else if (insertedText == parentName) {
      return ValidationInfo(
          getString(
              "action.GitMachete.BaseSlideInBranchBelowAction.dialog.slide-in.error.slide-in-under-itself"),
          branchField)
    } else {
      val entryByName = branchLayout.findEntryByName(insertedText)
      if (entryByName.map(isDescendantOf(presumedDescendantName = parentName)).getOrElse(false)) {
        return ValidationInfo(
            getString(
                "action.GitMachete.BaseSlideInBranchBelowAction.dialog.slide-in.error.slide-in-under-its-descendant"),
            branchField)
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

  private fun createPanel() =
      JPanel().apply {
        layout = MigLayout(LC().insets("0").hideMode(3), AC().grow())

        add(commandPanel, CC().growX())
      }

  private fun createCommandPanel(): JPanel {

    return JPanel().apply {
      layout =
          MigLayout(
              LC().fillX().insets("0").gridGap("0", "0").noVisualPadding(), AC().grow(100f, 1))

      add(
          JLabel(
              getString(
                  "action.GitMachete.BaseSlideInBranchBelowAction.dialog.slide-in.label.parent")),
          CC().gapAfter("0").minWidth("${JBUI.scale(100)}px"))

      add(
          JLabel("<html><b>$parentName</b></html>"),
          CC().minWidth("${JBUI.scale(300)}px").growX().wrap())

      add(
          JLabel(
              getString(
                  "action.GitMachete.BaseSlideInBranchBelowAction.dialog.slide-in.label.branch-name")),
          CC().gapAfter("0").minWidth("${JBUI.scale(100)}px"))

      add(branchField, CC().minWidth("${JBUI.scale(300)}px").growX().wrap())

      add(reattachCheckbox)
    }
  }

  private fun createBranchField() =
      ComboBoxWithAutoCompletion(MutableCollectionComboBoxModel(mutableListOf<String>()), project)
          .apply {
            prototypeDisplayValue = GIT_REF_PROTOTYPE_VALUE
            setPlaceholder(
                getString(
                    "action.GitMachete.BaseSlideInBranchBelowAction.dialog.slide-in.placeholder"))
            setUI(DarculaComboBoxUI(0f, Insets(1, 0, 1, 0), false))
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
