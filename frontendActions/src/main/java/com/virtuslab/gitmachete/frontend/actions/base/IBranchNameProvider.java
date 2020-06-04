package com.virtuslab.gitmachete.frontend.actions.base;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;

public interface IBranchNameProvider {
  Option<String> getNameOfBranchUnderAction(AnActionEvent anActionEvent);
}
