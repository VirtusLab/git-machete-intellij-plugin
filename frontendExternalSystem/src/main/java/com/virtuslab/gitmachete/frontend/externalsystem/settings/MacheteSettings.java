package com.virtuslab.gitmachete.frontend.externalsystem.settings;

import java.util.Collections;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener;
import com.intellij.openapi.project.Project;
import lombok.RequiredArgsConstructor;

@Service
public final class MacheteSettings
    extends
      AbstractExternalSystemSettings<MacheteSettings, MacheteProjectSettings, IMacheteSettingsListener> {

  protected MacheteSettings(Project project) {
    super(IMacheteSettingsListener.TOPIC, project);
    var macheteProjectSettings = new MacheteProjectSettings();
    var projectFilePath = project.getBasePath();
    assert projectFilePath != null : "Project path is null";
    macheteProjectSettings.setExternalProjectPath(projectFilePath);
    loadState(new MyState(macheteProjectSettings));
  }

  public static MacheteSettings getInstance(Project project) {
    return project.getService(MacheteSettings.class);
  }

  // TODO (#496): this method is deprecated
  @Override
  public void subscribe(ExternalSystemSettingsListener<MacheteProjectSettings> listener) {}

  @Override
  public void subscribe(ExternalSystemSettingsListener<MacheteProjectSettings> listener, Disposable parentDisposable) {
    super.subscribe(listener, parentDisposable);
  }

  @Override
  protected void copyExtraSettingsFrom(MacheteSettings settings) {}

  @Override
  protected void checkSettings(MacheteProjectSettings old, MacheteProjectSettings current) {}

  @RequiredArgsConstructor
  private static class MyState implements State<MacheteProjectSettings> {
    private final MacheteProjectSettings macheteProjectSettings;

    @Override
    public java.util.Set<MacheteProjectSettings> getLinkedExternalProjectsSettings() {
      return Collections.singleton(macheteProjectSettings);
    }

    @Override
    public void setLinkedExternalProjectsSettings(java.util.Set settings) {}
  }
}
