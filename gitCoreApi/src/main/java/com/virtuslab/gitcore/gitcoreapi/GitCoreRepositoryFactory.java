package com.virtuslab.gitcore.gitcoreapi;

import com.virtuslab.gitcore.gitcoreapi.IRepository;

import java.nio.file.Path;

public interface GitCoreRepositoryFactory {
    IRepository create(Path pathToRoot);
}
