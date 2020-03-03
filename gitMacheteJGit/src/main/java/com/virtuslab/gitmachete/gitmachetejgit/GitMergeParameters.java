package com.virtuslab.gitmachete.gitmachetejgit;

import lombok.Data;

import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMergeParameters;

@Data
public class GitMergeParameters implements IGitMergeParameters {
  private final IGitMacheteBranch currentBranch;
  private final IGitMacheteBranch upstreamBranch;
}
