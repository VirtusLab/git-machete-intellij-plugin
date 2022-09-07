package com.virtuslab.gitmachete.frontend.actions.base;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface IBranchNameProvider {
  @Nullable
  String getNameOfBranchUnderAction(AnActionEvent anActionEvent);
}
