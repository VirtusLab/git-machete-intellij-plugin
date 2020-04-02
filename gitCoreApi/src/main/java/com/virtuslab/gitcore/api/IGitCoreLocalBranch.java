package com.virtuslab.gitcore.api;

import java.util.Optional;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface IGitCoreLocalBranch extends IGitCoreBranch {
  Optional<IGitCoreBranchTrackingStatus> deriveRemoteTrackingStatus() throws GitCoreException;

  Optional<IGitCoreRemoteBranch> getRemoteTrackingBranch();

  Optional<BaseGitCoreCommit> deriveForkPoint(@Nullable BaseGitCoreCommit upstreamBranch) throws GitCoreException;
}
