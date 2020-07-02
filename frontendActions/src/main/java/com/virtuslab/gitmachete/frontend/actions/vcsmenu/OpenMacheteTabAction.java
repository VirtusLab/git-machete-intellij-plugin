package com.virtuslab.gitmachete.frontend.actions.vcsmenu;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
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

    // Getting project from event and assigning it to variable is needed to avoid exception
    // according to sharing data context between Swing events (esp. with #2 VcsNotifier call - inside lambda)
    Project project = getProject(e);

    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(getProject(e));
    ToolWindow toolWindow = toolWindowManager.getToolWindow(ToolWindowId.VCS);
    if (toolWindow == null) {
      LOG.debug("VCS tool window does not exist");
      VcsNotifier.getInstance(getProject(e)).notifyWarning(
          "Could not open Git Machete tab", "Ensure that Git VCS is available");
      return;
    }

    toolWindow.activate(() -> {
      var contentManager = toolWindow.getContentManager();
      var tab = contentManager.findContent("Git Machete");

      if (tab == null) {
        LOG.debug("Machete tab does not exist");
        VcsNotifier.getInstance(project).notifyWarning(
            "Could not open Git Machete tab", "Ensure that Git VCS is available");
        return;
      }

      contentManager.setSelectedContentCB(tab, /* requestFocus */ true);
    });
  }
}
