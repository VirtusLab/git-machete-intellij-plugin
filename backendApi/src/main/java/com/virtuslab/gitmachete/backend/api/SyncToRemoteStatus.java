package com.virtuslab.gitmachete.backend.api;

public interface SyncToRemoteStatus {
  enum Status {
    Untracked, Ahead, Behind, Diverged, InSync
  }

  Status getStatus();
  String getRemoteName();
}
