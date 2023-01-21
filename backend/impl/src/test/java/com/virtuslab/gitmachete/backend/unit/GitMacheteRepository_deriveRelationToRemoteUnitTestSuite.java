package com.virtuslab.gitmachete.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import io.vavr.collection.List;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;

import com.virtuslab.gitcore.api.GitCoreRelativeCommitCount;
import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreLocalBranchSnapshot;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.RelationToRemote;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;

public class GitMacheteRepository_deriveRelationToRemoteUnitTestSuite extends BaseGitMacheteRepositoryUnitTestSuite {

  private static final String ORIGIN = "origin";

  private final IGitCoreLocalBranchSnapshot coreLocalBranch = mock(IGitCoreLocalBranchSnapshot.class);
  private final IGitCoreRemoteBranchSnapshot coreRemoteBranch = mock(IGitCoreRemoteBranchSnapshot.class);
  private final IGitCoreCommit coreLocalBranchCommit = mock(IGitCoreCommit.class);
  private final IGitCoreCommit coreRemoteBranchCommit = mock(IGitCoreCommit.class);

  @SneakyThrows
  private RelationToRemote invokeDeriveRelationToRemote(IGitCoreLocalBranchSnapshot coreLocalBranch) {
    return aux().deriveRelationToRemote(coreLocalBranch);
  }

  @Test
  @SneakyThrows
  public void deriveRelationToRemote_NoRemotes() {
    // given
    when(gitCoreRepository.deriveAllRemoteNames()).thenReturn(List.empty());

    // when
    RelationToRemote relationToRemote = invokeDeriveRelationToRemote(coreLocalBranch);

    // then
    assertEquals(SyncToRemoteStatus.NoRemotes, relationToRemote.getSyncToRemoteStatus());
  }

  @Test
  @SneakyThrows
  public void deriveRelationToRemote_Untracked() {
    // given
    when(gitCoreRepository.deriveAllRemoteNames()).thenReturn(List.of(ORIGIN));

    when(coreLocalBranch.getRemoteTrackingBranch()).thenReturn(null);

    // when
    RelationToRemote relationToRemote = invokeDeriveRelationToRemote(coreLocalBranch);

    // then
    assertEquals(SyncToRemoteStatus.Untracked, relationToRemote.getSyncToRemoteStatus());
  }

  @Test
  @SneakyThrows
  public void deriveRelationToRemote_DivergedAndNewerThan() {
    // given
    when(gitCoreRepository.deriveAllRemoteNames()).thenReturn(List.of(ORIGIN));

    when(coreLocalBranch.getRemoteTrackingBranch()).thenReturn(coreRemoteBranch);

    when(coreLocalBranch.getPointedCommit()).thenReturn(coreLocalBranchCommit);
    when(coreRemoteBranch.getPointedCommit()).thenReturn(coreRemoteBranchCommit);
    val relativeCommitCount = GitCoreRelativeCommitCount.of(1, 1);
    when(gitCoreRepository
        .deriveRelativeCommitCount(coreLocalBranchCommit, coreRemoteBranchCommit)).thenReturn(relativeCommitCount);

    Instant newerInstant = Instant.parse("2000-05-01T10:00:00Z");
    Instant olderInstant = newerInstant.minus(10, ChronoUnit.MINUTES);
    when(coreLocalBranchCommit.getCommitTime()).thenReturn(newerInstant);
    when(coreRemoteBranchCommit.getCommitTime()).thenReturn(olderInstant);

    // when
    RelationToRemote relationToRemote = invokeDeriveRelationToRemote(coreLocalBranch);

    // then
    assertEquals(SyncToRemoteStatus.DivergedFromAndNewerThanRemote, relationToRemote.getSyncToRemoteStatus());
  }

  @Test
  @SneakyThrows
  public void deriveRelationToRemote_DivergedAndOlderThan() {
    // given
    when(gitCoreRepository.deriveAllRemoteNames()).thenReturn(List.of(ORIGIN));

    when(coreLocalBranch.getRemoteTrackingBranch()).thenReturn(coreRemoteBranch);

    when(coreLocalBranch.getPointedCommit()).thenReturn(coreLocalBranchCommit);
    when(coreRemoteBranch.getPointedCommit()).thenReturn(coreRemoteBranchCommit);
    val relativeCommitCount = GitCoreRelativeCommitCount.of(1, 2);
    when(gitCoreRepository
        .deriveRelativeCommitCount(coreLocalBranchCommit, coreRemoteBranchCommit)).thenReturn(relativeCommitCount);

    Instant olderInstant = Instant.parse("2000-05-01T10:00:00Z");
    Instant newerInstant = olderInstant.plus(10, ChronoUnit.MINUTES);
    when(coreLocalBranchCommit.getCommitTime()).thenReturn(olderInstant);
    when(coreRemoteBranchCommit.getCommitTime()).thenReturn(newerInstant);

    // when
    RelationToRemote relationToRemote = invokeDeriveRelationToRemote(coreLocalBranch);

    // then
    assertEquals(SyncToRemoteStatus.DivergedFromAndOlderThanRemote, relationToRemote.getSyncToRemoteStatus());
  }

