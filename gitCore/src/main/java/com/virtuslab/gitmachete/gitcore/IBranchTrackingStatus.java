package com.virtuslab.gitmachete.gitcore;

public interface IBranchTrackingStatus {
    boolean isTracked();
    int aheadCount();
    int behindCount();
}
