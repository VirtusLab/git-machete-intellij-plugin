package com.virtuslab.gitmachete.backend.unit;

import static com.virtuslab.gitmachete.backend.unit.TestUtils.TestGitCoreRepository;
import static com.virtuslab.gitmachete.backend.unit.TestUtils.TestGitCoreRepositoryFactory;
import static com.virtuslab.gitmachete.backend.unit.TestUtils.getCommit;
import static com.virtuslab.gitmachete.backend.unit.TestUtils.getGitCoreLocalBranch;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.IGitCoreBranch;
import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositoryFactory;

public class GitMacheteRepositoryFactory_deriveSyncToParentStatusTest {

  private static final GitMacheteRepositoryFactory gitMacheteRepositoryFactory = PowerMockito
      .mock(GitMacheteRepositoryFactory.class);

  private static final TestGitCoreRepositoryFactory gitCoreRepositoryFactory = new TestGitCoreRepositoryFactory();
  private static final TestGitCoreRepository gitCoreRepository = gitCoreRepositoryFactory.getInstance();

  private static final IGitCoreCommit MISSING_FORKPOINT = getCommit(null);

  @BeforeClass
  public static void init() {
    Whitebox.setInternalState(gitMacheteRepositoryFactory, "gitCoreRepositoryFactory", gitCoreRepositoryFactory);
  }

  SyncToParentStatus invokeDeriveSyncToParentStatus(IGitCoreLocalBranch coreLocalBranch,
      IGitCoreBranch parentCoreLocalBranch,
      IGitCoreCommit forkPoint) throws Exception {
    return Whitebox.invokeMethod(gitMacheteRepositoryFactory,
        "deriveSyncToParentStatus",
        gitCoreRepository,
        coreLocalBranch,
        parentCoreLocalBranch,
        forkPoint);
  }

  @Test
  public void branchAndParentPointingSameCommitAndBranchJustCreated_inSync() throws Exception {
    // given
    IGitCoreCommit pointedCommit = getCommit(null);
    IGitCoreLocalBranch parent = getGitCoreLocalBranch(pointedCommit);
    IGitCoreLocalBranch branch = getGitCoreLocalBranch(pointedCommit, /* forkPoint */ null,
        /* hasJustBeenCreated */ true);

    // when
    SyncToParentStatus syncToParentStatus = invokeDeriveSyncToParentStatus(branch, parent, MISSING_FORKPOINT);

    // then
    Assert.assertEquals(SyncToParentStatus.InSync, syncToParentStatus);
  }

  @Test
  public void branchAndParentPointingSameCommitAndBranchNotJustCreated_merged() throws Exception {
    // given
    IGitCoreCommit pointedCommit = getCommit(null);
    IGitCoreLocalBranch parent = getGitCoreLocalBranch(pointedCommit);
    IGitCoreLocalBranch branch = getGitCoreLocalBranch(pointedCommit);
    // when
    SyncToParentStatus syncToParentStatus = invokeDeriveSyncToParentStatus(branch, parent, MISSING_FORKPOINT);

    // then
    Assert.assertEquals(SyncToParentStatus.MergedToParent, syncToParentStatus);
  }

  @Test
  public void parentPointedCommitIsAncestorOfBranchPointedCommitAndItsForkPoint_inSync() throws Exception {
    // given
    IGitCoreCommit parentPointedCommit = getCommit(null);
    IGitCoreCommit branchPointedCommit = getCommit(parentPointedCommit);
    IGitCoreLocalBranch parent = getGitCoreLocalBranch(parentPointedCommit);
    IGitCoreLocalBranch branch = getGitCoreLocalBranch(branchPointedCommit,
        /* forkPoint */ parentPointedCommit);

    // when
    SyncToParentStatus syncToParentStatus = invokeDeriveSyncToParentStatus(branch, parent, parentPointedCommit);

    // then
    Assert.assertEquals(SyncToParentStatus.InSync, syncToParentStatus);
  }

  @Test
  public void parentPointedCommitIsAncestorOfBranchPointedCommitButNotItsForkPoint_inSyncButOffForkPoint()
      throws Exception {
    // given
    IGitCoreCommit forkPointCommit = getCommit(null);
    IGitCoreCommit parentPointedCommit = getCommit(forkPointCommit);
    IGitCoreCommit branchPointedCommit = getCommit(parentPointedCommit);
    IGitCoreLocalBranch parent = getGitCoreLocalBranch(parentPointedCommit);
    IGitCoreLocalBranch branch = getGitCoreLocalBranch(branchPointedCommit, forkPointCommit);

    // when
    SyncToParentStatus syncToParentStatus = invokeDeriveSyncToParentStatus(branch, parent, forkPointCommit);

    // then
    Assert.assertEquals(SyncToParentStatus.InSyncButForkPointOff, syncToParentStatus);
  }

  @Test
  public void branchPointedCommitIsAncestorOfParentPointedCommit_merged() throws Exception {
    // given
    IGitCoreCommit branchPointedCommit = getCommit(null);
    IGitCoreCommit parentPointedCommit = getCommit(branchPointedCommit);
    IGitCoreLocalBranch parent = getGitCoreLocalBranch(parentPointedCommit);
    IGitCoreLocalBranch branch = getGitCoreLocalBranch(branchPointedCommit);

    // when
    SyncToParentStatus syncToParentStatus = invokeDeriveSyncToParentStatus(branch, parent, MISSING_FORKPOINT);

    // then
    Assert.assertEquals(SyncToParentStatus.MergedToParent, syncToParentStatus);
  }

  @Test
  public void neitherBranchPointedCommitIsAncestorOfParentPointedCommitNorTheOtherWay_outOffSync() throws Exception {
    // given
    IGitCoreCommit someCommit = getCommit(null);
    IGitCoreCommit parentPointedCommit = getCommit(someCommit);
    IGitCoreCommit branchPointedCommit = getCommit(someCommit);
    IGitCoreLocalBranch parent = getGitCoreLocalBranch(parentPointedCommit);
    IGitCoreLocalBranch branch = getGitCoreLocalBranch(branchPointedCommit);

    // when
    SyncToParentStatus syncToParentStatus = invokeDeriveSyncToParentStatus(branch, parent, MISSING_FORKPOINT);

    // then
    Assert.assertEquals(SyncToParentStatus.OutOfSync, syncToParentStatus);
  }
}
