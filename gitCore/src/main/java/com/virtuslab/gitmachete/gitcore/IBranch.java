package com.virtuslab.gitmachete.gitcore;

import java.util.Optional;

public interface IBranch {
    String getName() throws GitException;
    String getFullName() throws GitException;
    ICommit getPointedCommit() throws GitException;
    Optional<ICommit> getMergeBase(IBranch branch) throws GitException;
    Optional<ICommit> getForkPoint(IBranch parentBranch) throws GitException;
    boolean isLocal();
}
