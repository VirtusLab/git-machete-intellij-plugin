package com.virtuslab.gitmachete.backend.api;

import io.vavr.collection.List;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.IBranchLayoutEntry;

public final class NullBranchLayout implements IBranchLayout {
  private static final IBranchLayout instance = new NullBranchLayout();

  public static IBranchLayout getInstance() {
    return instance;
  }

  private NullBranchLayout() {}

  @Override
  public List<IBranchLayoutEntry> getRootEntries() {
    return List.empty();
  }

  @Override
  public @Nullable IBranchLayoutEntry findEntryByName(String branchName) {
    return null;
  }

  @Override
  public IBranchLayout slideIn(String parentBranchName, IBranchLayoutEntry entryToSlideIn) {
    return this;
  }

  @Override
  public IBranchLayout slideOut(String branchName) {
    return this;
  }
}
