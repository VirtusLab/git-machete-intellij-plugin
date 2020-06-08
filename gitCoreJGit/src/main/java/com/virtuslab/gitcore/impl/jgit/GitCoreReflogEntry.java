package com.virtuslab.gitcore.impl.jgit;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.lib.ReflogEntry;

import com.virtuslab.gitcore.api.IGitCoreCommitHash;
import com.virtuslab.gitcore.api.IGitCoreReflogEntry;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
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
