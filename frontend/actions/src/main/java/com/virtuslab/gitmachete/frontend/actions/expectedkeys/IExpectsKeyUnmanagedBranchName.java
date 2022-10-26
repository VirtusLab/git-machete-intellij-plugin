package com.virtuslab.gitmachete.frontend.actions.expectedkeys;

import com.intellij.openapi.actionSystem.AnActionEvent;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.actions.base.IWithLogger;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;

public interface IExpectsKeyUnmanagedBranchName extends IWithLogger {
  default @Nullable String getNameOfUnmanagedBranch(AnActionEvent anActionEvent) {
    val unmanagedBranchName = anActionEvent.getData(DataKeys.UNMANAGED_BRANCH_NAME);
    if (isLoggingAcceptable() && unmanagedBranchName == null) {
      log().warn("Unmanaged branch is undefined");
    }
    return unmanagedBranchName;
  }
}
