package com.virtuslab.gitmachete.frontend.actions.base;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;

public interface IBranchNameProviderWithoutLogging {
  Option<String> getNameOfBranchUnderActionWithoutLogging(AnActionEvent anActionEvent);
}
