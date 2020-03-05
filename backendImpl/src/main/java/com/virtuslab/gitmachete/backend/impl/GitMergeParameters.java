package com.virtuslab.gitmachete.backend.impl;

import lombok.Data;

import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMergeParameters;

@Data
public class GitMergeParameters implements IGitMergeParameters {
  private final IGitMacheteBranch currentBranch;
  private final IGitMacheteBranch upstreamBranch;
}
