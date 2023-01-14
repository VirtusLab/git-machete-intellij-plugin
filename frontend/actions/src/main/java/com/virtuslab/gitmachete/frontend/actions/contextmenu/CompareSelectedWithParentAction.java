package com.virtuslab.gitmachete.frontend.actions.contextmenu;

import com.intellij.openapi.actionSystem.AnActionEvent;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.actions.base.BaseCompareWithParentAction;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedBranchName;

@CustomLog
public class CompareSelectedWithParentAction extends BaseCompareWithParentAction implements IExpectsKeySelectedBranchName {
  @Override
  protected boolean isSideEffecting() {
    return false;
  }

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  @Override
  public @Nullable String getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
    return getSelectedBranchName(anActionEvent);
  }
}
