package com.virtuslab.gitmachete.backend.api;

public enum SyncToRemoteStatus {
  NoRemotes, Untracked, InSyncToRemote, AheadOfRemote, BehindRemote, DivergedFromAndNewerThanRemote, DivergedFromAndOlderThanRemote
}
