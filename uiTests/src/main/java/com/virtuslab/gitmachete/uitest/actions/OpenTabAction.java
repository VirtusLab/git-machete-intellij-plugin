package com.virtuslab.gitmachete.uitest.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.ui.api.table.BaseGraphTable;
import com.virtuslab.gitmachete.frontend.ui.impl.root.GitMachetePanel;

/**
 * This action is only used for UI tests.
 *
 * Note that this action could theoretically remain a part of the main (published) plugin,
 * but this would also mean that we couldn't use certain practices that are acceptable in test code,
 * but would be highly questionable/dangerous/unmaintainable in production code.
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

    var project = e.getProject();
    assert project != null : "Project not found";
    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
    ToolWindow toolWindow = toolWindowManager.getToolWindow(ToolWindowId.VCS);
    assert toolWindow != null : "VCS tool window does not exist";

    toolWindow.activate(() -> {
      ContentManager contentManager = toolWindow.getContentManager();
      Content tab = contentManager.findContent("Git Machete");

      contentManager.setSelectedContentCB(tab, /* requestFocus */ true)
          .doWhenDone(() -> LOG.info("Opened Git Machete tab: " + tab.getDisplayName()));

      GitMachetePanel panel = (GitMachetePanel) tab.getComponent();
      BaseGraphTable graphTable = panel.getGraphTable();
      graphTable.queueRepositoryUpdateAndModelRefresh(
          /* doOnUIThreadWhenDone */ () -> LOG.info("Row count: " + graphTable.getModel().getRowCount()));
    });
  }
}
