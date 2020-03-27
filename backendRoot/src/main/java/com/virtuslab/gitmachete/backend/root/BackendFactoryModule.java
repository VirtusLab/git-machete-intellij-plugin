package com.virtuslab.gitmachete.backend.root;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;

import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitcore.impl.jgit.GitCoreRepository;

public class BackendFactoryModule extends AbstractModule {
  private static final Injector injector = Guice.createInjector(new BackendFactoryModule());

  @Override
  protected void configure() {
    install(new FactoryModuleBuilder().implement(IGitMacheteRepositoryBuilder.class, GitMacheteRepositoryBuilder.class)
        .build(IGitMacheteRepositoryBuilderFactory.class));
    install(new FactoryModuleBuilder().implement(IGitCoreRepository.class, GitCoreRepository.class)
        .build(IGitCoreRepositoryFactory.class));
  }

  public static Injector getInjector() {
    return injector;
  }
}
