package com.virtuslab.gitcore.impl.jgit;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.eclipse.jgit.lib.ReflogEntry;

import com.virtuslab.gitcore.api.IGitCoreCommitHash;
import com.virtuslab.gitcore.api.IGitCoreReflogEntry;

@EqualsAndHashCode
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class GitCoreReflogEntry implements IGitCoreReflogEntry {
  private final String comment;
  private final Option<IGitCoreCommitHash> oldCommitHash;
  private final IGitCoreCommitHash newCommitHash;

  static GitCoreReflogEntry of(ReflogEntry reflogEntry) {
    return new GitCoreReflogEntry(
        reflogEntry.getComment(),
        GitCoreCommitHash.ofZeroable(reflogEntry.getOldId()),
        GitCoreCommitHash.of(reflogEntry.getNewId()));
  }
}
