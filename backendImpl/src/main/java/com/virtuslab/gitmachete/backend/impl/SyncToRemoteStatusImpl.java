package com.virtuslab.gitmachete.backend.impl;

import lombok.Data;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;

@Data(staticConstructor = "of")
public class SyncToRemoteStatusImpl implements SyncToRemoteStatus {
  private final Status status;
  private final String remoteName;
}
