package com.virtuslab.gitmachete.frontend.externalSystem.settings;

import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.openapi.project.Project;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MacheteExecutionSettings extends ExternalSystemExecutionSettings {
  private final Project project;
}
