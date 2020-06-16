package com.virtuslab.gitmachete.frontend.externalSystem.settings;

import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;

public class MacheteProjectSettings extends ExternalProjectSettings {
  @Override
  @SuppressWarnings("superClone")
  public ExternalProjectSettings clone() {
    var macheteProjectSettings = new MacheteProjectSettings();
    copyTo(macheteProjectSettings);
    return macheteProjectSettings;
  }
}
