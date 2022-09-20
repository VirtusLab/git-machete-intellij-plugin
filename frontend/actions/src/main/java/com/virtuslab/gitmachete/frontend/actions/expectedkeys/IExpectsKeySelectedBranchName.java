package com.virtuslab.gitmachete.frontend.actions.expectedkeys;

import com.intellij.openapi.actionSystem.AnActionEvent;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.actions.base.IWithLogger;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;

public interface IExpectsKeySelectedBranchName extends IWithLogger {
  default @Nullable String getSelectedBranchName(AnActionEvent anActionEvent) {
    val selectedBranchName = anActionEvent.getData(DataKeys.KEY_SELECTED_BRANCH_NAME);
    if (isLoggingAcceptable() && selectedBranchName == null) {
      log().warn("Selected branch is undefined");
    }
    return selectedBranchName;
  }
}
