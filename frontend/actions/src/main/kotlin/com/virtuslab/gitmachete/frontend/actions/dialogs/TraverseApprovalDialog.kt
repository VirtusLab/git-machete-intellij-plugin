package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DoNotAskOption
import com.intellij.openapi.ui.Messages
import com.virtuslab.gitmachete.frontend.actions.traverse.TraverseAction
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString

class TraverseApprovalDialog(val project: Project) : DoNotAskOption.Adapter() {

  override fun rememberChoice(isSelected: Boolean, exitCode: Int) {
    if (exitCode == Messages.OK && isSelected) {
      PropertiesComponent.getInstance(project).setValue(TraverseAction.SHOW_TRAVERSE_APPROVAL, false, /* defaultValue */ true)
    }
  }

  override fun getDoNotShowMessage(): String {
    return getString("action.GitMachete.TraverseAction.dialog.do-not-show-again")
  }
}
