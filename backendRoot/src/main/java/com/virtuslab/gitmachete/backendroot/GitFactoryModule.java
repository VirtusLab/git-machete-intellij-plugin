package com.virtuslab.gitmachete.backendroot;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.virtuslab.branchrelationfile.BranchRelationFile;
import com.virtuslab.branchrelationfile.api.BranchRelationFileFactory;
import com.virtuslab.branchrelationfile.api.IBranchRelationFile;
import com.virtuslab.gitcore.gitcoreapi.GitCoreRepositoryFactory;
import com.virtuslab.gitcore.gitcoreapi.IGitCoreRepository;
import com.virtuslab.gitcore.gitcorejgit.JGitRepository;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteRepositoryFactory;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.gitmachetejgit.GitMacheteRepository;

public class GitFactoryModule extends AbstractModule {
  private static Injector injector = Guice.createInjector(new GitFactoryModule());

  @Override
  protected void configure() {
    install(
        new FactoryModuleBuilder()
            .implement(IGitMacheteRepository.class, GitMacheteRepository.class)
            .build(GitMacheteRepositoryFactory.class));
    install(
        new FactoryModuleBuilder()
            .implement(IGitCoreRepository.class, JGitRepository.class)
            .build(GitCoreRepositoryFactory.class));
    install(
        new FactoryModuleBuilder()
            .implement(IBranchRelationFile.class, BranchRelationFile.class)
            .build(BranchRelationFileFactory.class));
  }

  public static Injector getInjector() {
    return injector;
  }
}
