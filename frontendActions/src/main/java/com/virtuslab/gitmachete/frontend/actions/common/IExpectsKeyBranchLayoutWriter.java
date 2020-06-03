package com.virtuslab.gitmachete.frontend.actions.common;

import com.intellij.openapi.actionSystem.AnActionEvent;

import com.virtuslab.branchlayout.api.manager.IBranchLayoutWriter;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;

public interface IExpectsKeyBranchLayoutWriter {

  // Could theoretically be static, but this would require all invocations
  // in the implementing classes to be explicitly qualified with interface name:
  //   IExpectsKeyBranchLayoutWriter.getBranchLayoutWriter(e)
  // which would be cumbersome.
  default IBranchLayoutWriter getBranchLayoutWriter(AnActionEvent anActionEvent) {
    return anActionEvent.getData(DataKeys.KEY_BRANCH_LAYOUT_WRITER);
  }
}
