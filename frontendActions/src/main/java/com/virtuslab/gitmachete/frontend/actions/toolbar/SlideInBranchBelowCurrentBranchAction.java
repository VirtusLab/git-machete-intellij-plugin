package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;

import com.virtuslab.gitmachete.frontend.actions.base.BaseSlideInBranchBelowAction;

public class SlideInBranchBelowCurrentBranchAction extends BaseSlideInBranchBelowAction {
  @Override
  public Option<String> getNameOfBranchUnderActionWithLogging(AnActionEvent anActionEvent) {
    return getCurrentBranchNameIfManagedWithLoggingOnEmptyRepository(anActionEvent);
  }
}
