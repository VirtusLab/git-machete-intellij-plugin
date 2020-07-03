package com.virtuslab.gitmachete.frontend.actions.base;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;

import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;

public abstract class BaseProjectKeyAvailabilityAssuranceAction extends DumbAwareAction {
  protected boolean canBeUpdated(AnActionEvent anActionEvent) {
    if (this instanceof IExpectsKeyProject) {
      IExpectsKeyProject thisAction = (IExpectsKeyProject) this;
      var projectOption = thisAction.tryGetProject(anActionEvent);
      return projectOption.isDefined();
    } else {
      return true;
    }
  }
}
