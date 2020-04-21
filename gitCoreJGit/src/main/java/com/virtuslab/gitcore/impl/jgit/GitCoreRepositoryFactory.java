package com.virtuslab.gitcore.impl.jgit;

import java.nio.file.Path;

import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitcore.api.IGitCoreRepositoryFactory;

public class GitCoreRepositoryFactory implements IGitCoreRepositoryFactory {
  public IGitCoreRepository create(Path mainDirectoryPath, Path gitDirectoryPath) throws GitCoreException {
    return new GitCoreRepository(mainDirectoryPath, gitDirectoryPath);
  }
}
