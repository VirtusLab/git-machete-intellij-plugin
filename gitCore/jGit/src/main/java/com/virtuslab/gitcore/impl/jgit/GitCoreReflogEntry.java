package com.virtuslab.gitcore.impl.jgit;

import java.time.Instant;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.ExtensionMethod;
import org.checkerframework.checker.interning.qual.UsesObjectEquals;
import org.eclipse.jgit.lib.ReflogEntry;

import com.virtuslab.gitcore.api.IGitCoreCheckoutEntry;
import com.virtuslab.gitcore.api.IGitCoreCommitHash;
import com.virtuslab.gitcore.api.IGitCoreReflogEntry;

@RequiredArgsConstructor(access = AccessLevel.MODULE)
@ExtensionMethod(GitCoreCommitHash.class)
@ToString(onlyExplicitlyIncluded = true)
@UsesObjectEquals
public class GitCoreReflogEntry implements IGitCoreReflogEntry {

  private final ReflogEntry reflogEntry;

  @Override
  @ToString.Include(name = "comment")
  public String getComment() {
    return reflogEntry.getComment();
  }

  @Override
  @ToString.Include(name = "timestamp")
  public Instant getTimestamp() {
    return reflogEntry.getWho().getWhen().toInstant();
  }

  @Override
  @ToString.Include(name = "oldCommitHash")
  public Option<IGitCoreCommitHash> getOldCommitHash() {
    return reflogEntry.getOldId().toGitCoreCommitHashOption();
  }

  @Override
  @ToString.Include(name = "newCommitHash")
  public IGitCoreCommitHash getNewCommitHash() {
    return reflogEntry.getNewId().toGitCoreCommitHash();
  }

  @Override
  public Option<IGitCoreCheckoutEntry> parseCheckout() {
    return Option.of(reflogEntry.parseCheckout()).map(GitCoreCheckoutEntry::of);
  }
}
