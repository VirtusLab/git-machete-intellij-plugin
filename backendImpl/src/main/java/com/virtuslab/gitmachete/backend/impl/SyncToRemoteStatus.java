package com.virtuslab.gitmachete.backend.impl;

import lombok.Data;

import com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus;

@Data(staticConstructor = "of")
public class SyncToRemoteStatus implements ISyncToRemoteStatus {
  private final Relation relation;
  private final String remoteName;
}
