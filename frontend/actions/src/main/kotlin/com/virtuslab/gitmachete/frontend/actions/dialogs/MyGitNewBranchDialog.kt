package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.validation.WHEN_STATE_CHANGED
import com.intellij.openapi.ui.validation.WHEN_TEXT_FIELD_TEXT_CHANGED
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.util.textCompletion.DefaultTextCompletionValueDescriptor
import com.intellij.util.textCompletion.TextCompletionProviderBase
import com.intellij.util.textCompletion.TextFieldWithCompletion
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString
import git4idea.branch.GitBranchOperationType
import git4idea.branch.GitNewBranchOptions
import git4idea.repo.GitRepository
import git4idea.validators.*
import javax.swing.JCheckBox

/**
 * This class has been inspired by [git4idea.branch.GitNewBranchOptions].
 * The main reason is that we want to rename the checkbox "Set tracking branch"
 * (which is unclear) to "Rename tracking branch".
 */
class MyGitNewBranchDialog @JvmOverloads constructor(
  private val project: Project,
  private val repositories: Collection<GitRepository>,
  dialogTitle: String,
  initialName: String?,
  private val showCheckOutOption: Boolean = true,
  private val showResetOption: Boolean = false,
  private val showRenameRemoteOption: Boolean = false,
  private val localConflictsAllowed: Boolean = false,
  private val operation: GitBranchOperationType = if (showCheckOutOption) GitBranchOperationType.CREATE else GitBranchOperationType.CHECKOUT,
) :
  DialogWrapper(project, true) {

  companion object {
    private const val NAME_SEPARATOR = '/'
  }

  private var checkout = true
  private var reset = false
  private var remote = showRenameRemoteOption
  private var branchName = initialName.orEmpty()
  private val validator = GitRefNameValidator.getInstance()

  private val localBranchDirectories = collectDirectories(collectLocalBranchNames().asIterable(), /* withTrailingSlash */false).toSet()

  init {
    title = dialogTitle
    setOKButtonText(operation.text)
    init()
  }

  fun showAndGetOptions(): GitNewBranchOptions? {
    if (!showAndGet()) return null
    return GitNewBranchOptions(validator.cleanUpBranchName(branchName).trim(), checkout, reset, remote)
  }

  override fun createCenterPanel() = panel {
    val overwriteCheckbox = JCheckBox(getString("string.GitMachete.NewBranchDialog.overwrite-checkbox"))
    row {
      cell(
        TextFieldWithCompletion(
          project,
          createBranchNameCompletion(),
          branchName,
          /* oneLineMode */ true,
          /* autoPopup */ true,
          /* forceAutoPopup */ false,
          /* showHint */ false,
        ),
      )
        .bind({ c -> c.text }, { c, v -> c.text = v }, ::branchName.toMutableProperty())
        .align(AlignX.FILL)
        .label(getString("string.GitMachete.NewBranchDialog.new-branch-name"), LabelPosition.TOP)
        .focused()
        .applyToComponent {
          selectAll()
        }
        .validationRequestor(WHEN_STATE_CHANGED(overwriteCheckbox))
        .validationRequestor(WHEN_TEXT_FIELD_TEXT_CHANGED)
        .validationOnApply(validateBranchName(/* onApply */ true, overwriteCheckbox))
        .validationOnInput(validateBranchName(/* onApply */ false, overwriteCheckbox))
    }
    row {
      if (showCheckOutOption) {
        checkBox(getString("string.GitMachete.NewBranchDialog.checkout-checkbox"))
          .bindSelected(::checkout)
      }
      if (showResetOption) {
        cell(overwriteCheckbox)
          .bindSelected(::reset)
          .applyToComponent {
            isEnabled = false
          }
          .component
      }
      if (showRenameRemoteOption) {
        checkBox(getString("string.GitMachete.NewBranchDialog.rename-remote-checkbox"))
          .bindSelected(::remote)
          .component
      }
    }
  }

  private fun createBranchNameCompletion(): BranchNamesCompletion {
    val localBranches = collectLocalBranchNames()
    val remoteBranches = collectRemoteBranchNames()
    val localDirectories = collectDirectories(localBranches.asIterable(), /* withTrailingSlash */ true)
    val remoteDirectories = collectDirectories(remoteBranches.asIterable(), /* withTrailingSlash */ true)

    val allSuggestions = mutableSetOf<String>()
    allSuggestions += localBranches
    allSuggestions += remoteBranches
    allSuggestions += localDirectories
    allSuggestions += remoteDirectories
    return BranchNamesCompletion(localDirectories.toList(), allSuggestions.toList())
  }

  private fun collectLocalBranchNames() = repositories.asSequence().flatMap { it.branches.localBranches }.map { it.name }
  private fun collectRemoteBranchNames() = repositories.asSequence().flatMap { it.branches.remoteBranches }.map { it.nameForRemoteOperations }

  private fun collectDirectories(branchNames: Iterable<String>, withTrailingSlash: Boolean): Collection<String> {
    val directories = mutableSetOf<String>()
    for (branchName in branchNames) {
      if (branchName.contains(NAME_SEPARATOR)) {
        var index = 0
        while (index < branchName.length) {
          val end = branchName.indexOf(NAME_SEPARATOR, index)
          if (end == -1) break
          directories += if (withTrailingSlash) branchName.substring(0, end + 1) else branchName.substring(0, end)
          index = end + 1
        }
      }
    }
    return directories
  }

  private fun validateBranchName(onApply: Boolean, overwriteCheckbox: JCheckBox): ValidationInfoBuilder.(TextFieldWithCompletion) -> ValidationInfo? =
    {
      // Do not change Document inside DocumentListener callback
      invokeLater {
        it.cleanBranchNameAndAdjustCursorIfNeeded()
      }

      val branchName = validator.cleanUpBranchName(it.text).trim()
      val errorInfo = (if (onApply) checkRefNameEmptyOrHead(branchName) else null)
        ?: conflictsWithRemoteBranch(repositories, branchName)
        ?: conflictsWithLocalBranchDirectory(localBranchDirectories, branchName)
      if (errorInfo != null) {
        error(errorInfo.message)
      } else {
        val localBranchConflict = conflictsWithLocalBranch(repositories, branchName)
        overwriteCheckbox.isEnabled = localBranchConflict != null

        if (localBranchConflict == null || overwriteCheckbox.isSelected) {
          null // no conflicts or ask to reset
        } else if (localBranchConflict.warning && localConflictsAllowed) {
          warning(HtmlBuilder().append(localBranchConflict.message + ".").br().append(operation.description).toString())
        } else if (showResetOption) {
          error(
            HtmlBuilder().append(localBranchConflict.message + ".").br()
              .append(getString("string.GitMachete.NewBranchDialog.overwrite-warning")).toString(),
          )
        } else {
          error(localBranchConflict.message)
        }
      }
    }

  private fun TextFieldWithCompletion.cleanBranchNameAndAdjustCursorIfNeeded() {
    if (isDisposed) return

    val initialText = text
    val initialCaret = caretModel.offset

    val fixedText = validator.cleanUpBranchNameOnTyping(initialText)

    // if the text didn't change, there's no point in updating it or cursorPosition
    if (fixedText == initialText) return

    val initialTextBeforeCaret = initialText.take(initialCaret)
    val fixedTextBeforeCaret = validator.cleanUpBranchNameOnTyping(initialTextBeforeCaret)

    val fixedCaret = fixedTextBeforeCaret.length

    text = fixedText
    caretModel.moveToOffset(fixedCaret)
  }

  private class BranchNamesCompletion(
    val localDirectories: List<String>,
    val allSuggestions: List<String>,
  ) :
    TextCompletionProviderBase<String>(
      DefaultTextCompletionValueDescriptor.StringValueDescriptor(),
      emptyList(),
      /* caseSensitive */ false,
    ),
    DumbAware {
    override fun getValues(parameters: CompletionParameters, prefix: String, result: CompletionResultSet): Collection<String> {
      return if (parameters.isAutoPopup) {
        localDirectories
      } else {
        allSuggestions
      }
    }
  }
}
