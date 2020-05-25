package com.virtuslab.gitcore.impl.jgit;

import java.util.function.Function;

import io.vavr.Lazy;
import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vavr.control.Try;
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
    return pointedCommit.get().getOrElseThrow(Function.identity());
  }

  @Override
  public List<IGitCoreReflogEntry> deriveReflog() throws GitCoreException {
    return reflog.get().getOrElseThrow(Function.identity());
  }

  @Override
  public final boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    } else if (!(other instanceof BaseGitCoreBranch)) {
      return false;
    } else {
      var o = (BaseGitCoreBranch) other;
      return getFullName().equals(o.getFullName())
          && Try.of(() -> derivePointedCommit().equals(o.derivePointedCommit())).getOrElse(false);
    }
  }

  @Override
  public final int hashCode() {
    return getFullName().hashCode() * 37 + Try.of(() -> derivePointedCommit().hashCode()).getOrElse(0);
  }
}
