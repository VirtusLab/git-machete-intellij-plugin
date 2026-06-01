package com.virtuslab.gitmachete.backend.api;

import java.nio.file.Path;

import io.vavr.collection.List;
import io.vavr.collection.Map;
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

  /**
   * Per-branch label naming the worktree currently holding that branch checked out, suitable for
   * rendering next to the branch name in status. The result is empty unless the labeling feature
   * fires, which requires at least one <i>linked</i> worktree to exist; in a plain single-worktree
   * repo every branch is unambiguously in (or absent from) the only worktree, so a label would be
   * pure clutter.
   *
   * <p>When the feature does fire, the value for a labeled branch is one of:
   * <ul>
   *   <li>{@code "<this worktree>"} - the branch lives in the worktree this snapshot was built
   *       against, regardless of whether that's the main or a linked one;</li>
   *   <li>{@code "<main worktree>"} - the branch lives in the main worktree and the snapshot is
   *       taken from a linked one;</li>
   *   <li>otherwise - the holding linked worktree's path with the longest common path prefix of
   *       <em>all linked-worktree paths</em> stripped (typical layouts like {@code ~/wts/foo} and
   *       {@code ~/wts/bar} collapse to {@code foo} and {@code bar}). The main worktree is
   *       deliberately excluded from the prefix computation so that it can't artificially lengthen
   *       every linked label - e.g. when main lives under {@code ~/projects} but linked worktrees
   *       sit under {@code /tmp}, the only shared component would be {@code /}.</li>
   * </ul>
   *
   * <p>Branches not currently checked out in any worktree are simply absent from the map.
   */
  Map<String, String> getWorktreeLabelByLocalBranchName();

  @Data
  // So that Interning Checker doesn't complain about enum comparison (by `equals` and not by `==`) in Lombok-generated `equals`
  @SuppressWarnings("interning:unnecessary.equals")
  class OngoingRepositoryOperation {
    private final OngoingRepositoryOperationType operationType;

    private final @Nullable String baseBranchName;
  }

  OngoingRepositoryOperation getOngoingRepositoryOperation();
}
