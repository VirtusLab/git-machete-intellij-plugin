package com.virtuslab.gitmachete.gitmacheteapi;

import java.util.List;
import java.util.Optional;

public interface Repository {
    List<Branch> getRootBranches();
    Optional<Branch> getCurrentBranch() throws GitMacheteException;
}
