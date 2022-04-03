package com.virtuslab.gitcore.impl.jgit;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.ArrayLen;
import org.eclipse.jgit.lib.ObjectId;

import com.virtuslab.gitcore.api.IGitCoreObjectHash;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class GitCoreObjectHash implements IGitCoreObjectHash {

  @Getter(AccessLevel.PACKAGE)
  private final ObjectId objectId;

  @Override
  public final @ArrayLen(40) String getHashString() {
    return objectId.getName();
  }

  @Override
  public abstract String toString();

  @Override
  public final boolean equals(@Nullable Object other) {
    return IGitCoreObjectHash.defaultEquals(this, other);
  }

  @Override
  public final int hashCode() {
    return IGitCoreObjectHash.defaultHashCode(this);
  }
}
