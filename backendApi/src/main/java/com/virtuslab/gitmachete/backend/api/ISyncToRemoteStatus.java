package com.virtuslab.gitmachete.backend.api;

public interface ISyncToRemoteStatus {
  enum Relation {
    Untracked, Ahead, Behind, Diverged, InSync
  }

  Relation getRelation();
  String getRemoteName();
}