  @Test
  @SneakyThrows
  public void deriveRelationToRemote_DivergedAndNewerThan_theSameDates() {
    // given
    when(gitCoreRepository.deriveAllRemoteNames()).thenReturn(List.of(ORIGIN));

    when(coreLocalBranch.getRemoteTrackingBranch()).thenReturn(coreRemoteBranch);

    when(coreLocalBranch.getPointedCommit()).thenReturn(coreLocalBranchCommit);
    when(coreRemoteBranch.getPointedCommit()).thenReturn(coreRemoteBranchCommit);
    val relativeCommitCount = GitCoreRelativeCommitCount.of(2, 1);
    when(gitCoreRepository
        .deriveRelativeCommitCount(coreLocalBranchCommit, coreRemoteBranchCommit)).thenReturn(relativeCommitCount);

    Instant instant = Instant.parse("2000-05-01T10:00:00Z");
    when(coreLocalBranchCommit.getCommitTime()).thenReturn(instant);
    when(coreRemoteBranchCommit.getCommitTime()).thenReturn(instant);

    // when
    RelationToRemote relationToRemote = invokeDeriveRelationToRemote(coreLocalBranch);

    // then
    assertEquals(SyncToRemoteStatus.DivergedFromAndNewerThanRemote, relationToRemote.getSyncToRemoteStatus());
  }

  @Test
  @SneakyThrows
  public void deriveRelationToRemote_Ahead() {
    // given
    when(gitCoreRepository.deriveAllRemoteNames()).thenReturn(List.of(ORIGIN));

    when(coreLocalBranch.getRemoteTrackingBranch()).thenReturn(coreRemoteBranch);

    when(coreLocalBranch.getPointedCommit()).thenReturn(coreLocalBranchCommit);
    when(coreRemoteBranch.getPointedCommit()).thenReturn(coreRemoteBranchCommit);
    val relativeCommitCount = GitCoreRelativeCommitCount.of(3, 0);
    when(gitCoreRepository
        .deriveRelativeCommitCount(coreLocalBranchCommit, coreRemoteBranchCommit)).thenReturn(relativeCommitCount);

    // when
    RelationToRemote relationToRemote = invokeDeriveRelationToRemote(coreLocalBranch);

    // then
    assertEquals(SyncToRemoteStatus.AheadOfRemote, relationToRemote.getSyncToRemoteStatus());
  }

  @Test
  @SneakyThrows
  public void deriveRelationToRemote_Behind() {
    // given
    when(gitCoreRepository.deriveAllRemoteNames()).thenReturn(List.of(ORIGIN));

    when(coreLocalBranch.getRemoteTrackingBranch()).thenReturn(coreRemoteBranch);

    when(coreLocalBranch.getPointedCommit()).thenReturn(coreLocalBranchCommit);
    when(coreRemoteBranch.getPointedCommit()).thenReturn(coreRemoteBranchCommit);
    val relativeCommitCount = GitCoreRelativeCommitCount.of(0, 3);
    when(gitCoreRepository
        .deriveRelativeCommitCount(coreLocalBranchCommit, coreRemoteBranchCommit)).thenReturn(relativeCommitCount);

    // when
    RelationToRemote relationToRemote = invokeDeriveRelationToRemote(coreLocalBranch);

    // then
    assertEquals(SyncToRemoteStatus.BehindRemote, relationToRemote.getSyncToRemoteStatus());
  }

  @Test
  @SneakyThrows
  public void deriveRelationToRemote_InSync() {
    // given
    when(gitCoreRepository.deriveAllRemoteNames()).thenReturn(List.of(ORIGIN));

    when(coreLocalBranch.getRemoteTrackingBranch()).thenReturn(coreRemoteBranch);

    when(coreLocalBranch.getPointedCommit()).thenReturn(coreLocalBranchCommit);
    when(coreRemoteBranch.getPointedCommit()).thenReturn(coreRemoteBranchCommit);
    val relativeCommitCount = GitCoreRelativeCommitCount.of(0, 0);
    when(gitCoreRepository
        .deriveRelativeCommitCount(coreLocalBranchCommit, coreRemoteBranchCommit)).thenReturn(relativeCommitCount);

    // when
    RelationToRemote relationToRemote = invokeDeriveRelationToRemote(coreLocalBranch);

    // then
    assertEquals(SyncToRemoteStatus.InSyncToRemote, relationToRemote.getSyncToRemoteStatus());
  }
}
