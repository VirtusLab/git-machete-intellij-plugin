package com.virtuslab.gitmachete.frontend.externalsystem.project;

import java.io.File;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.externalsystem.MacheteProjectService;
import com.virtuslab.gitmachete.frontend.externalsystem.settings.MacheteExecutionSettings;

public class MacheteProjectResolver implements ExternalSystemProjectResolver<MacheteExecutionSettings> {

  @Override
  public DataNode<ProjectData> resolveProjectInfo(ExternalSystemTaskId id,
      String projectPath,
      boolean isPreviewMode,
      @Nullable MacheteExecutionSettings settings,
      ExternalSystemTaskNotificationListener listener)
      throws ExternalSystemException, IllegalArgumentException, IllegalStateException {

    System.out.println(":tada:");

    String projectName = new File(projectPath).getName();
    ProjectData projectData = new ProjectData(MacheteProjectService.SYSTEM_ID, projectName, projectPath, projectPath);
    return new DataNode<>(ProjectKeys.PROJECT, projectData, /* parent */ null);
  }

  @Override
  public boolean cancelTask(ExternalSystemTaskId taskId, ExternalSystemTaskNotificationListener listener) {
    return false;
  }
}
