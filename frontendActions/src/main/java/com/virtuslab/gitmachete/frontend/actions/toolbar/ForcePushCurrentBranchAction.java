package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;

import com.virtuslab.gitmachete.frontend.actions.base.BaseForcePushBranchAction;

public class ForcePushCurrentBranchAction extends BaseForcePushBranchAction {
  @Override
  public Option<String> getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
    return getCurrentBranchNameIfManaged(anActionEvent);
  }
}
