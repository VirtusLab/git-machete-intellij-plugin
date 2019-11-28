package com.virtuslab.gitcore.gitcoreapi;

import java.util.Optional;

public interface ILocalBranch extends IBranch {
    Optional<IBranchTrackingStatus> getTrackingStatus() throws GitException;
}
