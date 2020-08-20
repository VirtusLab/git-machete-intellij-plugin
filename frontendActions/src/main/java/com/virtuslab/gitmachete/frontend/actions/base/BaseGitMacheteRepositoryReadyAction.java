package com.virtuslab.gitmachete.frontend.actions.base;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;

public abstract class BaseGitMacheteRepositoryReadyAction extends BaseProjectDependentAction
    implements
      IExpectsKeyGitMacheteRepository {

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);

    boolean isEnabled = getGitMacheteRepositorySnapshotWithoutLogging(anActionEvent).isDefined();
    anActionEvent.getPresentation().setEnabled(isEnabled);

    if (!isEnabled) {
      anActionEvent.getPresentation().setDescription(
          GitMacheteBundle.getString(
              "action.GitMachete.BaseGitMacheteRepositoryReadyAction.description.disabled.undefined.git-machete-repository"));
    }
  }

  /**
   * Bear in mind that {@link AnAction#beforeActionPerformedUpdate} is called before each action.
   * (For more details check {@link com.intellij.openapi.actionSystem.ex.ActionUtil} as well).
   * The {@link AnActionEvent} argument passed to before-called {@link AnAction#update} is the same one that is passed here.
   * This gives us certainty that all checks from actions' update implementations will be performed
   * and all data available via data keys in those {@code update} implementations will still be available
   * in {@link #actionPerformed} implementations.
   */
  @Override
  @UIEffect
  public abstract void actionPerformed(AnActionEvent anActionEvent);
}
