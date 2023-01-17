package com.virtuslab.gitmachete.backend.unit;

import static com.virtuslab.gitmachete.backend.unit.UnitTestUtils.TestGitCoreReflogEntry;
import static com.virtuslab.gitmachete.backend.unit.UnitTestUtils.createGitCoreCommit;
import static com.virtuslab.gitmachete.backend.unit.UnitTestUtils.createGitCoreLocalBranch;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vavr.collection.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreLocalBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.impl.ForkPointCommitOfManagedBranch;

public class GitMacheteRepository_deriveSyncToParentStatusUnitTestSuite extends BaseGitMacheteRepositoryUnitTestSuite {

  private static final IGitCoreCommit MISSING_FORK_POINT = createGitCoreCommit();

  @SneakyThrows
  private SyncToParentStatus invokeDeriveSyncToParentStatus(
      IGitCoreLocalBranchSnapshot childBranch,
      IGitCoreLocalBranchSnapshot parentBranch,
      IGitCoreCommit forkPointCommit) {
    return Whitebox.invokeMethod(aux(), "deriveSyncToParentStatus",
        childBranch, parentBranch,
        ForkPointCommitOfManagedBranch.inferred(forkPointCommit, /* containingBranches */ List.empty()));
  }

  @Test
  public void branchAndParentPointingSameCommitAndBranchJustCreated_inSync() {
    // given
    IGitCoreCommit commit = createGitCoreCommit();
    IGitCoreLocalBranchSnapshot parentBranch = createGitCoreLocalBranch(commit);
    IGitCoreLocalBranchSnapshot childBranch = createGitCoreLocalBranch(commit);

    // when
    SyncToParentStatus syncToParentStatus = invokeDeriveSyncToParentStatus(childBranch, parentBranch, MISSING_FORK_POINT);

    // then
    assertEquals(SyncToParentStatus.InSync, syncToParentStatus);
  }

  @Test
  public void branchAndParentPointingSameCommitAndBranchNotJustCreated_merged() {
    // given
    IGitCoreCommit commit = createGitCoreCommit();
    IGitCoreLocalBranchSnapshot parentBranch = createGitCoreLocalBranch(commit);
    IGitCoreLocalBranchSnapshot childBranch = createGitCoreLocalBranch(commit, new TestGitCoreReflogEntry());

    // when
    SyncToParentStatus syncToParentStatus = invokeDeriveSyncToParentStatus(childBranch, parentBranch, MISSING_FORK_POINT);

    // then
    assertEquals(SyncToParentStatus.MergedToParent, syncToParentStatus);
  }

  @Test
  @SneakyThrows
  public void parentPointedCommitIsAncestorOfBranchPointedCommitAndItsForkPoint_inSync() {
    // given
    IGitCoreCommit parentCommit = createGitCoreCommit();
    IGitCoreCommit childCommit = createGitCoreCommit();
    IGitCoreLocalBranchSnapshot parentBranch = createGitCoreLocalBranch(parentCommit);
    IGitCoreLocalBranchSnapshot childBranch = createGitCoreLocalBranch(childCommit);
    PowerMockito.doReturn(true).when(gitCoreRepository).isAncestorOrEqual(parentCommit, childCommit);

    // when
    SyncToParentStatus syncToParentStatus = invokeDeriveSyncToParentStatus(childBranch, parentBranch, parentCommit);

    // then
    assertEquals(SyncToParentStatus.InSync, syncToParentStatus);
  }

  @Test
  @SneakyThrows
  public void parentPointedCommitIsAncestorOfBranchPointedCommitButNotItsForkPoint_inSyncButOffForkPoint() {
    // given
    IGitCoreCommit forkPointCommit = createGitCoreCommit();
    IGitCoreCommit parentCommit = createGitCoreCommit();
    IGitCoreCommit childCommit = createGitCoreCommit();
    IGitCoreLocalBranchSnapshot parentBranch = createGitCoreLocalBranch(parentCommit);
    IGitCoreLocalBranchSnapshot childBranch = createGitCoreLocalBranch(childCommit);
    PowerMockito.doReturn(true).when(gitCoreRepository).isAncestorOrEqual(parentCommit, childCommit);

    // when
    SyncToParentStatus syncToParentStatus = invokeDeriveSyncToParentStatus(childBranch, parentBranch, forkPointCommit);

    // then
    assertEquals(SyncToParentStatus.InSyncButForkPointOff, syncToParentStatus);
  }

  @Test
  @SneakyThrows
  public void branchPointedCommitIsAncestorOfParentPointedCommit_merged() {
    // given
    IGitCoreCommit childCommit = createGitCoreCommit();
    IGitCoreCommit parentCommit = createGitCoreCommit();
    IGitCoreLocalBranchSnapshot childBranch = createGitCoreLocalBranch(childCommit);
    IGitCoreLocalBranchSnapshot parentBranch = createGitCoreLocalBranch(parentCommit);
    PowerMockito.doReturn(false).when(gitCoreRepository).isAncestorOrEqual(parentCommit, childCommit);
    PowerMockito.doReturn(true).when(gitCoreRepository).isAncestorOrEqual(childCommit, parentCommit);

    // when
    SyncToParentStatus syncToParentStatus = invokeDeriveSyncToParentStatus(childBranch, parentBranch, MISSING_FORK_POINT);

    // then
    assertEquals(SyncToParentStatus.OutOfSync, syncToParentStatus);
  }

  @Test
  @SneakyThrows
  public void neitherBranchPointedCommitIsAncestorOfParentPointedCommitNorTheOtherWay_outOfSync() {
    // given
    IGitCoreCommit parentCommit = createGitCoreCommit();
    IGitCoreCommit childCommit = createGitCoreCommit();
    IGitCoreLocalBranchSnapshot parentBranch = createGitCoreLocalBranch(parentCommit);
    IGitCoreLocalBranchSnapshot childBranch = createGitCoreLocalBranch(childCommit);
    PowerMockito.doReturn(false).when(gitCoreRepository).isAncestorOrEqual(parentCommit, childCommit);
    PowerMockito.doReturn(false).when(gitCoreRepository).isAncestorOrEqual(childCommit, parentCommit);
    PowerMockito.doReturn(List.empty()).when(gitCoreRepository).deriveCommitRange(parentCommit, childCommit);

    // when
    SyncToParentStatus syncToParentStatus = invokeDeriveSyncToParentStatus(childBranch, parentBranch, MISSING_FORK_POINT);

    // then
    assertEquals(SyncToParentStatus.OutOfSync, syncToParentStatus);
  }
}
