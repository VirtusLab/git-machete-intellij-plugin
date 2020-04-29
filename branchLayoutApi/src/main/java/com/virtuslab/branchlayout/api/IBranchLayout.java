package com.virtuslab.branchlayout.api;

import io.vavr.collection.List;
import io.vavr.control.Option;

public interface IBranchLayout {
  List<BaseBranchLayoutEntry> getRootEntries();

  Option<BaseBranchLayoutEntry> findEntryByName(String branchName);

  IBranchLayout slideOut(String branchName) throws BranchLayoutException;
}
