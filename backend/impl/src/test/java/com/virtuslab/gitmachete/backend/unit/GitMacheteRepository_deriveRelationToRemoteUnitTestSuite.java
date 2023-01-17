package com.virtuslab.gitmachete.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import io.vavr.collection.List;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.GitCoreRelativeCommitCount;
import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreLocalBranchSnapshot;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.RelationToRemote;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;

public class GitMacheteRepository_deriveRelationToRemoteUnitTestSuite extends BaseGitMacheteRepositoryUnitTestSuite {

  private static final String ORIGIN = "origin";

  private final IGitCoreLocalBranchSnapshot coreLocalBranch = PowerMockito.mock(IGitCoreLocalBranchSnapshot.class);
  private final IGitCoreRemoteBranchSnapshot coreRemoteBranch = PowerMockito.mock(IGitCoreRemoteBranchSnapshot.class);
  private final IGitCoreCommit coreLocalBranchCommit = PowerMockito.mock(IGitCoreCommit.class);
  private final IGitCoreCommit coreRemoteBranchCommit = PowerMockito.mock(IGitCoreCommit.class);

  @SneakyThrows
  private RelationToRemote invokeDeriveRelationToRemote(IGitCoreLocalBranchSnapshot coreLocalBranch) {
    return Whitebox.invokeMethod(aux(), "deriveRelationToRemote", coreLocalBranch);
  }

  @Test
  @SneakyThrows
  public void deriveRelationToRemote_NoRemotes() {
    // given
    PowerMockito.doReturn(List.empty()).when(gitCoreRepository).deriveAllRemoteNames();

    // when
    RelationToRemote relationToRemote = invokeDeriveRelationToRemote(coreLocalBranch);

    // then
    assertEquals(SyncToRemoteStatus.NoRemotes, relationToRemote.getSyncToRemoteStatus());
  }

  @Test
  @SneakyThrows
  public void deriveRelationToRemote_Untracked() {
    // given
    PowerMockito.doReturn(List.of(ORIGIN)).when(gitCoreRepository).deriveAllRemoteNames();

    PowerMockito.doReturn(null).when(coreLocalBranch).getRemoteTrackingBranch();

    // when
    RelationToRemote relationToRemote = invokeDeriveRelationToRemote(coreLocalBranch);

    // then
    assertEquals(SyncToRemoteStatus.Untracked, relationToRemote.getSyncToRemoteStatus());
  }

  @Test
  @SneakyThrows
  public void deriveRelationToRemote_DivergedAndNewerThan() {
    // given
    PowerMockito.doReturn(List.of(ORIGIN)).when(gitCoreRepository).deriveAllRemoteNames();

    PowerMockito.doReturn(coreRemoteBranch).when(coreLocalBranch).getRemoteTrackingBranch();

    PowerMockito.doReturn(coreLocalBranchCommit).when(coreLocalBranch).getPointedCommit();
    PowerMockito.doReturn(coreRemoteBranchCommit).when(coreRemoteBranch).getPointedCommit();
    val relativeCommitCount = GitCoreRelativeCommitCount.of(1, 1);
    PowerMockito.doReturn(relativeCommitCount).when(gitCoreRepository)
        .deriveRelativeCommitCount(coreLocalBranchCommit, coreRemoteBranchCommit);

    Instant newerInstant = Instant.parse("2000-05-01T10:00:00Z");
    Instant olderInstant = newerInstant.minus(10, ChronoUnit.MINUTES);
    PowerMockito.doReturn(newerInstant).when(coreLocalBranchCommit).getCommitTime();
    PowerMockito.doReturn(olderInstant).when(coreRemoteBranchCommit).getCommitTime();

    // when
    RelationToRemote relationToRemote = invokeDeriveRelationToRemote(coreLocalBranch);

    // then
    assertEquals(SyncToRemoteStatus.DivergedFromAndNewerThanRemote, relationToRemote.getSyncToRemoteStatus());
  }

  @Test
  @SneakyThrows
  public void deriveRelationToRemote_DivergedAndOlderThan() {
    // given
    PowerMockito.doReturn(List.of(ORIGIN)).when(gitCoreRepository).deriveAllRemoteNames();

    PowerMockito.doReturn(coreRemoteBranch).when(coreLocalBranch).getRemoteTrackingBranch();

    PowerMockito.doReturn(coreLocalBranchCommit).when(coreLocalBranch).getPointedCommit();
    PowerMockito.doReturn(coreRemoteBranchCommit).when(coreRemoteBranch).getPointedCommit();
    val relativeCommitCount = GitCoreRelativeCommitCount.of(1, 2);
    PowerMockito.doReturn(relativeCommitCount).when(gitCoreRepository)
        .deriveRelativeCommitCount(coreLocalBranchCommit, coreRemoteBranchCommit);

    Instant olderInstant = Instant.parse("2000-05-01T10:00:00Z");
    Instant newerInstant = olderInstant.plus(10, ChronoUnit.MINUTES);
    PowerMockito.doReturn(olderInstant).when(coreLocalBranchCommit).getCommitTime();
    PowerMockito.doReturn(newerInstant).when(coreRemoteBranchCommit).getCommitTime();

    // when
    RelationToRemote relationToRemote = invokeDeriveRelationToRemote(coreLocalBranch);

    // then
    assertEquals(SyncToRemoteStatus.DivergedFromAndOlderThanRemote, relationToRemote.getSyncToRemoteStatus());
  }

