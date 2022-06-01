package com.virtuslab.gitcore.impl.jgit;

import io.vavr.control.Option;
import org.eclipse.jgit.lib.ObjectId;

import com.virtuslab.gitcore.api.IGitCoreCommitHash;

public final class GitCoreCommitHash extends GitCoreObjectHash implements IGitCoreCommitHash {

  private GitCoreCommitHash(ObjectId objectId) {
    super(objectId);
  }

  public static GitCoreCommitHash toGitCoreCommitHash(ObjectId objectId) {
    return new GitCoreCommitHash(objectId);
  }

  public static Option<IGitCoreCommitHash> toGitCoreCommitHashOption(ObjectId objectId) {
    return objectId.equals(ObjectId.zeroId()) ? Option.none() : Option.some(toGitCoreCommitHash(objectId));
  }

  @Override
  public String toString() {
    return "<commit " + getHashString() + ">";
  }
}
