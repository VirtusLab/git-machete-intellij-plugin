package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.dsl.builder.panel
import com.virtuslab.gitmachete.frontend.actions.compat.rowCompat
import org.checkerframework.checker.tainting.qual.Tainted
import org.checkerframework.checker.tainting.qual.Untainted
import java.awt.Dimension

class InfoDialog(
  project: Project,
  title: @Untainted String,
  private val content: @Tainted String,
  propertyKey: @Untainted String
) :
  DialogWrapper(project, /* canBeParent */ true) {

  init {
    this.title = title
    setOKButtonMnemonic('O'.code)
    setDoNotAskOption(createDoNotAskOptionAdapter(project, propertyKey))
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
    private fun createDoNotAskOptionAdapter(project: Project, propertyKey: @Untainted String) =
      object : com.intellij.openapi.ui.DoNotAskOption.Adapter() {
        override fun rememberChoice(isSelected: Boolean, exitCode: Int) {
          if (exitCode == Messages.OK && isSelected) {
            PropertiesComponent.getInstance(project).setValue(propertyKey, false, /* defaultValue */ true)
          }
        }
      }
  }
}
