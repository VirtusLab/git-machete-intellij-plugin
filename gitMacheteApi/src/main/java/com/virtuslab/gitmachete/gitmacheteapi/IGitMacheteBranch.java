package com.virtuslab.gitmachete.gitmacheteapi;

import java.util.List;
import java.util.Optional;

public interface IGitMacheteBranch {
  String getName();

  List<IGitMacheteCommit> getCommits();

  List<IGitMacheteBranch> getBranches();

  Optional<String> getCustomAnnotation();

  Optional<IGitMacheteBranch> getUpstreamBranch();

  SyncToParentStatus getSyncToParentStatus();

  SyncToOriginStatus getSyncToOriginStatus();
}
