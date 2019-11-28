package com.virtuslab.gitmachete.gitmacheteapi;

import com.virtuslab.gitcore.gitcoreapi.IGitCoreRepository;

import java.nio.file.Path;
import java.util.Optional;

public interface GitMacheteRepositoryFactory {
    IGitMacheteRepository create(Path pathToRepoRoot, Optional<String> repositoryName) throws GitMacheteException;
    //IGitMacheteRepository create(IGitCoreRepository repo, Optional<String> name);
}
