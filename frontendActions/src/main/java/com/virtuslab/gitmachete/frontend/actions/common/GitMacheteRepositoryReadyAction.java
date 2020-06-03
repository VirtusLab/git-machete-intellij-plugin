package com.virtuslab.gitmachete.frontend.actions.common;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.checkerframework.checker.guieffect.qual.UIEffect;

public abstract class GitMacheteRepositoryReadyAction extends DumbAwareAction implements IExpectsKeyGitMacheteRepository {

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    boolean isEnabled = getGitMacheteRepository(anActionEvent).isDefined();
    anActionEvent.getPresentation().setEnabled(isEnabled);

    if (!isEnabled) {
      anActionEvent.getPresentation().setDescription("Action disabled due to undefined Git Machete repository");
    }
  }

  /**
   * Bear in mind that {@link AnAction#beforeActionPerformedUpdate} is called before each action.
   * (For more details check {@link com.intellij.openapi.actionSystem.ex.ActionUtil} as well.)
   * The {@link AnActionEvent} argument passed to before-called {@link AnAction#update} is the same one that is passed here.
   * This gives us certainty that all checks from actions' update implementations will be performed
   * and all data available via data keys in those {@code update} implementations will still be available
   * in {@link GitMacheteRepositoryReadyAction#actionPerformed} implementations.
   */
  @Override
  @UIEffect
  public abstract void actionPerformed(AnActionEvent anActionEvent);
}
