package com.virtuslab;

public interface IBranch {
    String getName() throws GitException;
    ICommit getPointedCommit() throws GitException;
    ICommit getForkPoint(IBranch branch) throws GitException;
}
