package com.virtuslab.gitmachete.gitmachetejgit;

import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteCommit;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToParentStatus;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class GitMacheteBranch implements IGitMacheteBranch {
  private String name;
  List<IGitMacheteCommit> commits = new LinkedList<>();
  Optional<IGitMacheteBranch> upstreamBranch;
  Optional<String> customAnnotation;

  @Getter(AccessLevel.NONE)
  List<IGitMacheteBranch> childBranches = new LinkedList<>();

  SyncToParentStatus syncToParentStatus = null;
  SyncToOriginStatus syncToOriginStatus = null;

  public GitMacheteBranch(String name) {
    this.name = name;
  }

  public List<IGitMacheteBranch> getBranches() {
    return childBranches;
  }
}
