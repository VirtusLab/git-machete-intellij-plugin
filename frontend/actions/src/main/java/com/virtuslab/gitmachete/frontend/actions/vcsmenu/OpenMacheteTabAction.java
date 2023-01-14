package com.virtuslab.gitmachete.frontend.actions.vcsmenu;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.base.BaseProjectDependentAction;

@CustomLog
public class OpenMacheteTabAction extends BaseProjectDependentAction {
  @Override
  protected boolean isSideEffecting() {
    return false;
  }

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    LOG.debug("Performing");

    // Getting project from event and assigning it to variable is needed to avoid exception
    // because the data context is shared between Swing events (esp. with #2 VcsNotifier call - inside lambda)
    val project = getProject(anActionEvent);

    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
    ToolWindow toolWindow = toolWindowManager.getToolWindow(ToolWindowId.VCS);

    Runnable warnNoGit = () -> VcsNotifier.getInstance(project).notifyWarning(
        /* displayId */ null,
        getString("action.GitMachete.OpenMacheteTabAction.notification.title.could-not-open-tab.HTML"),
        getString("action.GitMachete.OpenMacheteTabAction.notification.message.no-git"));

    if (toolWindow == null) {
      LOG.debug("VCS tool window does not exist");
      warnNoGit.run();
      return;
    }

    toolWindow.activate(() -> {
      val contentManager = toolWindow.getContentManager();
      val tab = contentManager.findContent("Git Machete");

      if (tab == null) {
        LOG.debug("Machete tab does not exist");
        warnNoGit.run();
        return;
      }

      contentManager.setSelectedContentCB(tab, /* requestFocus */ true);
    });
  }
}
