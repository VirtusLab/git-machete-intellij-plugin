package com.virtuslab.gitmachete.backend.impl;

import java.nio.file.Path;
import java.util.WeakHashMap;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.val;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitcore.api.IGitCoreRepositoryFactory;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryCache;
import com.virtuslab.gitmachete.backend.impl.hooks.PreRebaseHookExecutor;
import com.virtuslab.gitmachete.backend.impl.hooks.StatusBranchHookExecutor;

public class GitMacheteRepositoryCache implements IGitMacheteRepositoryCache {

  private final IGitCoreRepositoryFactory gitCoreRepositoryFactory;

  private static final java.util.Map<Tuple2<Path, Path>, GitMacheteRepository> gitMacheteRepositoryCache = new WeakHashMap<>();

  public GitMacheteRepositoryCache() {
    gitCoreRepositoryFactory = RuntimeBinding.instantiateSoleImplementingClass(IGitCoreRepositoryFactory.class);
  }

  @Override
  public IGitMacheteRepository getInstance(Path rootDirectoryPath, Path mainGitDirectoryPath, Path worktreeGitDirectoryPath)
      throws GitMacheteException {
    val key = Tuple.of(rootDirectoryPath, worktreeGitDirectoryPath);
    if (!gitMacheteRepositoryCache.containsKey(key)) {
      val gitCoreRepository = createGitCoreRepository(rootDirectoryPath, mainGitDirectoryPath, worktreeGitDirectoryPath);
      val statusHookExecutor = StatusBranchHookExecutor.of(gitCoreRepository);
      val preRebaseHookExecutor = PreRebaseHookExecutor.of(gitCoreRepository);
      val value = new GitMacheteRepository(gitCoreRepository, statusHookExecutor, preRebaseHookExecutor);
      gitMacheteRepositoryCache.put(key, value);
    }
    return gitMacheteRepositoryCache.get(key);
  }

  private IGitCoreRepository createGitCoreRepository(Path rootDirectoryPath, Path mainGitDirectoryPath,
      Path worktreeGitDirectoryPath) throws GitMacheteException {
    try {
      return gitCoreRepositoryFactory.create(rootDirectoryPath, mainGitDirectoryPath, worktreeGitDirectoryPath);
    } catch (GitCoreException e) {
      throw new GitMacheteException("Can't create an ${IGitCoreRepository.class.getSimpleName()} instance " +
          "under ${rootDirectoryPath} (with main git directory under ${mainGitDirectoryPath} " +
          "and worktree git directory under ${worktreeGitDirectoryPath})", e);
    }
  }
}
