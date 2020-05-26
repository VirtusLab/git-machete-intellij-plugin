package com.virtuslab.gitcore.impl.jgit;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.ArrayLen;
import org.eclipse.jgit.lib.ObjectId;

import com.virtuslab.gitcore.api.IGitCoreCommitHash;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class GitCoreCommitHash implements IGitCoreCommitHash {
  private final @ArrayLen(40) String hashString;

  static GitCoreCommitHash of(ObjectId objectId) {
    return new GitCoreCommitHash(objectId.getName());
  }

  static Option<IGitCoreCommitHash> ofZeroable(ObjectId objectId) {
    return objectId.equals(ObjectId.zeroId()) ? Option.none() : Option.some(of(objectId));
  }

  @Override
  public @ArrayLen(40) String getHashString() {
    return hashString;
  }

  @Override
  public String toString() {
    return "<" + hashString + ">";
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return IGitCoreCommitHash.defaultEquals(this, other);
  }

  @Override
  public int hashCode() {
    return IGitCoreCommitHash.defaultHashCode(this);
  }
}
