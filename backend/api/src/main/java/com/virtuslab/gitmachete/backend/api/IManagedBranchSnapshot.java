package com.virtuslab.gitmachete.backend.api;

import io.vavr.collection.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.EnsuresQualifierIf;
import org.checkerframework.framework.qual.RequiresQualifier;

import com.virtuslab.qual.gitmachete.backend.api.ConfirmedNonRoot;
import com.virtuslab.qual.gitmachete.backend.api.ConfirmedNonTracked;
import com.virtuslab.qual.gitmachete.backend.api.ConfirmedRoot;
import com.virtuslab.qual.gitmachete.backend.api.ConfirmedTracked;

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

  @EnsuresQualifierIf(expression = "this", result = true, qualifier = ConfirmedTracked.class)
  @EnsuresQualifierIf(expression = "this", result = false, qualifier = ConfirmedNonTracked.class)
  default boolean isTracked() {
    return getRemoteTrackingBranch() != null;
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

  IGitRebaseParameters getParametersForRebaseOntoRemote() throws GitMacheteMissingForkPointException;

}
