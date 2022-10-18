package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.ui.DoNotAskOption
import com.intellij.openapi.ui.Messages
import com.virtuslab.gitmachete.frontend.actions.base.BaseResetToRemoteAction.SHOW_RESET_INFO

class ResetBranchToRemoteInfoDialog : DoNotAskOption.Adapter() {
  override fun rememberChoice(isSelected: Boolean, exitCode: Int) {
    if (exitCode == Messages.OK && isSelected) {
      PropertiesComponent.getInstance().setValue(SHOW_RESET_INFO, false, /* defaultValue */ true)
    }
  }
}
