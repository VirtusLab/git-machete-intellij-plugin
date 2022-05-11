package com.virtuslab.gitcore.impl.jgit;

import io.vavr.control.Option;
import org.eclipse.jgit.lib.ObjectId;

import com.virtuslab.gitcore.api.IGitCoreCommitHash;

public final class GitCoreCommitHash extends GitCoreObjectHash implements IGitCoreCommitHash {

  private GitCoreCommitHash(ObjectId objectId) {
    super(objectId);
  }

  static GitCoreCommitHash of(ObjectId objectId) {
    return new GitCoreCommitHash(objectId);
  }

  static Option<IGitCoreCommitHash> ofZeroable(ObjectId objectId) {
    return objectId.equals(ObjectId.zeroId()) ? Option.none() : Option.some(of(objectId));
  }

  @Override
  public String toString() {
    return "<commit " + getHashString() + ">";
  }
}
