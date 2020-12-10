package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.virtuslab.gitmachete.frontend.actions.base.BaseSlideOutBranchAction.SLIDE_OUT_DELETION_SUGGESTION_REMEMBERED
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString

class DeleteBranchOnSlideOutSuggestionDialog : DialogWrapper.DoNotAskOption.Adapter() {
  override fun rememberChoice(isSelected: Boolean, exitCode: Int) {
    if (exitCode == Messages.OK && isSelected) {
      PropertiesComponent.getInstance().setValue(SLIDE_OUT_DELETION_SUGGESTION_REMEMBERED, true)
    }
  }

  override fun getDoNotShowMessage(): String {
    return getString(
        "action.GitMachete.BaseSlideOutBranchAction.deletion-suggestion-dialog.remember-choice")
  }
}
