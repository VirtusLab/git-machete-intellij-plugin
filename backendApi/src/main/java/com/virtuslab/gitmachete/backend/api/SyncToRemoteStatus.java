package com.virtuslab.gitmachete.backend.api;

import lombok.Data;

@Data(staticConstructor = "of")
public class SyncToRemoteStatus {
  public enum Relation {
    NoRemotes, Untracked, InSyncToRemote, AheadOfRemote, BehindRemote, DivergedFromAndNewerThanRemote, DivergedFromAndOlderThanRemote
  }

  public static SyncToRemoteStatus noRemotes() {
    return of(Relation.NoRemotes, "");
  }

  public static SyncToRemoteStatus untracked() {
    return of(Relation.Untracked, "");
  }

  private final Relation relation;
  private final String remoteName;
}
