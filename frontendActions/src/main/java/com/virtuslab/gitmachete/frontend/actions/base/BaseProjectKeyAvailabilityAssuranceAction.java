package com.virtuslab.gitmachete.frontend.actions.base;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;

public abstract class BaseProjectKeyAvailabilityAssuranceAction extends DumbAwareAction {
  private final ProjectParameters projectParameters;

  public BaseProjectKeyAvailabilityAssuranceAction() {
    projectParameters = new ProjectParameters(null, true);
    ApplicationManager.getApplication().getMessageBus().connect().subscribe(ProjectManager.TOPIC,
        new ProjectManagerListenerImpl(projectParameters));
  }

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    if (this instanceof IExpectsKeyProject) {
      IExpectsKeyProject thisAction = (IExpectsKeyProject) this;
      var projectOption = thisAction.tryGetProject(anActionEvent);
      if (projectOption.isDefined()) {
        projectParameters.setProjectBelongsToMacheteInstance(projectOption.get());
      }
    }
  }

  protected boolean canBeUpdated() {
    return projectParameters.isProjectOpen();
  }

  @AllArgsConstructor
  private static class ProjectManagerListenerImpl implements ProjectManagerListener {
    private final ProjectParameters projectParameters;

    @Override
    public void projectClosed(Project project) {
      if (project.equals(projectParameters.getProjectBelongsToMacheteInstance())) {
        projectParameters.setProjectOpen(false);
      }
    }
  }

  @Data
  @AllArgsConstructor
  @ToString(callSuper = true)
  private static class ProjectParameters {
    private @Nullable Project projectBelongsToMacheteInstance;
    private boolean isProjectOpen;
  }
}
