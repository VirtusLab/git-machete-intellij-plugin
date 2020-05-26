package com.virtuslab.gitcore.impl.jgit;

import io.vavr.Lazy;
import io.vavr.collection.List;
import io.vavr.control.Either;
import lombok.CustomLog;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreBranch;
import com.virtuslab.gitcore.api.IGitCoreReflogEntry;

@CustomLog
@RequiredArgsConstructor
public abstract class BaseGitCoreBranch implements IGitCoreBranch {

  @Getter
  protected final String shortName;

  private final Lazy<Either<GitCoreException, GitCoreCommit>> pointedCommit;

  private final Lazy<Either<GitCoreException, List<IGitCoreReflogEntry>>> reflog;

  public abstract String getBranchTypeString(boolean capitalized);

  @Override
  public GitCoreCommit derivePointedCommit() throws GitCoreException {
    return pointedCommit.get().getOrElseThrow(e -> e);
  }

  @Override
  public List<IGitCoreReflogEntry> deriveReflog() throws GitCoreException {
    return reflog.get().getOrElseThrow(e -> e);
  }

  @Override
  public final boolean equals(@Nullable Object other) {
    return IGitCoreBranch.defaultEquals(this, other);
  }

  @Override
  public final int hashCode() {
    return IGitCoreBranch.defaultHashCode(this);
  }
}
