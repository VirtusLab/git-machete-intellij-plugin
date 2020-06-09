package com.virtuslab.gitmachete.backend.api;

import lombok.Data;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data(staticConstructor = "of")
public class SyncToRemoteStatus {
  public enum Relation {
    NoRemotes, Untracked, InSyncToRemote, AheadOfRemote, BehindRemote, DivergedFromAndNewerThanRemote, DivergedFromAndOlderThanRemote
  }

  public static SyncToRemoteStatus noRemotes() {
    return of(Relation.NoRemotes, null);
  }

  public static SyncToRemoteStatus untracked() {
    return of(Relation.Untracked, null);
  }

  private final Relation relation;
  private final @Nullable String remoteName;
}
