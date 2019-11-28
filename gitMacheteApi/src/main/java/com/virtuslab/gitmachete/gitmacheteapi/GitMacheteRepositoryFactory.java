package com.virtuslab.gitmachete.gitmacheteapi;

import com.virtuslab.gitcore.gitcoreapi.IRepository;

import java.util.Optional;

public interface GitMacheteRepositoryFactory {
    Repository create(IRepository repo, Optional<String> name);
}
