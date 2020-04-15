package com.virtuslab.gitmachete.backend.root;

import static com.virtuslab.gitmachete.backend.root.TestUtils.*;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.BaseGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreBranch;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;

public class GitMacheteRepositoryBuilder_deriveSyncToParentStatusTest {

  private static final GitMacheteRepositoryBuilder gitMacheteRepositoryBuilder = PowerMockito
      .mock(GitMacheteRepositoryBuilder.class);

  private static final TestGitCoreRepositoryFactory repositoryFactory = new TestGitCoreRepositoryFactory();
  private static final TestGitCoreRepository repository = repositoryFactory.getInstance();

  private static final BaseGitCoreCommit MISSING_FORKPOINT = getCommit(null);

  @BeforeClass
  public static void init() {
    Whitebox.setInternalState(gitMacheteRepositoryBuilder, "gitCoreRepositoryFactory", repositoryFactory);
  }

  SyncToParentStatus invokeDeriveSyncToParentStatus(IGitCoreLocalBranch coreLocalBranch,
      IGitCoreBranch parentCoreLocalBranch,
      BaseGitCoreCommit forkPoint) throws Exception {
    return Whitebox.invokeMethod(gitMacheteRepositoryBuilder,
        "deriveSyncToParentStatus",
        repository,
        coreLocalBranch,
        parentCoreLocalBranch,
        forkPoint);
  }

  @Test
  public void branchAndParentPointingSameCommitAndBranchJustCreated_inSync() throws Exception {
    // given
    BaseGitCoreCommit pointedCommit = getCommit(null);
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
    BaseGitCoreCommit pointedCommit = getCommit(null);
    IGitCoreLocalBranch parent = getGitCoreLocalBranch(pointedCommit);
    IGitCoreLocalBranch branch = getGitCoreLocalBranch(pointedCommit);
    // when
    SyncToParentStatus syncToParentStatus = invokeDeriveSyncToParentStatus(branch, parent, MISSING_FORKPOINT);

    // then
    Assert.assertEquals(SyncToParentStatus.Merged, syncToParentStatus);
  }

  @Test
  public void parentPointedCommitIsAncestorOfBranchPointedCommitAndItsForkPoint_inSync() throws Exception {
    // given
    BaseGitCoreCommit parentPointedCommit = getCommit(null);
    BaseGitCoreCommit branchPointedCommit = getCommit(parentPointedCommit);
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
    BaseGitCoreCommit forkPointCommit = getCommit(null);
    BaseGitCoreCommit parentPointedCommit = getCommit(forkPointCommit);
    BaseGitCoreCommit branchPointedCommit = getCommit(parentPointedCommit);
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
    BaseGitCoreCommit branchPointedCommit = getCommit(null);
    BaseGitCoreCommit parentPointedCommit = getCommit(branchPointedCommit);
    IGitCoreLocalBranch parent = getGitCoreLocalBranch(parentPointedCommit);
    IGitCoreLocalBranch branch = getGitCoreLocalBranch(branchPointedCommit);

    // when
    SyncToParentStatus syncToParentStatus = invokeDeriveSyncToParentStatus(branch, parent, MISSING_FORKPOINT);

    // then
    Assert.assertEquals(SyncToParentStatus.Merged, syncToParentStatus);
  }

  @Test
  public void neitherBranchPointedCommitIsAncestorOfParentPointedCommitNorTheOtherWay_outOffSync() throws Exception {
    // given
    BaseGitCoreCommit someCommit = getCommit(null);
    BaseGitCoreCommit parentPointedCommit = getCommit(someCommit);
    BaseGitCoreCommit branchPointedCommit = getCommit(someCommit);
    IGitCoreLocalBranch parent = getGitCoreLocalBranch(parentPointedCommit);
    IGitCoreLocalBranch branch = getGitCoreLocalBranch(branchPointedCommit);

    // when
    SyncToParentStatus syncToParentStatus = invokeDeriveSyncToParentStatus(branch, parent, MISSING_FORKPOINT);

    // then
    Assert.assertEquals(SyncToParentStatus.OutOfSync, syncToParentStatus);
  }
}
