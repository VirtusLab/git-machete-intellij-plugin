package com.virtuslab.gitmachete.frontend.actions.expectedkeys;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;

import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;

public interface IExpectsKeySelectedBranchName extends IExpectsKeyLogger {
  default Option<String> getSelectedBranchName(AnActionEvent anActionEvent) {
    var selectedBranchName = Option.of(anActionEvent.getData(DataKeys.KEY_SELECTED_BRANCH_NAME));
    if (selectedBranchName.isEmpty()) {
      log().warn("Selected branch is undefined");
    }
    return selectedBranchName;
  }
}
