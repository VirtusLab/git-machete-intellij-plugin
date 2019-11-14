package com.virtuslab;

public interface IBranch {
    String branchesPath = "";

    String getName() throws GitException;
    ICommit getPointedCommit() throws GitException;
    ICommit getMergeBase(IBranch branch) throws GitException;
}
