package com.virtuslab.gitcore.impl.jgit;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.jgit.lib.ObjectId;

import com.virtuslab.gitcore.api.IGitCoreCommitHash;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public final class GitCoreCommitHash implements IGitCoreCommitHash {
  private final String hashString;

  static GitCoreCommitHash of(ObjectId objectId) {
    return new GitCoreCommitHash(objectId.getName());
  }

  static Option<IGitCoreCommitHash> ofZeroable(ObjectId objectId) {
    return objectId.equals(ObjectId.zeroId()) ? Option.none() : Option.some(of(objectId));
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    } else if (!(other instanceof IGitCoreCommitHash)) {
      return false;
    } else {
      return getHashString().equals(((IGitCoreCommitHash) other).getHashString());
    }
  }

  @Override
  public int hashCode() {
    return getHashString().hashCode();
  }
}
