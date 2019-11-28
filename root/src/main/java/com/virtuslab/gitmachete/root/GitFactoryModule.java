package com.virtuslab.gitmachete.root;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.virtuslab.gitcore.gitcoreapi.GitCoreRepositoryFactory;
import com.virtuslab.gitcore.gitcoreapi.IRepository;
import com.virtuslab.gitcore.gitcorejgit.JGitRepository;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteRepositoryFactory;
import com.virtuslab.gitmachete.gitmacheteapi.Repository;
import com.virtuslab.gitmachete.gitmachetejgit.GitMacheteLoader;
import com.virtuslab.gitmachete.gitmachetejgit.GitMacheteLoaderFactory;
import com.virtuslab.gitmachete.gitmachetejgit.GitMacheteRepository;

public class GitFactoryModule extends AbstractModule {
    private static Injector injector = Guice.createInjector(new GitFactoryModule());
    @Override
    protected void configure() {
        install(new FactoryModuleBuilder().implement(Repository.class, GitMacheteRepository.class).build(GitMacheteRepositoryFactory.class));
        install(new FactoryModuleBuilder().implement(IRepository.class, JGitRepository.class).build(GitCoreRepositoryFactory.class));
        install(new FactoryModuleBuilder().implement(GitMacheteLoader.class, GitMacheteLoader.class).build(GitMacheteLoaderFactory.class));
    }

    public static Injector getInjector() {
        return injector;
    }
}
