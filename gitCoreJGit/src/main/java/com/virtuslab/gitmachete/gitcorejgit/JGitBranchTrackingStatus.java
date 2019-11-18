package com.virtuslab.gitmachete.gitcorejgit;

import com.virtuslab.gitmachete.gitcore.IBranchTrackingStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.text.MessageFormat;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JGitBranchTrackingStatus implements IBranchTrackingStatus {
    private boolean isTracked;
    private int ahead;
    private int behind;

    @Override
    public boolean isTracked() {
        return isTracked;
    }

    @Override
    public int aheadCount() {
        return isTracked() ? ahead : -1;
    }

    @Override
    public int behindCount() {
        return isTracked() ? behind : -1;
    }

    static JGitBranchTrackingStatus buildUntracked() {
        return new JGitBranchTrackingStatus(false, -1, -1);
    }

    static JGitBranchTrackingStatus buildTracked(int ahead, int behind) {
        return new JGitBranchTrackingStatus(true, ahead, behind);
    }

    @Override
    public String toString() {
        return isTracked ? MessageFormat.format( "Is tracked: ahead - {0}; behind - {1}", ahead, behind) : "Is untracked";
    }
}
