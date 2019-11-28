package com.virtuslab.gitmachete.gitmacheteapi;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface IGitMacheteRepository {
    List<IGitMacheteBranch> getRootBranches();
    void addRootBranch(IGitMacheteBranch branch);
    Optional<IGitMacheteBranch> getCurrentBranch() throws GitMacheteException;
    Optional<String> getRepositoryName();
    Map<String, IGitMacheteRepository> getSubmoduleRepositories() throws GitMacheteException;
}
