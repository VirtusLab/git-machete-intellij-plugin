package com.virtuslab.gitmachete.frontend.actions.contextmenu;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;

import com.virtuslab.gitmachete.frontend.actions.common.BaseFastForwardParentToMatchBranchAction;
import com.virtuslab.gitmachete.frontend.actions.common.IExpectsKeySelectedBranchName;

public class FastForwardParentToMatchSelectedBranchAction extends BaseFastForwardParentToMatchBranchAction
    implements
      IExpectsKeySelectedBranchName {
  @Override
  public Option<String> getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
    return getSelectedBranchName(anActionEvent);
  }
}
