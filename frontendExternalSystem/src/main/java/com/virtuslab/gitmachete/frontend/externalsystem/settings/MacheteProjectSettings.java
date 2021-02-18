package com.virtuslab.gitmachete.frontend.externalsystem.settings;

import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import lombok.val;

public class MacheteProjectSettings extends ExternalProjectSettings {
  @Override
  @SuppressWarnings("superClone")
  public ExternalProjectSettings clone() {
    val macheteProjectSettings = new MacheteProjectSettings();
    copyTo(macheteProjectSettings);
    return macheteProjectSettings;
  }
}
