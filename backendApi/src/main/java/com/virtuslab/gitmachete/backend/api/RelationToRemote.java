package com.virtuslab.gitmachete.backend.api;

import lombok.Data;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data(staticConstructor = "of")
// So that Interning Checker doesn't complain about enum comparison (by `equals` and not by `==`) in Lombok-generated `equals`
@SuppressWarnings("interning:unnecessary.equals")
public class RelationToRemote {
  // TODO (#499): Resolve "status" (to parent) and "relation" (to remote) inconsistency

  public static RelationToRemote noRemotes() {
    return of(SyncToRemoteStatus.NoRemotes, null);
  }

  public static RelationToRemote untracked() {
    return of(SyncToRemoteStatus.Untracked, null);
  }

  private final SyncToRemoteStatus syncToRemoteStatus;
  private final @Nullable String remoteName;
}
