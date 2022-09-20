package com.virtuslab.gitmachete.frontend.actions.contextmenu;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.actions.base.BaseFastForwardMergeToParentAction;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedBranchName;

public class FastForwardMergeSelectedToParentAction extends BaseFastForwardMergeToParentAction
    implements
      IExpectsKeySelectedBranchName {
  @Override
  public @Nullable String getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
    return getSelectedBranchName(anActionEvent);
  }
}
