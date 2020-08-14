package com.virtuslab.gitmachete.frontend.actions.base;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;

public abstract class BaseProjectKeyAvailabilityAssuranceAction extends DumbAwareAction {
  @Override
  @UIEffect
  public final void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    if (this instanceof IExpectsKeyProject) {
      IExpectsKeyProject thisAction = (IExpectsKeyProject) this;
      var projectOption = thisAction.tryGetProject(anActionEvent);
      if (projectOption.isEmpty()) {
        return;
      }
    }

    onUpdate(anActionEvent);
  }

  /**
   * If overridden {@code super.onUpdate(anActionEvent);} should always be called in the first line of overriding method.
   *
   * @param anActionEvent an action event
   */
  @UIEffect
  protected abstract void onUpdate(AnActionEvent anActionEvent);
}
