package com.virtuslab.gitmachete.gitmacheteapi;

import com.virtuslab.gitcore.gitcoreapi.GitException;
import com.virtuslab.gitcore.gitcoreapi.IGitCoreLocalBranch;
import java.util.List;
import java.util.Optional;

public interface IGitMacheteBranch {
  IGitCoreLocalBranch getCoreLocalBranch();

  String getName();

  List<IGitMacheteCommit> getCommits() throws GitException;

  List<IGitMacheteBranch> getBranches();

  Optional<String> getCustomAnnotation();

  Optional<IGitMacheteBranch> getUpstreamBranch();

  SyncToParentStatus getSyncToParentStatus() throws GitException;

  SyncToOriginStatus getSyncToOriginStatus() throws GitException;

  void slideOut() throws GitMacheteException;
}
