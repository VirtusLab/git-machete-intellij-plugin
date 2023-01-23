package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BranchLayoutEntry;
import com.virtuslab.gitmachete.frontend.actions.traverse.BaseTraverseAction;

@CustomLog
public class TraverseFromFirstAction extends BaseTraverseAction {
  @Override
  public @Nullable String getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
    val branchLayout = getBranchLayout(anActionEvent);

    if (branchLayout == null) {
      return null;
    }

    return branchLayout.getRootEntries().headOption().map(BranchLayoutEntry::getName).getOrNull();
  }

  @Override
  public LambdaLogger log() {
    return LOG;
  }
}
