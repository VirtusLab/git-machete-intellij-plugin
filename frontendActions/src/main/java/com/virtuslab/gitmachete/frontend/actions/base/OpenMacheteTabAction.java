package com.virtuslab.gitmachete.frontend.actions.base;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public class OpenMacheteTabAction extends DumbAwareAction implements IExpectsKeyProject {
  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent e) {
    LOG.debug("Performing");

    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(getProject(e));
    ToolWindow toolWindow = toolWindowManager.getToolWindow(ToolWindowId.VCS);
    if (toolWindow == null) {
      LOG.debug(() -> "VCS tool window does not exist");
      return;
    }

    toolWindow.activate(() -> {
      var contentManager = toolWindow.getContentManager();
      var tab = contentManager.findContent("Git Machete");

      contentManager.setSelectedContentCB(tab, /* requestFocus */ true);
    });
  }
}
