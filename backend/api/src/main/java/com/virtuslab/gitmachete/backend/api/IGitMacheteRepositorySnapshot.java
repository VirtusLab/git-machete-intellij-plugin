package com.virtuslab.gitmachete.backend.api;

import java.nio.file.Path;

import io.vavr.collection.List;
import io.vavr.collection.Set;
import lombok.Data;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BranchLayout;

/**
 * An immutable snapshot of an {@link IGitMacheteRepository} for some specific moment in time.
 * Each {@code get...} method is guaranteed to return the same value each time it's called on a given object.
 */
public interface IGitMacheteRepositorySnapshot {
  /**
   * @return the root directory of the worktree this snapshot was built against
   *         (the directory containing the per-worktree gitlink {@code .git}).
   *         Canonicalized at snapshot creation time, so a plain {@link Path#equals} against another canonical path
   *         is enough to ask "is this snapshot scoped to that worktree?".
   */
  Path getRootDirectoryPath();

  BranchLayout getBranchLayout();

  List<IRootManagedBranchSnapshot> getRootBranches();

  @Nullable
  IManagedBranchSnapshot getCurrentBranchIfManaged();

  /** Branches are ordered as they occur in the machete file */
  List<IManagedBranchSnapshot> getManagedBranches();

  @Nullable
  IManagedBranchSnapshot getManagedBranchByName(String branchName);

  Set<String> getDuplicatedBranchNames();

  Set<String> getSkippedBranchNames();

  @Data
  // So that Interning Checker doesn't complain about enum comparison (by `equals` and not by `==`) in Lombok-generated `equals`
  @SuppressWarnings("interning:unnecessary.equals")
  class OngoingRepositoryOperation {
    private final OngoingRepositoryOperationType operationType;

    private final @Nullable String baseBranchName;
  }

  OngoingRepositoryOperation getOngoingRepositoryOperation();
}
