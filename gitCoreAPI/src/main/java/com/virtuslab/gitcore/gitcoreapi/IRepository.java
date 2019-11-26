package com.virtuslab.gitcore.gitcoreapi;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public interface IRepository {
    Optional<ILocalBranch> getCurrentBranch() throws GitException;
    ILocalBranch getLocalBranch(String branchName) throws GitException;
    IRemoteBranch getRemoteBranch(String branchName) throws GitException;
    Path getRepositoryPath();
    Path getGitFolderPath();
    Map<String, ISubmoduleEntry> getSubmodules() throws GitException;
}
