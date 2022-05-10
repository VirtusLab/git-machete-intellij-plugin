package com.virtuslab.gitmachete.backend.api;

import lombok.Data;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data(staticConstructor = "of")
// So that Interning Checker doesn't complain about enum comparison (by `equals` and not by `==`) in Lombok-generated `equals`
@SuppressWarnings("interning:unnecessary.equals")
public class RelationToRemote {

  public static RelationToRemote noRemotes() {
    return of(SyncToRemoteStatus.NoRemotes, null);
  }

  public static RelationToRemote untracked() {
    return of(SyncToRemoteStatus.Untracked, null);
  }

  private final SyncToRemoteStatus syncToRemoteStatus;
  private final @Nullable String remoteName;
}
