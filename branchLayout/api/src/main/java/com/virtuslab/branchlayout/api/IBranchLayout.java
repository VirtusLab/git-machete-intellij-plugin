package com.virtuslab.branchlayout.api;

import io.vavr.collection.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface IBranchLayout {
  List<IBranchLayoutEntry> getRootEntries();

  @Nullable
  IBranchLayoutEntry findEntryByName(String branchName);

  default boolean hasEntry(String branchName) {
    return findEntryByName(branchName) != null;
  }

  IBranchLayout slideIn(String parentBranchName, IBranchLayoutEntry entryToSlideIn)
      throws EntryDoesNotExistException, EntryIsDescendantOfException;

  IBranchLayout slideOut(String branchName);
}
