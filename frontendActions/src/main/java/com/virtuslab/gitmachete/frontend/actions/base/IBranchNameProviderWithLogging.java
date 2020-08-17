package com.virtuslab.gitmachete.frontend.actions.base;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;

public interface IBranchNameProviderWithLogging {
  /**
   * As a rule of thumb, method {@code getNameOfBranchUnderActionWithLogging} should be used only in `actionPerformed` methods
   * and definitely NOT in any `update` or `onUpdate` methods.
   * In `update` and `onUpdate` methods only {@code IBranchNameProviderWithoutLogging#getNameOfBranchUnderActionWithoutLogging}
   * should be used.
   *
   * @param anActionEvent an action event
   * @return Option of branch name under action
   */
  Option<String> getNameOfBranchUnderActionWithLogging(AnActionEvent anActionEvent);
}
