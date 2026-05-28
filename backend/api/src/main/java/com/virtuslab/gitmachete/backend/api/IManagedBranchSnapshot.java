package com.virtuslab.gitmachete.backend.api;

import java.nio.file.Path;

import io.vavr.collection.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.EnsuresQualifierIf;
import org.checkerframework.framework.qual.RequiresQualifier;

import com.virtuslab.qual.subtyping.gitmachete.backend.api.ConfirmedNonRoot;
import com.virtuslab.qual.subtyping.gitmachete.backend.api.ConfirmedRoot;

/**
 * The only criterion for equality of any instances of any class implementing this interface is reference equality
 */
public interface IManagedBranchSnapshot extends ILocalBranchReference {
  @EnsuresQualifierIf(expression = "this", result = true, qualifier = ConfirmedRoot.class)
  @EnsuresQualifierIf(expression = "this", result = false, qualifier = ConfirmedNonRoot.class)
  boolean isRoot();

  @EnsuresQualifierIf(expression = "this", result = true, qualifier = ConfirmedNonRoot.class)
  @EnsuresQualifierIf(expression = "this", result = false, qualifier = ConfirmedRoot.class)
  default boolean isNonRoot() {
    return !isRoot();
  }

  @RequiresQualifier(expression = "this", qualifier = ConfirmedRoot.class)
  IRootManagedBranchSnapshot asRoot();

  @RequiresQualifier(expression = "this", qualifier = ConfirmedNonRoot.class)
  INonRootManagedBranchSnapshot asNonRoot();

  String getName();

  String getFullName();

  ICommitOfManagedBranch getPointedCommit();

  List<? extends INonRootManagedBranchSnapshot> getChildren();

  RelationToRemote getRelationToRemote();

  @Nullable
  IRemoteTrackingBranchReference getRemoteTrackingBranch();

  @Nullable
  String getCustomAnnotation();

  @Nullable
  String getStatusHookOutput();

  /**
   * @return the root directory of the worktree currently holding this branch checked out, or {@code null}
   *         if no worktree holds it. This may be the worktree against which the enclosing
   *         {@link IGitMacheteRepositorySnapshot} was built (in which case the branch is the snapshot's
   *         current branch); use {@link IGitMacheteRepositorySnapshot#getRootDirectoryPath()} to tell
   *         "our" worktree apart from "another" one.
   */
  @Nullable
  Path getWorktreeRootHoldingBranch();
}
