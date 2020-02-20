package com.virtuslab.gitmachete.gitmachetejgit;

import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteCommit;
import com.virtuslab.gitmachete.gitmacheteapi.IGitRebaseParameters;
import lombok.Data;

@Data
public class GitRebaseParameters implements IGitRebaseParameters {
  private final IGitMacheteBranch currentBranch;
  private final IGitMacheteCommit newBaseCommit;
  private final IGitMacheteCommit forkPointCommit;
}
