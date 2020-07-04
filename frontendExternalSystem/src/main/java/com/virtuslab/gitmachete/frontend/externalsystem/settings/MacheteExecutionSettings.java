package com.virtuslab.gitmachete.frontend.externalsystem.settings;

import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.openapi.project.Project;
import lombok.Data;

@Data
public class MacheteExecutionSettings extends ExternalSystemExecutionSettings {
  private final Project project;
}
