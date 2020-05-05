package com.virtuslab.gitmachete.backend.unit;

import io.vavr.control.Option;
import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.GitCoreBranchTrackingStatus;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositoryFactory;

public class GitMacheteRepositoryFactory_deriveSyncToRemoteStatusTest {

  private final IGitCoreLocalBranch coreLocalBranch = PowerMockito.mock(IGitCoreLocalBranch.class);

  private SyncToRemoteStatus invokeDeriveSyncToRemoteStatus(IGitCoreLocalBranch coreLocalBranch) throws Exception {
    return Whitebox.invokeMethod(PowerMockito.mock(GitMacheteRepositoryFactory.class),
        "deriveSyncToRemoteStatus",
        coreLocalBranch);
  }

  @Test
  public void deriveSyncToRemoteStatus_Untracked() throws Exception {
    // given
    PowerMockito.doReturn(Option.none()).when(coreLocalBranch).deriveRemoteTrackingStatus();

    // when
    SyncToRemoteStatus status = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Relation.Untracked, status.getRelation());
  }

  @Test
  public void deriveSyncToRemoteStatus_Diverged() throws Exception {
    // given
    PowerMockito.doReturn(getTrackingStatusOption(1, 1, "origin")).when(coreLocalBranch).deriveRemoteTrackingStatus();

    // when
    SyncToRemoteStatus status = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Relation.DivergedAndNewerThanRemote, status.getRelation());
  }

  @Test
  public void deriveSyncToRemoteStatus_Ahead() throws Exception {
    // given
    PowerMockito.doReturn(getTrackingStatusOption(1, 0, "origin")).when(coreLocalBranch).deriveRemoteTrackingStatus();

    // when
    SyncToRemoteStatus status = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Relation.Ahead, status.getRelation());
  }

  @Test
  public void deriveSyncToRemoteStatus_Behind() throws Exception {
    // given
    PowerMockito.doReturn(getTrackingStatusOption(0, 1, "origin")).when(coreLocalBranch).deriveRemoteTrackingStatus();

    // when
    SyncToRemoteStatus status = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Relation.Behind, status.getRelation());
  }

  @Test
  public void deriveSyncToRemoteStatus_InSync() throws Exception {
    // given
    PowerMockito.doReturn(getTrackingStatusOption(0, 0, "origin")).when(coreLocalBranch).deriveRemoteTrackingStatus();

    // when
    SyncToRemoteStatus status = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Relation.InSync, status.getRelation());
  }

  private Option<GitCoreBranchTrackingStatus> getTrackingStatusOption(int ahead, int behind, String remoteName) {
    return Option.of(GitCoreBranchTrackingStatus.of(ahead, behind, remoteName));
  }
}
