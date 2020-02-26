package com.virtuslab.gitmachete.gitmacheteapi;

import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import java.util.List;
import java.util.Optional;

public interface IGitMacheteBranch {
  IGitCoreLocalBranch getCoreLocalBranch();

  String getName();

  List<IGitMacheteCommit> computeCommits() throws GitCoreException;

  IGitMacheteCommit getPointedCommit() throws GitCoreException;

  List<IGitMacheteBranch> getDownstreamBranches();

  Optional<String> getCustomAnnotation();

  Optional<IGitMacheteBranch> getUpstreamBranch();

  SyncToParentStatus computeSyncToParentStatus() throws GitCoreException;

  SyncToOriginStatus computeSyncToOriginStatus() throws GitCoreException;

  IGitRebaseParameters computeRebaseParameters() throws GitCoreException, GitMacheteException;

  IGitMergeParameters getMergeParameters() throws GitMacheteException;
}
