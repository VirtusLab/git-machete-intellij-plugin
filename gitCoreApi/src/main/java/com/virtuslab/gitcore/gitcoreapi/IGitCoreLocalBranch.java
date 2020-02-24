package com.virtuslab.gitcore.gitcoreapi;

import java.util.Optional;

public interface IGitCoreLocalBranch extends IGitCoreBranch {
  Optional<IGitCoreBranchTrackingStatus> computeRemoteTrackingStatus() throws GitException;

  Optional<IGitCoreRemoteBranch> getRemoteTrackingBranch() throws GitException;

  Optional<IGitCoreCommit> computeForkPoint() throws GitException;
}
