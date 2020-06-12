package com.virtuslab.gitmachete.frontend.ui.api.table;

import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;

@Service
public final class GraphTableProvider {
  private final Project project;

  public GraphTableProvider(Project project) {
    this.project = project;
  }

  @UIEffect
  public Option<BaseGraphTable> getGraphTable() {
    var toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.VCS);
    if (toolWindow == null) {
      return Option.none();
    }

    var contentManager = toolWindow.getContentManager();
    var git_machete = contentManager.findContent("Git Machete");
    var component = (DataProvider) git_machete.getComponent();
    return Option.of((BaseGraphTable) component.getData(DataKeys.KEY_GRAPH_TABLE.getName()));
  }
}
