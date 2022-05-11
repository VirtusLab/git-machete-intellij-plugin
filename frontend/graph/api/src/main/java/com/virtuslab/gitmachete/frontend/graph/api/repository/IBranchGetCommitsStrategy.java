package com.virtuslab.gitmachete.frontend.graph.api.repository;

import io.vavr.collection.List;

import com.virtuslab.gitmachete.backend.api.ICommitOfManagedBranch;
import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot;

public interface IBranchGetCommitsStrategy {
  List<ICommitOfManagedBranch> getCommitsOf(INonRootManagedBranchSnapshot branch);
}
