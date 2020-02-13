package com.virtuslab.gitmachete.gitmacheteapi;

import com.virtuslab.gitcore.gitcoreapi.GitException;
import com.virtuslab.gitcore.gitcoreapi.IGitCoreLocalBranch;
import java.util.List;
import java.util.Optional;

public interface IGitMacheteBranch {
  IGitCoreLocalBranch getCoreLocalBranch();

  String getName();

  List<IGitMacheteCommit> computeCommits() throws GitException;

  IGitMacheteCommit getPointedCommit() throws GitException;

  List<IGitMacheteBranch> getDownstreamBranches();

  Optional<String> getCustomAnnotation();

  Optional<IGitMacheteBranch> getUpstreamBranch();

  SyncToParentStatus computeSyncToParentStatus() throws GitException;

  SyncToOriginStatus computeSyncToOriginStatus() throws GitException;

  IGitRebaseParameters computeRebaseParameters() throws GitException, GitMacheteException;
}
