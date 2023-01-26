package com.virtuslab.gitmachete.backend.impl;

import com.virtuslab.gitcore.api.IGitCoreRepositoryFactory;

@FunctionalInterface
public interface IDependencyResolver {
  IGitCoreRepositoryFactory getGitCoreRepositoryFactory();
}
