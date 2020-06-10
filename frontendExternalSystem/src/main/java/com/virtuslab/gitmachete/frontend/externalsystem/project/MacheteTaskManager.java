package com.virtuslab.gitmachete.frontend.externalsystem.project;

import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.task.ExternalSystemTaskManager;

import com.virtuslab.gitmachete.frontend.externalsystem.settings.MacheteExecutionSettings;

@SuppressWarnings("regexp")
public class MacheteTaskManager implements ExternalSystemTaskManager<MacheteExecutionSettings> {
  @Override
  public boolean cancelTask(ExternalSystemTaskId id, ExternalSystemTaskNotificationListener listener)
      throws ExternalSystemException {
    return false;
  }
}
