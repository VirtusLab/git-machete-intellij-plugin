package com.virtuslab.gitmachete.backend.impl;

import java.lang.ref.SoftReference;
import java.nio.file.Path;

import com.intellij.openapi.application.ApplicationManager;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import lombok.val;

import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitcore.api.IGitCoreRepositoryFactory;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryCache;
import com.virtuslab.gitmachete.backend.impl.hooks.PreRebaseHookExecutor;
import com.virtuslab.gitmachete.backend.impl.hooks.StatusBranchHookExecutor;
import com.virtuslab.gitmachete.backend.impl.repository.GitMacheteRepository;

public class GitMacheteRepositoryCache implements IGitMacheteRepositoryCache {

  private final IGitCoreRepositoryFactory gitCoreRepositoryFactory;

  private static Map<Tuple2<Path, Path>, SoftReference<GitMacheteRepository>> gitMacheteRepositoryCache = HashMap.empty();

  public GitMacheteRepositoryCache() {
    gitCoreRepositoryFactory = ApplicationManager.getApplication().getService(IGitCoreRepositoryFactory.class);
  }

  @Override
  public IGitMacheteRepository getInstance(Path rootDirectoryPath, Path mainGitDirectoryPath,
      Path worktreeGitDirectoryPath)
      throws GitMacheteException {
    val key = Tuple.of(rootDirectoryPath, worktreeGitDirectoryPath);
    val valueReference = gitMacheteRepositoryCache.get(key).getOrNull();

    if (valueReference != null) {
      val value = valueReference.get();
      if (value != null) {
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
