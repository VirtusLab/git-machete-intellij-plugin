package com.virtuslab.gitmachete.frontend.actions.contextmenu;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;

import com.virtuslab.gitmachete.frontend.actions.common.BaseRebaseBranchOntoParentAction;
import com.virtuslab.gitmachete.frontend.actions.common.IExpectsKeySelectedBranchName;

public class RebaseSelectedBranchOntoParentAction extends BaseRebaseBranchOntoParentAction
    implements
      IExpectsKeySelectedBranchName {
  @Override
  public Option<String> getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
    return getSelectedBranchName(anActionEvent);
  }
}
