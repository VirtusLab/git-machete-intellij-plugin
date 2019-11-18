package com.virtuslab.gitmachete.gitcore;

public interface ILocalBranch extends IBranch {
    IBranchTrackingStatus getTrackingStatus() throws GitException;
}
