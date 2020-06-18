package com.virtuslab.gitmachete.frontend.externalsystem;

import java.io.File;

import com.intellij.execution.configurations.SimpleJavaParameters;
import com.intellij.openapi.externalSystem.ExternalSystemAutoImportAware;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver;
import com.intellij.openapi.externalSystem.service.project.autoimport.CachingExternalSystemAutoImportAware;
import com.intellij.openapi.externalSystem.task.ExternalSystemTaskManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.util.Function;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.externalsystem.project.MacheteAutoImportAware;
import com.virtuslab.gitmachete.frontend.externalsystem.project.MacheteProjectResolver;
import com.virtuslab.gitmachete.frontend.externalsystem.project.MacheteTaskManager;
import com.virtuslab.gitmachete.frontend.externalsystem.settings.IMacheteSettingsListener;
import com.virtuslab.gitmachete.frontend.externalsystem.settings.MacheteExecutionSettings;
import com.virtuslab.gitmachete.frontend.externalsystem.settings.MacheteLocalSettings;
import com.virtuslab.gitmachete.frontend.externalsystem.settings.MacheteProjectSettings;
import com.virtuslab.gitmachete.frontend.externalsystem.settings.MacheteSettings;

public final class MacheteProjectService
    implements
      ExternalSystemAutoImportAware,
      ExternalSystemManager<MacheteProjectSettings, IMacheteSettingsListener, MacheteSettings, MacheteLocalSettings, MacheteExecutionSettings> {

  public static final ProjectSystemId SYSTEM_ID = new ProjectSystemId("MACHETE");

  private final ExternalSystemAutoImportAware myAutoImportDelegate = new CachingExternalSystemAutoImportAware(
      new MacheteAutoImportAware());

  @Override
  @UIEffect
  public @Nullable String getAffectedExternalProjectPath(String changedFileOrDirPath, Project project) {
    return myAutoImportDelegate.getAffectedExternalProjectPath(changedFileOrDirPath, project);
  }

  @Override
  @UIEffect
  public java.util.List<File> getAffectedExternalProjectFiles(String projectPath, Project project) {
    return myAutoImportDelegate.getAffectedExternalProjectFiles(projectPath, project);
  }

  @Override
  public ProjectSystemId getSystemId() {
    return SYSTEM_ID;
  }

  @Override
  public Function<Project, MacheteSettings> getSettingsProvider() {
    return MacheteSettings::getInstance;
  }

  @Override
  public Function<Project, MacheteLocalSettings> getLocalSettingsProvider() {
    return MacheteLocalSettings::getInstance;
  }

  @Override
  public Function<Pair<Project, String>, MacheteExecutionSettings> getExecutionSettingsProvider() {
    return pair -> new MacheteExecutionSettings(pair.first);
  }

  @Override
  public Class<? extends ExternalSystemProjectResolver<MacheteExecutionSettings>> getProjectResolverClass() {
    return MacheteProjectResolver.class;
  }

  @Override
  public Class<? extends ExternalSystemTaskManager<MacheteExecutionSettings>> getTaskManagerClass() {
    return MacheteTaskManager.class;
  }

  @Override
  @UIEffect
  public FileChooserDescriptor getExternalProjectDescriptor() {
    return FileChooserDescriptorFactory.createSingleFolderDescriptor();
  }

  @Override
  public void enhanceRemoteProcessing(SimpleJavaParameters parameters) {
    throw new UnsupportedOperationException();
  }
}
