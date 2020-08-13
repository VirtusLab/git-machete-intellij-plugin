package com.virtuslab.gitmachete.backend.api;

import io.vavr.collection.List;

public interface IForkPointCommitOfManagedBranch extends ICommitOfManagedBranch {
  List<String> getBranchesContainingInReflog();

  boolean isOverridden();
}
