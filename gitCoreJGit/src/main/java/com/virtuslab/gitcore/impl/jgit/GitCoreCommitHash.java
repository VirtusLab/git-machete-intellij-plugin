package com.virtuslab.gitcore.impl.jgit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitcore.api.IGitCoreCommitHash;

@Getter
@RequiredArgsConstructor
@ToString
public class GitCoreCommitHash implements IGitCoreCommitHash {
  private final String hashString;

  @Override
  public final boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    } else if (!(other instanceof IGitCoreCommitHash)) {
      return false;
    } else {
      return getHashString().equals(((IGitCoreCommitHash) other).getHashString());
    }
  }

  @Override
  public final int hashCode() {
    return getHashString().hashCode();
  }
}
