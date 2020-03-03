package com.virtuslab.gitmachete.gitmachetejgit;

import lombok.Data;

import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteCommit;
import com.virtuslab.gitmachete.gitmacheteapi.IGitRebaseParameters;

@Data
public class GitRebaseParameters implements IGitRebaseParameters {
  private final IGitMacheteBranch currentBranch;
  private final IGitMacheteCommit newBaseCommit;
  private final IGitMacheteCommit forkPointCommit;
}
