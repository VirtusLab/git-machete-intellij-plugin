package com.virtuslab.gitmachete.backend.impl;

import java.lang.ref.SoftReference;
import java.nio.file.Path;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
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

  private static io.vavr.collection.Map<Tuple2<Path, Path>, SoftReference<GitMacheteRepository>> gitMacheteRepositoryCache = HashMap
      .empty();

  public GitMacheteRepositoryCache() {
    gitCoreRepositoryFactory = RuntimeBinding.instantiateSoleImplementingClass(IGitCoreRepositoryFactory.class);
  }

  @Override
  public IGitMacheteRepository getInstance(Path rootDirectoryPath, Path mainGitDirectoryPath,
      Path worktreeGitDirectoryPath)
      throws GitMacheteException {
    val key = Tuple.of(rootDirectoryPath, worktreeGitDirectoryPath);
    val valueReferenceOption = gitMacheteRepositoryCache.get(key);

    if (!valueReferenceOption.isEmpty()) {
      val value = valueReferenceOption.get().get();
      if (value != null) {
        val sep = System.lineSeparator();
        return value;
      }
    }

    val gitCoreRepository = createGitCoreRepository(rootDirectoryPath, mainGitDirectoryPath, worktreeGitDirectoryPath);
    val statusHookExecutor = StatusBranchHookExecutor.of(gitCoreRepository);
    val preRebaseHookExecutor = PreRebaseHookExecutor.of(gitCoreRepository);
    val newValue = new GitMacheteRepository(gitCoreRepository, statusHookExecutor, preRebaseHookExecutor);

    gitMacheteRepositoryCache = gitMacheteRepositoryCache.put(key, new SoftReference<>(newValue));
    return newValue;
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
