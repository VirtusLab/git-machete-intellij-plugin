package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.ui.DoNotAskOption
import com.intellij.openapi.ui.Messages
import com.virtuslab.gitmachete.frontend.actions.traverse.TraverseRepositoryAction
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString

class PushApprovalDialog : DoNotAskOption.Adapter() {

  override fun rememberChoice(isSelected: Boolean, exitCode: Int) {
    if (exitCode == Messages.OK && isSelected) {
      PropertiesComponent.getInstance().setValue(TraverseRepositoryAction.SHOW_PUSH_APPROVAL, false, /* defaultValue */ true)
    }
  }

  override fun getDoNotShowMessage(): String {
    return getString("action.GitMachete.TraverseRepositoryAction.dialog.do-not-show-again")
  }
}
