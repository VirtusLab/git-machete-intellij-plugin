package com.virtuslab.gitmachete.backend.api;

public enum SyncToParentStatus {
  MergedToParent(0), InSyncButForkPointOff(1), OutOfSync(2), InSync(3);

  private final int id;

  SyncToParentStatus(int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }
}
