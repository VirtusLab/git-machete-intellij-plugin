package com.virtuslab.gitmachete.backend.impl;

import io.vavr.collection.Set;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.ILocalBranchReference;
import com.virtuslab.gitmachete.backend.impl.helper.CreateGitMacheteRepositoryHelper;
import com.virtuslab.gitmachete.backend.impl.helper.DiscoverGitMacheteRepositoryHelper;
import com.virtuslab.gitmachete.backend.impl.helper.Helper;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

public class GitMacheteRepository implements IGitMacheteRepository {

  private final IGitCoreRepository gitCoreRepository;
  private final StatusBranchHookExecutor statusHookExecutor;

  private static final int NUMBER_OF_MOST_RECENTLY_CHECKED_OUT_BRANCHES_FOR_DISCOVER = 10;

  @UIThreadUnsafe
  public GitMacheteRepository(IGitCoreRepository gitCoreRepository) {
    this.gitCoreRepository = gitCoreRepository;
    this.statusHookExecutor = new StatusBranchHookExecutor(gitCoreRepository);
  }

  @Override
  @UIThreadUnsafe
  public IGitMacheteRepositorySnapshot createSnapshotForLayout(BranchLayout branchLayout) throws GitMacheteException {
    try {
      val helper = new CreateGitMacheteRepositoryHelper(gitCoreRepository, statusHookExecutor);
      return helper.createSnapshot(branchLayout);
    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

  @Override
  @UIThreadUnsafe
  public @Nullable ILocalBranchReference inferParentForLocalBranch(
      Set<String> eligibleLocalBranchNames,
      String localBranchName) throws GitMacheteException {
    try {
      val helper = new Helper(gitCoreRepository);
      return helper.inferParentForLocalBranch(eligibleLocalBranchNames, localBranchName);
    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

  @Override
  @UIThreadUnsafe
  public IGitMacheteRepositorySnapshot discoverLayoutAndCreateSnapshot() throws GitMacheteException {
    try {
      val helper = new DiscoverGitMacheteRepositoryHelper(gitCoreRepository, statusHookExecutor);
      return helper.discoverLayoutAndCreateSnapshot(NUMBER_OF_MOST_RECENTLY_CHECKED_OUT_BRANCHES_FOR_DISCOVER);
    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

}
