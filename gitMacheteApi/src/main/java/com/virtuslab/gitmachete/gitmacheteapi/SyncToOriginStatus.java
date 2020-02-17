package com.virtuslab.gitmachete.gitmacheteapi;

public enum SyncToOriginStatus {
  Untracked(0),
  Ahead(1),
  Behind(2),
  Diverged(3),
  InSync(4);

  private final int id;

  SyncToOriginStatus(int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }
}
