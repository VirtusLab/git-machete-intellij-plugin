package com.virtuslab;

import java.util.Optional;

public interface IRepository {
    Optional<ILocalBranch> getCurrentBranch() throws GitException;
    ILocalBranch getLocalBranch(String branchName) throws GitException;
    IRemoteBranch getRemoteBranch(String branchName) throws GitException;
}
