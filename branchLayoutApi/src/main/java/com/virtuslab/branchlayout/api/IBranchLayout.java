package com.virtuslab.branchlayout.api;

import java.util.Optional;

import io.vavr.collection.List;

public interface IBranchLayout {
  List<IBranchLayoutEntry> getRootBranches();

  Optional<IBranchLayoutEntry> findEntryByName(String branchName);

  IBranchLayout slideOut(String branchName) throws BranchLayoutException;
}
