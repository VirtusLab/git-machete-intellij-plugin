package com.virtuslab.branchlayout.api;

import io.vavr.collection.List;
import io.vavr.control.Option;

public interface IBranchLayout {
  List<IBranchLayoutEntry> getRootEntries();

  Option<IBranchLayoutEntry> findEntryByName(String branchName);

  default boolean hasEntry(String branchName) {
    return findEntryByName(branchName).isDefined();
  }

  IBranchLayout slideIn(String parentBranchName, IBranchLayoutEntry entryToSlideIn)
      throws EntryDoesNotExistException, EntryIsDescendantOfException;

  IBranchLayout slideOut(String branchName);
}
