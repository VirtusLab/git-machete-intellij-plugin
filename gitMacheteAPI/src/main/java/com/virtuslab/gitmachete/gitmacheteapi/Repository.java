package com.virtuslab.gitmachete.gitmacheteapi;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface Repository {
    List<Branch> getRootBranches();
    Optional<Branch> getCurrentBranch() throws GitMacheteException;
    void addRootBranch(Branch branch);
    Optional<String> getRepositoryName();
    Map<String, Repository> getSubmoduleRepositories() throws GitMacheteException;
}
