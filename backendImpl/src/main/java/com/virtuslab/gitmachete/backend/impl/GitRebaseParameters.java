package com.virtuslab.gitmachete.backend.impl;

import lombok.Data;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;

@Data
public class GitRebaseParameters implements IGitRebaseParameters {
  private final BaseGitMacheteBranch currentBranch;
  private final IGitMacheteCommit newBaseCommit;
  private final IGitMacheteCommit forkPointCommit;
}
