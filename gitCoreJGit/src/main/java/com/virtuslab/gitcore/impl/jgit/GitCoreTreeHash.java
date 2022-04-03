package com.virtuslab.gitcore.impl.jgit;

import org.eclipse.jgit.lib.ObjectId;

import com.virtuslab.gitcore.api.IGitCoreTreeHash;

public final class GitCoreTreeHash extends GitCoreObjectHash implements IGitCoreTreeHash {

  private GitCoreTreeHash(ObjectId objectId) {
    super(objectId);
  }

  static IGitCoreTreeHash of(ObjectId objectId) {
    return new GitCoreTreeHash(objectId);
  }

  @Override
  public String toString() {
    return "<tree " + getHashString() + ">";
  }
}
