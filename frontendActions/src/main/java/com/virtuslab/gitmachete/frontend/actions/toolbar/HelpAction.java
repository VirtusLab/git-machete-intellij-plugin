package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.dialogs.GraphTableDialog;

public class HelpAction extends DumbAwareAction {

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    GraphTableDialog.Companion.ofDemoRepository().show();
  }
}
