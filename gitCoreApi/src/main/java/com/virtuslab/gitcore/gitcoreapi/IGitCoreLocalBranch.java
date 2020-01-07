package com.virtuslab.gitcore.gitcoreapi;

import java.util.Optional;

public interface IGitCoreLocalBranch extends IGitCoreBranch {
  Optional<IGitCoreBranchTrackingStatus> getRemoteTrackingStatus() throws GitException;

  Optional<IGitCoreRemoteBranch> getRemoteTrackingBranch() throws GitException;

  Optional<IGitCoreCommit> getForkPoint() throws GitException;
}
