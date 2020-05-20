package com.virtuslab.gitmachete.frontend.externalsystem;

import java.util.Objects;
import java.util.stream.Collectors;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectAware;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectId;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectRefreshListener;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectReloadContext;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemRefreshStatus;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.observable.properties.AtomicBooleanProperty;
import com.intellij.openapi.project.Project;
import git4idea.GitUtil;
import io.vavr.collection.List;
import kotlin.Unit;

public class MacheteProjectAware implements ExternalSystemProjectAware {
  public static final ProjectSystemId SYSTEM_ID = new ProjectSystemId("MACHETE");

  private final Project project;
  private final AtomicBooleanProperty isReloadCompleted = new AtomicBooleanProperty(true);

  public MacheteProjectAware(Project project) {
    this.project = project;
    project.getService(MacheteProjectService.class).addServiceListener(new MacheteProjectService.IListener() {
      @Override
      public void reloadCompleted() {
        isReloadCompleted.set(true);
      }

      @Override
      public void reloadScheduled() {
        isReloadCompleted.set(false);
      }
    });
  }

  @Override
  public ExternalSystemProjectId getProjectId() {
    return new ExternalSystemProjectId(SYSTEM_ID, project.getName());
  }

  @Override
  public java.util.Set<String> getSettingsFiles() {
    return List.ofAll(GitUtil.getRepositories(project))
        .map(Objects::toString)
        .map(path -> path.concat("/.git/machete"))
        .collect(Collectors.toSet());
  }

  @SuppressWarnings("guieffect:call.invalid.ui")
  public void reloadProject(ExternalSystemProjectReloadContext context) {
    FileDocumentManager.getInstance().saveAllDocuments();
    project.getService(MacheteProjectService.class).foo();
  }

  public void refreshProject() {
    reloadProject(() -> true);
  }

  @Override
  public void subscribe(ExternalSystemProjectRefreshListener listener, Disposable disposable) {
    isReloadCompleted.afterReset(() -> {
      listener.beforeProjectRefresh();
      return Unit.INSTANCE;
    }, disposable);
    isReloadCompleted.afterSet(() -> {
      listener.afterProjectRefresh(ExternalSystemRefreshStatus.SUCCESS);
      return Unit.INSTANCE;
    }, disposable);
  }
}
