package com.virtuslab.gitmachete.frontend.externalSystem.settings;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemLocalSettings;
import com.intellij.openapi.project.Project;

import com.virtuslab.gitmachete.frontend.externalSystem.MacheteProjectService;

@Service
public final class MacheteLocalSettings extends AbstractExternalSystemLocalSettings<MacheteLocalSettings.MyState> {
  protected MacheteLocalSettings(Project project) {
    super(MacheteProjectService.SYSTEM_ID, project, new MyState());
  }

  public static MacheteLocalSettings getInstance(Project project) {
    return project.getService(MacheteLocalSettings.class);
  }

  public static class MyState extends AbstractExternalSystemLocalSettings.State {}
}
