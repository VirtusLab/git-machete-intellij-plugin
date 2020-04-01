package com.virtuslab.gitmachete.backend.impl;

import lombok.Data;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMergeParameters;

@Data
public class GitMergeParameters implements IGitMergeParameters {
  private final BaseGitMacheteBranch currentBranch;
  private final BaseGitMacheteBranch branchToMergeInto;
}
