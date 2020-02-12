package com.virtuslab.gitmachete.gitmachetejgit;

import com.virtuslab.gitmachete.gitmacheteapi.IGitRebaseParameters;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class GitRebaseParameters implements IGitRebaseParameters {
  private String currentBranchName;
  private String newBaseBranchName;
  private String mergeBaseCommitHash;
  private String forkPointCommitHash;
}
