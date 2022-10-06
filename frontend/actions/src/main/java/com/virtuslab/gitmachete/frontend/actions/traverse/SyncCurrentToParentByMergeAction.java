package com.virtuslab.gitmachete.frontend.actions.traverse;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.actions.base.BaseSyncToParentByMergeAction;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedBranchName;

public class SyncCurrentToParentByMergeAction extends BaseSyncToParentByMergeAction
    implements
      IExpectsKeySelectedBranchName {

  @Override
  public @Nullable String getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
    return getCurrentBranchNameIfManaged(anActionEvent);
  }
}
