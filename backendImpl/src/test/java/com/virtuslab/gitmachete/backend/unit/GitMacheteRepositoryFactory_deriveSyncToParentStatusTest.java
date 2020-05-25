package com.virtuslab.gitmachete.backend.unit;

import static com.virtuslab.gitmachete.backend.unit.TestUtils.TestGitCoreReflogEntry;
import static com.virtuslab.gitmachete.backend.unit.TestUtils.createGitCoreCommit;
import static com.virtuslab.gitmachete.backend.unit.TestUtils.createGitCoreLocalBranch;

import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositoryFactory;

public class GitMacheteRepositoryFactory_deriveSyncToParentStatusTest {

  private static final GitMacheteRepositoryFactory gitMacheteRepositoryFactory = PowerMockito
      .mock(GitMacheteRepositoryFactory.class);

  private final IGitCoreRepository gitCoreRepository = PowerMockito.mock(IGitCoreRepository.class);

  private static final IGitCoreCommit MISSING_FORK_POINT = createGitCoreCommit();

  SyncToParentStatus invokeDeriveSyncToParentStatus(
      IGitCoreLocalBranch childBranch,
      IGitCoreLocalBranch parentBranch,
      IGitCoreCommit forkPointCommit) throws Exception {
    return Whitebox.invokeMethod(gitMacheteRepositoryFactory,
        "deriveSyncToParentStatus",
        gitCoreRepository,
        childBranch,
        parentBranch,
        forkPointCommit);
  }

  @Test
  public void branchAndParentPointingSameCommitAndBranchJustCreated_inSync() throws Exception {
    // given
    IGitCoreCommit commit = createGitCoreCommit();
    IGitCoreLocalBranch parentBranch = createGitCoreLocalBranch(commit);
    IGitCoreLocalBranch childBranch = createGitCoreLocalBranch(commit);

    // when
    SyncToParentStatus syncToParentStatus = invokeDeriveSyncToParentStatus(childBranch, parentBranch, MISSING_FORK_POINT);

    // then
    Assert.assertEquals(SyncToParentStatus.InSync, syncToParentStatus);
  }

  @Test
  public void branchAndParentPointingSameCommitAndBranchNotJustCreated_merged() throws Exception {
    // given
    IGitCoreCommit commit = createGitCoreCommit();
    IGitCoreLocalBranch parentBranch = createGitCoreLocalBranch(commit);
    IGitCoreLocalBranch childBranch = createGitCoreLocalBranch(commit, new TestGitCoreReflogEntry());

    // when
    SyncToParentStatus syncToParentStatus = invokeDeriveSyncToParentStatus(childBranch, parentBranch, MISSING_FORK_POINT);

    // then
    Assert.assertEquals(SyncToParentStatus.MergedToParent, syncToParentStatus);
  }

  @Test
  public void parentPointedCommitIsAncestorOfBranchPointedCommitAndItsForkPoint_inSync() throws Exception {
    // given
    IGitCoreCommit parentCommit = createGitCoreCommit();
    IGitCoreCommit childCommit = createGitCoreCommit();
    IGitCoreLocalBranch parentBranch = createGitCoreLocalBranch(parentCommit);
    IGitCoreLocalBranch childBranch = createGitCoreLocalBranch(childCommit);
    PowerMockito.doReturn(true).when(gitCoreRepository).isAncestor(parentCommit, childCommit);

    // when
    SyncToParentStatus syncToParentStatus = invokeDeriveSyncToParentStatus(childBranch, parentBranch, parentCommit);

    // then
    Assert.assertEquals(SyncToParentStatus.InSync, syncToParentStatus);
  }

  @Test
  public void parentPointedCommitIsAncestorOfBranchPointedCommitButNotItsForkPoint_inSyncButOffForkPoint()
      throws Exception {
    // given
    IGitCoreCommit forkPointCommit = createGitCoreCommit();
    IGitCoreCommit parentCommit = createGitCoreCommit();
    IGitCoreCommit childCommit = createGitCoreCommit();
    IGitCoreLocalBranch parentBranch = createGitCoreLocalBranch(parentCommit);
    IGitCoreLocalBranch childBranch = createGitCoreLocalBranch(childCommit);
    PowerMockito.doReturn(true).when(gitCoreRepository).isAncestor(parentCommit, childCommit);

    // when
    SyncToParentStatus syncToParentStatus = invokeDeriveSyncToParentStatus(childBranch, parentBranch, forkPointCommit);

    // then
    Assert.assertEquals(SyncToParentStatus.InSyncButForkPointOff, syncToParentStatus);
  }

  @Test
  public void branchPointedCommitIsAncestorOfParentPointedCommit_merged() throws Exception {
    // given
    IGitCoreCommit childCommit = createGitCoreCommit();
    IGitCoreCommit parentCommit = createGitCoreCommit();
    IGitCoreLocalBranch childBranch = createGitCoreLocalBranch(childCommit);
    IGitCoreLocalBranch parentBranch = createGitCoreLocalBranch(parentCommit);
    PowerMockito.doReturn(false).when(gitCoreRepository).isAncestor(parentCommit, childCommit);
    PowerMockito.doReturn(true).when(gitCoreRepository).isAncestor(childCommit, parentCommit);

    // when
    SyncToParentStatus syncToParentStatus = invokeDeriveSyncToParentStatus(childBranch, parentBranch, MISSING_FORK_POINT);

    // then
    Assert.assertEquals(SyncToParentStatus.MergedToParent, syncToParentStatus);
  }

  @Test
  public void neitherBranchPointedCommitIsAncestorOfParentPointedCommitNorTheOtherWay_outOfSync() throws Exception {
    // given
    IGitCoreCommit parentCommit = createGitCoreCommit();
    IGitCoreCommit childCommit = createGitCoreCommit();
    IGitCoreLocalBranch parentBranch = createGitCoreLocalBranch(parentCommit);
    IGitCoreLocalBranch childBranch = createGitCoreLocalBranch(childCommit);
    PowerMockito.doReturn(false).when(gitCoreRepository).isAncestor(parentCommit, childCommit);
    PowerMockito.doReturn(false).when(gitCoreRepository).isAncestor(childCommit, parentCommit);

    // when
    SyncToParentStatus syncToParentStatus = invokeDeriveSyncToParentStatus(childBranch, parentBranch, MISSING_FORK_POINT);

    // then
    Assert.assertEquals(SyncToParentStatus.OutOfSync, syncToParentStatus);
  }
}
