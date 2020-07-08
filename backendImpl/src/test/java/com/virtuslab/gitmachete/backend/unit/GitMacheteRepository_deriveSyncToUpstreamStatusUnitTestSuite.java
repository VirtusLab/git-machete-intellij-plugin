package com.virtuslab.gitmachete.backend.unit;

import static com.virtuslab.gitmachete.backend.unit.UnitTestUtils.TestGitCoreReflogEntry;
import static com.virtuslab.gitmachete.backend.unit.UnitTestUtils.createGitCoreCommit;
import static com.virtuslab.gitmachete.backend.unit.UnitTestUtils.createGitCoreLocalBranch;

import io.vavr.collection.List;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreLocalBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.SyncToUpstreamStatus;
import com.virtuslab.gitmachete.backend.impl.GitMacheteForkPointCommit;

public class GitMacheteRepository_deriveSyncToUpstreamStatusUnitTestSuite extends BaseGitMacheteRepositoryUnitTestSuite {

  private static final IGitCoreCommit MISSING_FORK_POINT = createGitCoreCommit();

  @SneakyThrows
  private SyncToUpstreamStatus invokeDeriveSyncToUpstreamStatus(
      IGitCoreLocalBranchSnapshot downstreamBranch,
      IGitCoreLocalBranchSnapshot upstreamBranch,
      IGitCoreCommit forkPointCommit) {
    return Whitebox.invokeMethod(aux(), "deriveSyncToUpstreamStatus",
        downstreamBranch, upstreamBranch,
        GitMacheteForkPointCommit.inferred(forkPointCommit, /* containingBranches */ List.empty()));
  }

  @Test
  public void branchAndUpstreamPointingSameCommitAndBranchJustCreated_inSync() {
    // given
    IGitCoreCommit commit = createGitCoreCommit();
    IGitCoreLocalBranchSnapshot upstreamBranch = createGitCoreLocalBranch(commit);
    IGitCoreLocalBranchSnapshot downstreamBranch = createGitCoreLocalBranch(commit);

    // when
    SyncToUpstreamStatus syncToUpstreamStatus = invokeDeriveSyncToUpstreamStatus(downstreamBranch, upstreamBranch,
        MISSING_FORK_POINT);

    // then
    Assert.assertEquals(SyncToUpstreamStatus.InSync, syncToUpstreamStatus);
  }

  @Test
  public void branchAndUpstreamPointingSameCommitAndBranchNotJustCreated_merged() {
    // given
    IGitCoreCommit commit = createGitCoreCommit();
    IGitCoreLocalBranchSnapshot upstreamBranch = createGitCoreLocalBranch(commit);
    IGitCoreLocalBranchSnapshot downstreamBranch = createGitCoreLocalBranch(commit, new TestGitCoreReflogEntry());

    // when
    SyncToUpstreamStatus syncToUpstreamStatus = invokeDeriveSyncToUpstreamStatus(downstreamBranch, upstreamBranch,
        MISSING_FORK_POINT);

    // then
    Assert.assertEquals(SyncToUpstreamStatus.MergedToUpstream, syncToUpstreamStatus);
  }

  @Test
  @SneakyThrows
  public void upstreamPointedCommitIsAncestorOfBranchPointedCommitAndItsForkPoint_inSync() {
    // given
    IGitCoreCommit upstreamCommit = createGitCoreCommit();
    IGitCoreCommit downstreamCommit = createGitCoreCommit();
    IGitCoreLocalBranchSnapshot upstreamBranch = createGitCoreLocalBranch(upstreamCommit);
    IGitCoreLocalBranchSnapshot downstreamBranch = createGitCoreLocalBranch(downstreamCommit);
    PowerMockito.doReturn(true).when(gitCoreRepository).isAncestor(upstreamCommit, downstreamCommit);

    // when
    SyncToUpstreamStatus syncToUpstreamStatus = invokeDeriveSyncToUpstreamStatus(downstreamBranch, upstreamBranch,
        upstreamCommit);

    // then
    Assert.assertEquals(SyncToUpstreamStatus.InSync, syncToUpstreamStatus);
  }

  @Test
  @SneakyThrows
  public void upstreamPointedCommitIsAncestorOfBranchPointedCommitButNotItsForkPoint_inSyncButOffForkPoint() {
    // given
    IGitCoreCommit forkPointCommit = createGitCoreCommit();
    IGitCoreCommit upstreamCommit = createGitCoreCommit();
    IGitCoreCommit downstreamCommit = createGitCoreCommit();
    IGitCoreLocalBranchSnapshot upstreamBranch = createGitCoreLocalBranch(upstreamCommit);
    IGitCoreLocalBranchSnapshot downstreamBranch = createGitCoreLocalBranch(downstreamCommit);
    PowerMockito.doReturn(true).when(gitCoreRepository).isAncestor(upstreamCommit, downstreamCommit);

    // when
    SyncToUpstreamStatus syncToUpstreamStatus = invokeDeriveSyncToUpstreamStatus(downstreamBranch, upstreamBranch,
        forkPointCommit);

    // then
    Assert.assertEquals(SyncToUpstreamStatus.InSyncButForkPointOff, syncToUpstreamStatus);
  }

  @Test
  @SneakyThrows
  public void branchPointedCommitIsAncestorOfUpstreamPointedCommit_merged() {
    // given
    IGitCoreCommit downstreamCommit = createGitCoreCommit();
    IGitCoreCommit upstreamCommit = createGitCoreCommit();
    IGitCoreLocalBranchSnapshot downstreamBranch = createGitCoreLocalBranch(downstreamCommit);
    IGitCoreLocalBranchSnapshot upstreamBranch = createGitCoreLocalBranch(upstreamCommit);
    PowerMockito.doReturn(false).when(gitCoreRepository).isAncestor(upstreamCommit, downstreamCommit);
    PowerMockito.doReturn(true).when(gitCoreRepository).isAncestor(downstreamCommit, upstreamCommit);

    // when
    SyncToUpstreamStatus syncToUpstreamStatus = invokeDeriveSyncToUpstreamStatus(downstreamBranch, upstreamBranch,
        MISSING_FORK_POINT);

    // then
    Assert.assertEquals(SyncToUpstreamStatus.MergedToUpstream, syncToUpstreamStatus);
  }

  @Test
  @SneakyThrows
  public void neitherBranchPointedCommitIsAncestorOfUpstreamPointedCommitNorTheOtherWay_outOfSync() {
    // given
    IGitCoreCommit upstreamCommit = createGitCoreCommit();
    IGitCoreCommit downstreamCommit = createGitCoreCommit();
    IGitCoreLocalBranchSnapshot upstreamBranch = createGitCoreLocalBranch(upstreamCommit);
    IGitCoreLocalBranchSnapshot downstreamBranch = createGitCoreLocalBranch(downstreamCommit);
    PowerMockito.doReturn(false).when(gitCoreRepository).isAncestor(upstreamCommit, downstreamCommit);
    PowerMockito.doReturn(false).when(gitCoreRepository).isAncestor(downstreamCommit, upstreamCommit);

    // when
    SyncToUpstreamStatus syncToUpstreamStatus = invokeDeriveSyncToUpstreamStatus(downstreamBranch, upstreamBranch,
        MISSING_FORK_POINT);

    // then
    Assert.assertEquals(SyncToUpstreamStatus.OutOfSync, syncToUpstreamStatus);
  }
}
