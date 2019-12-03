package com.virtuslab;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

public class GitMachete extends AnAction {
  public void actionPerformed(AnActionEvent event) {
    Project project = event.getProject();
    Messages.showMessageDialog(
        project, "Hello world from GM!", "Git Machete", Messages.getInformationIcon());
  }
}
