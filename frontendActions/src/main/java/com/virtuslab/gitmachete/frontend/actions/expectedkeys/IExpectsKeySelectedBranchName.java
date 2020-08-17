package com.virtuslab.gitmachete.frontend.actions.expectedkeys;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;

import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;

/**
 * As a rule of thumb, method {@code getSelectedBranchNameWithLogging} should be used only in `actionPerformed` methods
 * and definitely NOT in any `update` or `onUpdate` methods.
 * In `update` and `onUpdate` methods only {@code getSelectedBranchNameWithoutLogging} should be used.
 */

public interface IExpectsKeySelectedBranchName extends IWithLogger {
  default Option<String> getSelectedBranchNameWithoutLogging(AnActionEvent anActionEvent) {
    return Option.of(anActionEvent.getData(DataKeys.KEY_SELECTED_BRANCH_NAME));
  }

  default Option<String> getSelectedBranchNameWithLogging(AnActionEvent anActionEvent) {
    var selectedBranchName = Option.of(anActionEvent.getData(DataKeys.KEY_SELECTED_BRANCH_NAME));
    if (selectedBranchName.isEmpty()) {
      log().warn("Selected branch is undefined");
    }
    return selectedBranchName;
  }
}
