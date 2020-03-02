package com.virtuslab.gitmachete.backendroot;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.virtuslab.branchrelationfile.BranchRelationFile;
import com.virtuslab.branchrelationfile.api.IBranchRelationFile;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitcore.impl.jgit.GitCoreRepository;

public class BackendFactoryModule extends AbstractModule {
	private static final Injector injector = Guice.createInjector(new BackendFactoryModule());

	@Override
	protected void configure() {
		install(new FactoryModuleBuilder()
				.implement(IGitMacheteRepositoryBuilder.class, GitMacheteRepositoryBuilder.class)
				.build(GitMacheteRepositoryBuilderFactory.class));
		install(new FactoryModuleBuilder().implement(IGitCoreRepository.class, GitCoreRepository.class)
				.build(GitCoreRepositoryFactory.class));
		install(new FactoryModuleBuilder().implement(IBranchRelationFile.class, BranchRelationFile.class)
				.build(BranchRelationFileFactory.class));
	}

	public static Injector getInjector() {
		return injector;
	}
}
