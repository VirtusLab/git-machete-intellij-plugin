package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.dsl.builder.panel
import com.virtuslab.gitmachete.frontend.actions.base.BaseResetToRemoteAction
import com.virtuslab.gitmachete.frontend.actions.compat.rowCompat
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString
import org.checkerframework.checker.tainting.qual.Tainted
import java.awt.Dimension

class ResetInfoDialog(project: Project, private val content: @Tainted String) :
  DialogWrapper(project, /* canBeParent */ true) {

  init {
    title = getString("action.GitMachete.BaseResetToRemoteAction.info-dialog.title")
    setOKButtonMnemonic('O'.code)
    setDoNotAskOption(createDoNotAskOptionAdapter(project))
    super.init()
  }

  override fun createCenterPanel() = panel {
    rowCompat {
      label(content).applyToComponent {
        preferredSize = Dimension(480, 200)
      }
    }
  }

  companion object {
    private fun createDoNotAskOptionAdapter(project: Project) =
      object : com.intellij.openapi.ui.DoNotAskOption.Adapter() {
        override fun rememberChoice(isSelected: Boolean, exitCode: Int) {
          if (exitCode == Messages.OK && isSelected) {
            PropertiesComponent.getInstance(project).setValue(BaseResetToRemoteAction.SHOW_RESET_INFO, false, /* defaultValue */ true)
          }
        }
      }
  }
}
