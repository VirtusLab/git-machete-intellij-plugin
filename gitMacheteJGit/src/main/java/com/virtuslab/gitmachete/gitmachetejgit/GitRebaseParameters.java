package com.virtuslab.gitmachete.gitmachetejgit;

import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteCommit;
import com.virtuslab.gitmachete.gitmacheteapi.IGitRebaseParameters;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GitRebaseParameters implements IGitRebaseParameters {
  private IGitMacheteBranch currentBranch;
  private IGitMacheteBranch newBaseBranch;
  private IGitMacheteCommit forkPointCommit;
}
