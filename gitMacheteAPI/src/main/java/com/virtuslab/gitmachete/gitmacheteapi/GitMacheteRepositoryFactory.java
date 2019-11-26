package com.virtuslab.gitmachete.gitmacheteapi;

import com.virtuslab.gitcore.gitcoreapi.IRepository;

public interface GitMacheteRepositoryFactory {
    Repository create(IRepository repo);
}
