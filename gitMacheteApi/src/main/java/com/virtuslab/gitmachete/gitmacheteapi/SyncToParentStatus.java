package com.virtuslab.gitmachete.gitmacheteapi;

public enum SyncToParentStatus {
  Merged(0),
  InSyncButForkPointOff(1),
  OutOfSync(2),
  InSync(3);

  private final int colorId;

  SyncToParentStatus(int colorId) {
    this.colorId = colorId;
  }

  public int getColorId() {
    return colorId;
  }
}
