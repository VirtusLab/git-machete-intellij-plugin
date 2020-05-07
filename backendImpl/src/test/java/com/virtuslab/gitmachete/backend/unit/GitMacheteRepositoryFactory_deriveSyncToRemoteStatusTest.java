package com.virtuslab.gitmachete.backend.unit;

import java.time.Instant;

import io.vavr.control.Option;
import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.BaseGitCoreCommit;
import com.virtuslab.gitcore.api.GitCoreBranchTrackingStatus;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranch;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositoryFactory;

public class GitMacheteRepositoryFactory_deriveSyncToRemoteStatusTest {

  private final IGitCoreLocalBranch coreLocalBranch = PowerMockito.mock(IGitCoreLocalBranch.class);
  private final IGitCoreRemoteBranch coreRemoteBranch = PowerMockito.mock(IGitCoreRemoteBranch.class);
  private final BaseGitCoreCommit coreLocalBranchCommit = PowerMockito.mock(BaseGitCoreCommit.class);
  private final BaseGitCoreCommit coreRemoteBranchCommit = PowerMockito.mock(BaseGitCoreCommit.class);

  private SyncToRemoteStatus invokeDeriveSyncToRemoteStatus(IGitCoreLocalBranch coreLocalBranch) throws Exception {
    return Whitebox.invokeMethod(PowerMockito.mock(GitMacheteRepositoryFactory.class),
        "deriveSyncToRemoteStatus",
        coreLocalBranch);
  }

  @Test
  public void deriveSyncToRemoteStatus_Untracked() throws Exception {
    // given
    PowerMockito.doReturn(Option.none()).when(coreLocalBranch).deriveRemoteTrackingStatus();
    PowerMockito.doReturn(Option.none()).when(coreLocalBranch).getRemoteTrackingBranch();

    // when
    SyncToRemoteStatus status = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Relation.Untracked, status.getRelation());
  }

  @Test
  public void deriveSyncToRemoteStatus_DivergedAndNewerThan() throws Exception {
    // given
    PowerMockito.doReturn(getTrackingStatusOption(1, 1, "origin")).when(coreLocalBranch).deriveRemoteTrackingStatus();
    PowerMockito.doReturn(Option.of(coreRemoteBranch)).when(coreLocalBranch).getRemoteTrackingBranch();
    PowerMockito.doReturn(coreLocalBranchCommit).when(coreLocalBranch).getPointedCommit();
    PowerMockito.doReturn(coreRemoteBranchCommit).when(coreRemoteBranch).getPointedCommit();
    PowerMockito.doReturn(Instant.ofEpochSecond(1588761544)).when(coreLocalBranchCommit).getCommitTime();
    PowerMockito.doReturn(Instant.ofEpochSecond(1588761500)).when(coreRemoteBranchCommit).getCommitTime();

    // when
    SyncToRemoteStatus status = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Relation.DivergedAndNewerThanRemote, status.getRelation());
  }

  @Test
  public void deriveSyncToRemoteStatus_DivergedAndOlderThan() throws Exception {
    // given
    PowerMockito.doReturn(getTrackingStatusOption(1, 1, "origin")).when(coreLocalBranch).deriveRemoteTrackingStatus();
    PowerMockito.doReturn(Option.of(coreRemoteBranch)).when(coreLocalBranch).getRemoteTrackingBranch();
    PowerMockito.doReturn(coreLocalBranchCommit).when(coreLocalBranch).getPointedCommit();
    PowerMockito.doReturn(coreRemoteBranchCommit).when(coreRemoteBranch).getPointedCommit();
    PowerMockito.doReturn(Instant.ofEpochSecond(1588761544)).when(coreLocalBranchCommit).getCommitTime();
    PowerMockito.doReturn(Instant.ofEpochSecond(1588761678)).when(coreRemoteBranchCommit).getCommitTime();

    // when
    SyncToRemoteStatus status = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Relation.DivergedAndOlderThanRemote, status.getRelation());
  }

  @Test
  public void deriveSyncToRemoteStatus_Ahead() throws Exception {
    // given
    PowerMockito.doReturn(getTrackingStatusOption(1, 0, "origin")).when(coreLocalBranch).deriveRemoteTrackingStatus();
    PowerMockito.doReturn(Option.of(coreRemoteBranch)).when(coreLocalBranch).getRemoteTrackingBranch();

    // when
    SyncToRemoteStatus status = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Relation.Ahead, status.getRelation());
  }

  @Test
  public void deriveSyncToRemoteStatus_Behind() throws Exception {
    // given
    PowerMockito.doReturn(getTrackingStatusOption(0, 1, "origin")).when(coreLocalBranch).deriveRemoteTrackingStatus();
    PowerMockito.doReturn(Option.of(coreRemoteBranch)).when(coreLocalBranch).getRemoteTrackingBranch();

    // when
    SyncToRemoteStatus status = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Relation.Behind, status.getRelation());
  }

  @Test
  public void deriveSyncToRemoteStatus_InSync() throws Exception {
    // given
    PowerMockito.doReturn(getTrackingStatusOption(0, 0, "origin")).when(coreLocalBranch).deriveRemoteTrackingStatus();
    PowerMockito.doReturn(Option.of(coreRemoteBranch)).when(coreLocalBranch).getRemoteTrackingBranch();

    // when
    SyncToRemoteStatus status = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Relation.InSync, status.getRelation());
  }

  private Option<GitCoreBranchTrackingStatus> getTrackingStatusOption(int ahead, int behind, String remoteName) {
    return Option.of(GitCoreBranchTrackingStatus.of(ahead, behind, remoteName));
  }
}
