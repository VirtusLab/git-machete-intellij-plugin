package com.virtuslab.gitmachete.backend.integration;

import java.io.FileInputStream;

import lombok.SneakyThrows;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutReader;
import com.virtuslab.branchlayout.impl.readwrite.BranchLayoutReader;
import com.virtuslab.gitcore.api.IGitCoreRepositoryFactory;
import com.virtuslab.gitcore.impl.jgit.GitCoreRepositoryFactory;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryCache;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositoryCache;
import com.virtuslab.gitmachete.testcommon.GitRepositoryBackedIntegrationTestSuiteInitializer;

class BaseIntegrationTestSuite {
  protected static final IGitCoreRepositoryFactory gitCoreRepositoryFactory = new GitCoreRepositoryFactory();
  protected static final IGitMacheteRepositoryCache gitMacheteRepositoryCache = new GitMacheteRepositoryCache(
      () -> gitCoreRepositoryFactory);
  protected static final IBranchLayoutReader branchLayoutReader = new BranchLayoutReader();

  protected IGitMacheteRepository gitMacheteRepository;
  protected BranchLayout branchLayout;
  protected GitRepositoryBackedIntegrationTestSuiteInitializer it;

  @SneakyThrows
  public void setUp(String scriptName) {
    it = new GitRepositoryBackedIntegrationTestSuiteInitializer(scriptName);
    gitMacheteRepository = gitMacheteRepositoryCache.getInstance(it.rootDirectoryPath, it.mainGitDirectoryPath,
        it.worktreeGitDirectoryPath);
    branchLayout = branchLayoutReader.read(new FileInputStream(it.mainGitDirectoryPath.resolve("machete").toFile()));
  }
}
