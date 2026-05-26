package com.virtuslab.gitmachete.backend.impl;

import java.lang.ref.SoftReference;
import java.nio.file.Path;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import lombok.val;

import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitcore.api.IGitCoreRepositoryFactory;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryCache;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

public class GitMacheteRepositoryCache implements IGitMacheteRepositoryCache {

  // Keyed on the project root path; for linked worktrees this is the per-worktree checkout dir,
  // which is unique across (repo, worktree) pairs (git itself disallows the same directory being
  // registered as more than one worktree).
  private static Map<Path, SoftReference<GitMacheteRepository>> gitMacheteRepositoryCache = HashMap.empty();

  @Override
  @UIThreadUnsafe
  public IGitMacheteRepository getInstance(Path rootDirectoryPath, Injector injector) throws GitMacheteException {
    val valueReference = gitMacheteRepositoryCache.get(rootDirectoryPath).getOrNull();

    if (valueReference != null) {
      val value = valueReference.get();
      if (value != null) {
        return value;
      }
    }

    val gitCoreRepository = createGitCoreRepository(rootDirectoryPath, injector);
    val newValue = new GitMacheteRepository(gitCoreRepository);
    gitMacheteRepositoryCache = gitMacheteRepositoryCache.put(rootDirectoryPath, new SoftReference<>(newValue));

    return newValue;
  }

  @UIThreadUnsafe
  private IGitCoreRepository createGitCoreRepository(Path rootDirectoryPath, Injector injector)
      throws GitMacheteException {
    try {
      val gitCoreRepositoryFactory = injector.inject(IGitCoreRepositoryFactory.class);
      return gitCoreRepositoryFactory.create(rootDirectoryPath);
    } catch (GitCoreException e) {
      throw new GitMacheteException("Can't create an ${IGitCoreRepository.class.getSimpleName()} instance " +
          "under ${rootDirectoryPath}", e);
    }
  }
}
