package com.virtuslab.gitmachete.gitcore;

import java.util.Optional;

public interface ILocalBranch extends IBranch {
    Optional<IBranchTrackingStatus> getTrackingStatus() throws GitException;
}
