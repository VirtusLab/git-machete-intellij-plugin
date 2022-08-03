package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.virtuslab.gitmachete.frontend.actions.base.BaseResetBranchToRemoteAction.RESET_INFO_SHOWN
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle

@Suppress("DEPRECATION")
class ResetBranchToRemoteInfoDialog : DialogWrapper.DoNotAskOption.Adapter() {
  override fun rememberChoice(isSelected: Boolean, exitCode: Int) {
    if (exitCode == Messages.OK && isSelected) {
      PropertiesComponent.getInstance().setValue(RESET_INFO_SHOWN, true)
    }
  }

  override fun getDoNotShowMessage(): String {
    return GitMacheteBundle.getString(
      "action.GitMachete.BaseResetBranchToRemoteAction.info-dialog.do-not-show-again"
    )
  }
}
