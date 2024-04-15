package com.virtuslab.gitmachete.backend.integration;

import java.io.FileInputStream;

import lombok.SneakyThrows;
import lombok.val;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutReader;
import com.virtuslab.branchlayout.impl.readwrite.BranchLayoutReader;
import com.virtuslab.gitcore.api.IGitCoreRepositoryFactory;
import com.virtuslab.gitcore.impl.jgit.GitCoreRepositoryFactory;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryCache;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositoryCache;
import com.virtuslab.gitmachete.testcommon.TestGitRepository;

class BaseIntegrationTestSuite {
  protected static final IGitCoreRepositoryFactory gitCoreRepositoryFactory = new GitCoreRepositoryFactory();
  protected static final IGitMacheteRepositoryCache gitMacheteRepositoryCache = new GitMacheteRepositoryCache();
  protected static final IBranchLayoutReader branchLayoutReader = new BranchLayoutReader();

  protected TestGitRepository repo;
  protected IGitMacheteRepository gitMacheteRepository;
  protected BranchLayout branchLayout;

  @SneakyThrows
  public void setUp(String scriptName) {
    repo = new TestGitRepository(scriptName);
    val injector = new IGitMacheteRepositoryCache.Injector() {
      @Override
      @SuppressWarnings("unchecked")
      public <T> T inject(Class<T> clazz) {
        return (T) gitCoreRepositoryFactory;
      }
    };
    gitMacheteRepository = gitMacheteRepositoryCache.getInstance(repo.rootDirectoryPath, repo.mainGitDirectoryPath,
        repo.worktreeGitDirectoryPath, injector);
    branchLayout = branchLayoutReader.read(new FileInputStream(repo.mainGitDirectoryPath.resolve("machete").toFile()));
  }
}
