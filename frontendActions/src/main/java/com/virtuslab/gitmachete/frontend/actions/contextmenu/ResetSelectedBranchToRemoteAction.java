package com.virtuslab.gitmachete.frontend.actions.contextmenu;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;

import com.virtuslab.gitmachete.frontend.actions.base.BaseResetBranchToRemoteAction;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedBranchName;

public class ResetSelectedBranchToRemoteAction extends BaseResetBranchToRemoteAction implements IExpectsKeySelectedBranchName {
  @Override
  public Option<String> getNameOfBranchUnderActionWithoutLogging(AnActionEvent anActionEvent) {
    return getSelectedBranchNameWithoutLogging(anActionEvent);
  }

  @Override
  public Option<String> getNameOfBranchUnderActionWithLogging(AnActionEvent anActionEvent) {
    return getSelectedBranchNameWithLogging(anActionEvent);
  }
}
