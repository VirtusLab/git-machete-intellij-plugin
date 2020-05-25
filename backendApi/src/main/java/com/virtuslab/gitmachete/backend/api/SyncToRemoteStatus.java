package com.virtuslab.gitmachete.backend.api;

import lombok.Data;

@Data(staticConstructor = "of")
public class SyncToRemoteStatus {
  public enum Relation {
    Untracked, AheadOfRemote, BehindRemote, DivergedFromAndNewerThanRemote, DivergedFromAndOlderThanRemote, InSyncToRemote
  }

  public static SyncToRemoteStatus untracked() {
    return of(Relation.Untracked, "");
  }

  private final Relation relation;
  private final String remoteName;
}
