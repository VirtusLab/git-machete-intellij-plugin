package com.virtuslab;

public interface IRepository {
    IBranch getCurrentBranch() throws GitException;
    IBranch getBranch(String branchName) throws GitException;
}
