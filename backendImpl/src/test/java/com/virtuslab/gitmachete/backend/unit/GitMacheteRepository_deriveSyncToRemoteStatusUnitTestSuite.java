package com.virtuslab.gitmachete.backend.unit;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.GitCoreRelativeCommitCount;
import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreLocalBranchSnapshot;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;

public class GitMacheteRepository_deriveSyncToRemoteStatusUnitTestSuite extends BaseGitMacheteRepositoryUnitTestSuite {

  private static final String ORIGIN = "origin";

  private final IGitCoreLocalBranchSnapshot coreLocalBranch = PowerMockito.mock(IGitCoreLocalBranchSnapshot.class);
  private final IGitCoreRemoteBranchSnapshot coreRemoteBranch = PowerMockito.mock(IGitCoreRemoteBranchSnapshot.class);
  private final IGitCoreCommit coreLocalBranchCommit = PowerMockito.mock(IGitCoreCommit.class);
  private final IGitCoreCommit coreRemoteBranchCommit = PowerMockito.mock(IGitCoreCommit.class);

  @SneakyThrows
  private SyncToRemoteStatus invokeDeriveSyncToRemoteStatus(IGitCoreLocalBranchSnapshot coreLocalBranch) {
    return Whitebox.invokeMethod(aux(), "deriveSyncToRemoteStatus", coreLocalBranch);
  }

