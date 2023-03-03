package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.openapi.ui.messages.MessageDialog
import com.intellij.util.ui.JBUI
import com.virtuslab.gitmachete.frontend.icons.MacheteIcons
import javax.swing.border.EmptyBorder

fun pushInfo(text: String) = MessageDialog(
  /* message */ text,
  /* title */ "ignore",
  /* options */ emptyArray(),
  /* defaultOptionIndex */ -1,
  /* icon */ MacheteIcons.LOGO,
).contentPanel.apply {
  border = JBUI.Borders.compound(
    JBUI.Borders.emptyTop(12),
    JBUI.Borders.customLineBottom(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()),
    EmptyBorder(JBUI.CurrentTheme.ActionsList.cellPadding()),
  )
}
