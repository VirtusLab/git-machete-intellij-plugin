package com.virtuslab.gitmachete.frontend.actions.unanchored;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getProject;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;

/**
 * This action is only used for UI tests.
 *
 * Expects DataKeys:
 * <ul>
 *  <li>{@link com.intellij.openapi.actionSystem.CommonDataKeys#PROJECT}</li>
 * </ul>
 */
@CustomLog
public class OpenTabAction extends DumbAwareAction {

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent e) {
    LOG.debug("Performing");

    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(getProject(e));
    ToolWindow toolWindow = toolWindowManager.getToolWindow(ToolWindowId.VCS);
    assert toolWindow != null : "VCS tool window does not exist";

    toolWindow.activate(() -> {
      var contentManager = toolWindow.getContentManager();
      var tab = contentManager.findContent("Git Machete");
      var tabDisplayName = tab.getDisplayName();

      contentManager.setSelectedContentCB(tab, /* requestFocus */ true).doWhenDone(
          () -> LOG.info("Opened Git Machete tab: " + tabDisplayName));
    });
  }
}