  @Test
  @SneakyThrows
  public void deriveSyncToRemoteStatus_NoRemotes() {
    // given
    PowerMockito.doReturn(List.empty()).when(gitCoreRepository).deriveAllRemoteNames();

    // when
    SyncToRemoteStatus syncToRemoteStatus = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Relation.NoRemotes, syncToRemoteStatus.getRelation());
  }

  @Test
  @SneakyThrows
  public void deriveSyncToRemoteStatus_Untracked() {
    // given
    PowerMockito.doReturn(List.of(ORIGIN)).when(gitCoreRepository).deriveAllRemoteNames();

    PowerMockito.doReturn(Option.none()).when(coreLocalBranch).getRemoteTrackingBranch();

    // when
    SyncToRemoteStatus syncToRemoteStatus = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Relation.Untracked, syncToRemoteStatus.getRelation());
  }

  @Test
  @SneakyThrows
  public void deriveSyncToRemoteStatus_DivergedAndNewerThan() {
    // given
    PowerMockito.doReturn(List.of(ORIGIN)).when(gitCoreRepository).deriveAllRemoteNames();

    PowerMockito.doReturn(Option.some(coreRemoteBranch)).when(coreLocalBranch).getRemoteTrackingBranch();

    PowerMockito.doReturn(coreLocalBranchCommit).when(coreLocalBranch).getPointedCommit();
    PowerMockito.doReturn(coreRemoteBranchCommit).when(coreRemoteBranch).getPointedCommit();
    var relativeCommitCount = GitCoreRelativeCommitCount.of(1, 1);
    PowerMockito.doReturn(Option.some(relativeCommitCount)).when(gitCoreRepository)
        .deriveRelativeCommitCount(coreLocalBranchCommit, coreRemoteBranchCommit);

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
  @SneakyThrows
  public void deriveSyncToRemoteStatus_DivergedAndOlderThan() {
    // given
    PowerMockito.doReturn(List.of(ORIGIN)).when(gitCoreRepository).deriveAllRemoteNames();

    PowerMockito.doReturn(Option.some(coreRemoteBranch)).when(coreLocalBranch).getRemoteTrackingBranch();

    PowerMockito.doReturn(coreLocalBranchCommit).when(coreLocalBranch).getPointedCommit();
    PowerMockito.doReturn(coreRemoteBranchCommit).when(coreRemoteBranch).getPointedCommit();
    var relativeCommitCount = GitCoreRelativeCommitCount.of(1, 2);
    PowerMockito.doReturn(Option.some(relativeCommitCount)).when(gitCoreRepository)
        .deriveRelativeCommitCount(coreLocalBranchCommit, coreRemoteBranchCommit);

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
  @SneakyThrows
  public void deriveSyncToRemoteStatus_DivergedAndNewerThan_theSameDates() {
    // given
    PowerMockito.doReturn(List.of(ORIGIN)).when(gitCoreRepository).deriveAllRemoteNames();

    PowerMockito.doReturn(Option.some(coreRemoteBranch)).when(coreLocalBranch).getRemoteTrackingBranch();

    PowerMockito.doReturn(coreLocalBranchCommit).when(coreLocalBranch).getPointedCommit();
    PowerMockito.doReturn(coreRemoteBranchCommit).when(coreRemoteBranch).getPointedCommit();
    var relativeCommitCount = GitCoreRelativeCommitCount.of(2, 1);
    PowerMockito.doReturn(Option.some(relativeCommitCount)).when(gitCoreRepository)
        .deriveRelativeCommitCount(coreLocalBranchCommit, coreRemoteBranchCommit);

    Instant instant = Instant.parse("2000-05-01T10:00:00Z");
    PowerMockito.doReturn(instant).when(coreLocalBranchCommit).getCommitTime();
    PowerMockito.doReturn(instant).when(coreRemoteBranchCommit).getCommitTime();

    // when
    SyncToRemoteStatus syncToRemoteStatus = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Relation.DivergedFromAndNewerThanRemote, syncToRemoteStatus.getRelation());
  }

  @Test
  @SneakyThrows
  public void deriveSyncToRemoteStatus_Ahead() {
    // given
    PowerMockito.doReturn(List.of(ORIGIN)).when(gitCoreRepository).deriveAllRemoteNames();

    PowerMockito.doReturn(Option.some(coreRemoteBranch)).when(coreLocalBranch).getRemoteTrackingBranch();

    PowerMockito.doReturn(coreLocalBranchCommit).when(coreLocalBranch).getPointedCommit();
    PowerMockito.doReturn(coreRemoteBranchCommit).when(coreRemoteBranch).getPointedCommit();
    var relativeCommitCount = GitCoreRelativeCommitCount.of(3, 0);
    PowerMockito.doReturn(Option.some(relativeCommitCount)).when(gitCoreRepository)
        .deriveRelativeCommitCount(coreLocalBranchCommit, coreRemoteBranchCommit);

    // when
    SyncToRemoteStatus syncToRemoteStatus = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Relation.AheadOfRemote, syncToRemoteStatus.getRelation());
  }

  @Test
  @SneakyThrows
  public void deriveSyncToRemoteStatus_Behind() {
    // given
    PowerMockito.doReturn(List.of(ORIGIN)).when(gitCoreRepository).deriveAllRemoteNames();

    PowerMockito.doReturn(Option.some(coreRemoteBranch)).when(coreLocalBranch).getRemoteTrackingBranch();

    PowerMockito.doReturn(coreLocalBranchCommit).when(coreLocalBranch).getPointedCommit();
    PowerMockito.doReturn(coreRemoteBranchCommit).when(coreRemoteBranch).getPointedCommit();
    var relativeCommitCount = GitCoreRelativeCommitCount.of(0, 3);
    PowerMockito.doReturn(Option.some(relativeCommitCount)).when(gitCoreRepository)
        .deriveRelativeCommitCount(coreLocalBranchCommit, coreRemoteBranchCommit);

    // when
    SyncToRemoteStatus syncToRemoteStatus = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Relation.BehindRemote, syncToRemoteStatus.getRelation());
  }

  @Test
  @SneakyThrows
  public void deriveSyncToRemoteStatus_InSync() {
    // given
    PowerMockito.doReturn(List.of(ORIGIN)).when(gitCoreRepository).deriveAllRemoteNames();

    PowerMockito.doReturn(Option.some(coreRemoteBranch)).when(coreLocalBranch).getRemoteTrackingBranch();

    PowerMockito.doReturn(coreLocalBranchCommit).when(coreLocalBranch).getPointedCommit();
    PowerMockito.doReturn(coreRemoteBranchCommit).when(coreRemoteBranch).getPointedCommit();
    var relativeCommitCount = GitCoreRelativeCommitCount.of(0, 0);
    PowerMockito.doReturn(Option.some(relativeCommitCount)).when(gitCoreRepository)
        .deriveRelativeCommitCount(coreLocalBranchCommit, coreRemoteBranchCommit);

    // when
    SyncToRemoteStatus syncToRemoteStatus = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Relation.InSyncToRemote, syncToRemoteStatus.getRelation());
  }
}
