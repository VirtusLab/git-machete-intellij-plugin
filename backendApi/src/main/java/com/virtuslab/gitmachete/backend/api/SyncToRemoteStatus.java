package com.virtuslab.gitmachete.backend.api;

import lombok.Data;

@Data(staticConstructor = "of")
public class SyncToRemoteStatus {
  public enum Relation {
    Untracked, AheadOfRemote, BehindRemote, DivergedFromAndNewerThanRemote, DivergedFromAndOlderThanRemote, InSyncToRemote
  }

  private final Relation relation;
  private final String remoteName;
}
