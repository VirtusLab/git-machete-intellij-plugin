package com.virtuslab.gitmachete.gitmachetejgit;

import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMergeParameters;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GitMergeParameters implements IGitMergeParameters {
  private final IGitMacheteBranch currentBranch;
  private final IGitMacheteBranch upstreamBranch;
}
