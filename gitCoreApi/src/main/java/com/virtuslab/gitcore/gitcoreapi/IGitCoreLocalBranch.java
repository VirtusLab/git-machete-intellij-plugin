package com.virtuslab.gitcore.gitcoreapi;

import java.util.Optional;

public interface IGitCoreLocalBranch extends IGitCoreBranch {
  Optional<IGitCoreBranchTrackingStatus> getTrackingStatus() throws GitException;

  Optional<IGitCoreRemoteBranch> getTrackingBranch() throws GitException;
}
