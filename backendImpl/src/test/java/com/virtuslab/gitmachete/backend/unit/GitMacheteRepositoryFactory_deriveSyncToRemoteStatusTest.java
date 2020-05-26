package com.virtuslab.gitmachete.backend.unit;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import io.vavr.control.Option;
import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.GitCoreBranchTrackingStatus;
import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranch;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;

public class GitMacheteRepositoryFactory_deriveSyncToRemoteStatusTest extends BaseGitMacheteRepositoryFactoryTest {

  private final IGitCoreLocalBranch coreLocalBranch = PowerMockito.mock(IGitCoreLocalBranch.class);
  private final IGitCoreRemoteBranch coreRemoteBranch = PowerMockito.mock(IGitCoreRemoteBranch.class);
  private final IGitCoreCommit coreLocalBranchCommit = PowerMockito.mock(IGitCoreCommit.class);
  private final IGitCoreCommit coreRemoteBranchCommit = PowerMockito.mock(IGitCoreCommit.class);

  private SyncToRemoteStatus invokeDeriveSyncToRemoteStatus(IGitCoreLocalBranch coreLocalBranch) throws Exception {
    return Whitebox.invokeMethod(aux, "deriveSyncToRemoteStatus", coreLocalBranch);
  }

  @Test
  public void deriveSyncToRemoteStatus_Untracked() throws Exception {
    // given
    PowerMockito.doReturn(Option.none()).when(coreLocalBranch).getRemoteTrackingBranch();

    // when
    SyncToRemoteStatus syncToRemoteStatus = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Relation.Untracked, syncToRemoteStatus.getRelation());
  }

  @Test
  public void deriveSyncToRemoteStatus_DivergedAndNewerThan() throws Exception {
    // given
    var branchTrackingStatus = GitCoreBranchTrackingStatus.of(1, 1);
    PowerMockito.doReturn(Option.of(branchTrackingStatus)).when(gitCoreRepository).deriveRemoteTrackingStatus(coreLocalBranch);
    PowerMockito.doReturn(Option.of(coreRemoteBranch)).when(coreLocalBranch).getRemoteTrackingBranch();
    PowerMockito.doReturn(coreLocalBranchCommit).when(coreLocalBranch).derivePointedCommit();
    PowerMockito.doReturn(coreRemoteBranchCommit).when(coreRemoteBranch).derivePointedCommit();

    Instant newerInstant = Instant.parse("2000-05-01T10:00:00Z");
    Instant olderInstant = newerInstant.minus(10, ChronoUnit.MINUTES);
    PowerMockito.doReturn(newerInstant).when(coreLocalBranchCommit).getCommitTime();
    PowerMockito.doReturn(olderInstant).when(coreRemoteBranchCommit).getCommitTime();

    // when
    SyncToRemoteStatus syncToRemoteStatus = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Relation.DivergedFromAndNewerThanRemote, syncToRemoteStatus.getRelation());
  }

  @Test
  public void deriveSyncToRemoteStatus_DivergedAndOlderThan() throws Exception {
    // given
    var gitCoreBranchTrackingStatus = GitCoreBranchTrackingStatus.of(1, 2);
    PowerMockito.doReturn(Option.of(gitCoreBranchTrackingStatus)).when(gitCoreRepository)
        .deriveRemoteTrackingStatus(coreLocalBranch);
    PowerMockito.doReturn(Option.of(coreRemoteBranch)).when(coreLocalBranch).getRemoteTrackingBranch();
    PowerMockito.doReturn(coreLocalBranchCommit).when(coreLocalBranch).derivePointedCommit();
    PowerMockito.doReturn(coreRemoteBranchCommit).when(coreRemoteBranch).derivePointedCommit();
    Instant olderInstant = Instant.parse("2000-05-01T10:00:00Z");
    Instant newerInstant = olderInstant.plus(10, ChronoUnit.MINUTES);
    PowerMockito.doReturn(olderInstant).when(coreLocalBranchCommit).getCommitTime();
    PowerMockito.doReturn(newerInstant).when(coreRemoteBranchCommit).getCommitTime();

    // when
    SyncToRemoteStatus syncToRemoteStatus = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Relation.DivergedFromAndOlderThanRemote, syncToRemoteStatus.getRelation());
  }

  @Test
  public void deriveSyncToRemoteStatus_DivergedAndNewerThan_theSameDates() throws Exception {
    // given
    var gitCoreBranchTrackingStatus = GitCoreBranchTrackingStatus.of(2, 1);
    PowerMockito.doReturn(Option.of(gitCoreBranchTrackingStatus)).when(gitCoreRepository)
        .deriveRemoteTrackingStatus(coreLocalBranch);
    PowerMockito.doReturn(Option.of(coreRemoteBranch)).when(coreLocalBranch).getRemoteTrackingBranch();
    PowerMockito.doReturn(coreLocalBranchCommit).when(coreLocalBranch).derivePointedCommit();
    PowerMockito.doReturn(coreRemoteBranchCommit).when(coreRemoteBranch).derivePointedCommit();
    Instant instant = Instant.parse("2000-05-01T10:00:00Z");
    PowerMockito.doReturn(instant).when(coreLocalBranchCommit).getCommitTime();
    PowerMockito.doReturn(instant).when(coreRemoteBranchCommit).getCommitTime();

    // when
    SyncToRemoteStatus syncToRemoteStatus = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Relation.DivergedFromAndNewerThanRemote, syncToRemoteStatus.getRelation());
  }

  @Test
  public void deriveSyncToRemoteStatus_Ahead() throws Exception {
    // given
    var gitCoreBranchTrackingStatus = GitCoreBranchTrackingStatus.of(3, 0);
    PowerMockito.doReturn(Option.of(gitCoreBranchTrackingStatus)).when(gitCoreRepository)
        .deriveRemoteTrackingStatus(coreLocalBranch);
    PowerMockito.doReturn(Option.of(coreRemoteBranch)).when(coreLocalBranch).getRemoteTrackingBranch();

    // when
    SyncToRemoteStatus syncToRemoteStatus = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Relation.AheadOfRemote, syncToRemoteStatus.getRelation());
  }

  @Test
  public void deriveSyncToRemoteStatus_Behind() throws Exception {
    // given
    var gitCoreBranchTrackingStatus = GitCoreBranchTrackingStatus.of(0, 3);
    PowerMockito.doReturn(Option.of(gitCoreBranchTrackingStatus)).when(gitCoreRepository)
        .deriveRemoteTrackingStatus(coreLocalBranch);
    PowerMockito.doReturn(Option.of(coreRemoteBranch)).when(coreLocalBranch).getRemoteTrackingBranch();

    // when
    SyncToRemoteStatus syncToRemoteStatus = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Relation.BehindRemote, syncToRemoteStatus.getRelation());
  }

  @Test
  public void deriveSyncToRemoteStatus_InSync() throws Exception {
    // given
    var gitCoreBranchTrackingStatus = GitCoreBranchTrackingStatus.of(0, 0);
    PowerMockito.doReturn(Option.of(gitCoreBranchTrackingStatus)).when(gitCoreRepository)
        .deriveRemoteTrackingStatus(coreLocalBranch);
    PowerMockito.doReturn(Option.of(coreRemoteBranch)).when(coreLocalBranch).getRemoteTrackingBranch();

    // when
    SyncToRemoteStatus syncToRemoteStatus = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Relation.InSyncToRemote, syncToRemoteStatus.getRelation());
  }
}
