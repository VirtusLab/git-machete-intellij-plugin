package com.virtuslab.gitmachete.frontend.externalsystem.project;

import java.io.File;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import io.vavr.control.Option;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.externalsystem.MacheteProjectService;
import com.virtuslab.gitmachete.frontend.externalsystem.settings.MacheteExecutionSettings;
import com.virtuslab.gitmachete.frontend.ui.providerservice.GraphTableProvider;

@CustomLog
public class MacheteProjectResolver implements ExternalSystemProjectResolver<MacheteExecutionSettings> {

  @Override
  public DataNode<ProjectData> resolveProjectInfo(
      ExternalSystemTaskId id,
      String projectPath,
      boolean isPreviewMode,
      @Nullable MacheteExecutionSettings settings,
      ExternalSystemTaskNotificationListener listener)
      throws ExternalSystemException, IllegalArgumentException, IllegalStateException {

    val graphTableProvider = Option.of(settings)
        .map(s -> s.getProject().getService(GraphTableProvider.class));
    val hasSelectedGitMacheteTab = settings != null && hasSelectedGitMacheteTab(settings.getProject());

    if (graphTableProvider.isEmpty()) {
      LOG.warn("Graph table provider is undefined");
    } else if (hasSelectedGitMacheteTab) {
      graphTableProvider.get().getGraphTable().queueRepositoryUpdateAndModelRefresh();
    }

    String projectName = new File(projectPath).getName();
    val projectData = new ProjectData(MacheteProjectService.SYSTEM_ID, projectName, projectPath, projectPath);
    return new DataNode<>(ProjectKeys.PROJECT, projectData, /* parent */ null);
  }

  @Override
  public boolean cancelTask(ExternalSystemTaskId taskId, ExternalSystemTaskNotificationListener listener) {
    return false;
  }

  @SuppressWarnings("interning:not.interned")
  private Boolean hasSelectedGitMacheteTab(Project project) {
    val toolWindowManager = ToolWindowManager.getInstance(project);
    val toolWindow = toolWindowManager.getToolWindow(ToolWindowId.VCS);
    if (toolWindow != null) {
      val contentManager = toolWindow.getContentManagerIfCreated();
      if (contentManager != null) {
        val gitMacheteContent = contentManager.findContent("Git Machete");
        return contentManager.getSelectedContent() == gitMacheteContent;
      }
    }
    return false;
  }
}
