package com.virtuslab.gitmachete.backend.api;

import lombok.Data;

@Data(staticConstructor = "of")
public class SyncToRemoteStatus {
  public enum Relation {
    Untracked, Ahead, Behind, Diverged, InSync
  }

  private final Relation relation;
  private final String remoteName;
}
