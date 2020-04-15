package com.virtuslab.gitcore.impl.jgit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.eclipse.jgit.revwalk.RevCommit;

import com.virtuslab.gitcore.api.BaseGitCoreCommitHash;

@Getter
@RequiredArgsConstructor
@ToString
public class GitCoreCommitHash extends BaseGitCoreCommitHash {
  private final String hashString;

  public static GitCoreCommitHash of(RevCommit jgitCommit) {
    return new GitCoreCommitHash(jgitCommit.getId().getName());
  }
}
