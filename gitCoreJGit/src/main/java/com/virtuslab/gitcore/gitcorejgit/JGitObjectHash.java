package com.virtuslab.gitcore.gitcorejgit;

import com.virtuslab.gitcore.gitcoreapi.IGitCoreCommitHash;
import lombok.Data;

@Data
public class JGitObjectHash implements IGitCoreCommitHash {
  private final String hashString;
}
