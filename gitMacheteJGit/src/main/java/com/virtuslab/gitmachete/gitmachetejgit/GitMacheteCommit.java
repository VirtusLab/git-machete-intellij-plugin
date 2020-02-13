package com.virtuslab.gitmachete.gitmachetejgit;

import com.virtuslab.gitcore.gitcoreapi.GitException;
import com.virtuslab.gitcore.gitcoreapi.IGitCoreCommit;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteCommit;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GitMacheteCommit implements IGitMacheteCommit {
  private String message;
  private String hash;

  public GitMacheteCommit(IGitCoreCommit coreCommit) throws GitException {
    message = coreCommit.getMessage();
    hash = coreCommit.getHash().getHashString();
  }
}
