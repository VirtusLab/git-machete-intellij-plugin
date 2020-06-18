package com.virtuslab.gitmachete.frontend.actions.contextmenu;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;

import com.virtuslab.gitmachete.frontend.actions.base.BaseForcePushBranchAction;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedBranchName;

public class ForcePushSelectedBranchAction extends BaseForcePushBranchAction implements IExpectsKeySelectedBranchName {
  @Override
  public Option<String> getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
    return getSelectedBranchName(anActionEvent);
  }
}