  @Test
  @SneakyThrows
  public void deriveRelationToRemote_DivergedAndNewerThan_theSameDates() {
    // given
    PowerMockito.doReturn(List.of(ORIGIN)).when(gitCoreRepository).deriveAllRemoteNames();

    PowerMockito.doReturn(coreRemoteBranch).when(coreLocalBranch).getRemoteTrackingBranch();

    PowerMockito.doReturn(coreLocalBranchCommit).when(coreLocalBranch).getPointedCommit();
    PowerMockito.doReturn(coreRemoteBranchCommit).when(coreRemoteBranch).getPointedCommit();
    val relativeCommitCount = GitCoreRelativeCommitCount.of(2, 1);
    PowerMockito.doReturn(relativeCommitCount).when(gitCoreRepository)
        .deriveRelativeCommitCount(coreLocalBranchCommit, coreRemoteBranchCommit);

    Instant instant = Instant.parse("2000-05-01T10:00:00Z");
    PowerMockito.doReturn(instant).when(coreLocalBranchCommit).getCommitTime();
    PowerMockito.doReturn(instant).when(coreRemoteBranchCommit).getCommitTime();

    // when
    RelationToRemote relationToRemote = invokeDeriveRelationToRemote(coreLocalBranch);

    // then
    assertEquals(SyncToRemoteStatus.DivergedFromAndNewerThanRemote, relationToRemote.getSyncToRemoteStatus());
  }

  @Test
  @SneakyThrows
  public void deriveRelationToRemote_Ahead() {
    // given
    PowerMockito.doReturn(List.of(ORIGIN)).when(gitCoreRepository).deriveAllRemoteNames();

    PowerMockito.doReturn(coreRemoteBranch).when(coreLocalBranch).getRemoteTrackingBranch();

    PowerMockito.doReturn(coreLocalBranchCommit).when(coreLocalBranch).getPointedCommit();
    PowerMockito.doReturn(coreRemoteBranchCommit).when(coreRemoteBranch).getPointedCommit();
    val relativeCommitCount = GitCoreRelativeCommitCount.of(3, 0);
    PowerMockito.doReturn(relativeCommitCount).when(gitCoreRepository)
        .deriveRelativeCommitCount(coreLocalBranchCommit, coreRemoteBranchCommit);

    // when
    RelationToRemote relationToRemote = invokeDeriveRelationToRemote(coreLocalBranch);

    // then
    assertEquals(SyncToRemoteStatus.AheadOfRemote, relationToRemote.getSyncToRemoteStatus());
  }

  @Test
  @SneakyThrows
  public void deriveRelationToRemote_Behind() {
    // given
    PowerMockito.doReturn(List.of(ORIGIN)).when(gitCoreRepository).deriveAllRemoteNames();

    PowerMockito.doReturn(coreRemoteBranch).when(coreLocalBranch).getRemoteTrackingBranch();

    PowerMockito.doReturn(coreLocalBranchCommit).when(coreLocalBranch).getPointedCommit();
    PowerMockito.doReturn(coreRemoteBranchCommit).when(coreRemoteBranch).getPointedCommit();
    val relativeCommitCount = GitCoreRelativeCommitCount.of(0, 3);
    PowerMockito.doReturn(relativeCommitCount).when(gitCoreRepository)
        .deriveRelativeCommitCount(coreLocalBranchCommit, coreRemoteBranchCommit);

    // when
    RelationToRemote relationToRemote = invokeDeriveRelationToRemote(coreLocalBranch);

    // then
    assertEquals(SyncToRemoteStatus.BehindRemote, relationToRemote.getSyncToRemoteStatus());
  }

  @Test
  @SneakyThrows
  public void deriveRelationToRemote_InSync() {
    // given
    PowerMockito.doReturn(List.of(ORIGIN)).when(gitCoreRepository).deriveAllRemoteNames();

    PowerMockito.doReturn(coreRemoteBranch).when(coreLocalBranch).getRemoteTrackingBranch();

    PowerMockito.doReturn(coreLocalBranchCommit).when(coreLocalBranch).getPointedCommit();
    PowerMockito.doReturn(coreRemoteBranchCommit).when(coreRemoteBranch).getPointedCommit();
    val relativeCommitCount = GitCoreRelativeCommitCount.of(0, 0);
    PowerMockito.doReturn(relativeCommitCount).when(gitCoreRepository)
        .deriveRelativeCommitCount(coreLocalBranchCommit, coreRemoteBranchCommit);

    // when
    RelationToRemote relationToRemote = invokeDeriveRelationToRemote(coreLocalBranch);

    // then
    assertEquals(SyncToRemoteStatus.InSyncToRemote, relationToRemote.getSyncToRemoteStatus());
  }
}
