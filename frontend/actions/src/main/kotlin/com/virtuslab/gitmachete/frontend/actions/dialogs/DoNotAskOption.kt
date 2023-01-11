package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DoNotAskOption
import com.intellij.openapi.ui.Messages

class DoNotAskOption(val project: Project, val keyName: String) : DoNotAskOption.Adapter() {

  override fun rememberChoice(isSelected: Boolean, exitCode: Int) {
    if (exitCode == Messages.OK && isSelected) {
      PropertiesComponent.getInstance(project).setValue(keyName, false, /* defaultValue */ true)
    }
  }
}
