package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.base.BaseRebaseBranchOntoParentAction;

public class RebaseCurrentBranchOntoParentAction extends BaseRebaseBranchOntoParentAction {
  @Override
  public Option<String> getNameOfBranchUnderActionWithoutLogging(AnActionEvent anActionEvent) {
    return getCurrentBranchNameIfManagedWithoutLogging(anActionEvent);
  }

  @Override
  public Option<String> getNameOfBranchUnderActionWithLogging(AnActionEvent anActionEvent) {
    return getCurrentBranchNameIfManagedWithLogging(anActionEvent);
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);
    var presentation = anActionEvent.getPresentation();
    if (!presentation.isVisible()) {
      return;
    }

    var isNonRootBranch = getCurrentBranchNameIfManagedWithoutLogging(anActionEvent)
        .flatMap(bn -> getGitMacheteBranchByNameWithLogging(anActionEvent, bn))
        .map(b -> b.isNonRoot())
        .getOrElse(false);

    presentation.setVisible(isNonRootBranch);
  }
}
