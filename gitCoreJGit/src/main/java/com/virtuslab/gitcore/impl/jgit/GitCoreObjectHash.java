package com.virtuslab.gitcore.impl.jgit;

import com.virtuslab.gitcore.api.IGitCoreCommitHash;
import lombok.Data;

@Data
public class GitCoreObjectHash implements IGitCoreCommitHash {
  private final String hashString;
}
