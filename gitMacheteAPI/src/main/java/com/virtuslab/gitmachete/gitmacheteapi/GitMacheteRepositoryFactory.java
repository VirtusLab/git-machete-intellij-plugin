package com.virtuslab.gitmachete.gitmacheteapi;

import com.virtuslab.gitcore.gitcoreapi.IRepository;
import com.virtuslab.gitmachete.gitmacheteapi.Repository;

import java.nio.file.Path;

public interface GitMacheteRepositoryFactory {
    Repository create(IRepository repo);
}
