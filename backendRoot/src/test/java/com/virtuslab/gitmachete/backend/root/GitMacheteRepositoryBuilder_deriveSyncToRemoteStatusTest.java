package com.virtuslab.gitmachete.backend.root;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.IGitCoreBranchTrackingStatus;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.impl.jgit.GitCoreBranchTrackingStatus;
import com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus;

public class GitMacheteRepositoryBuilder_deriveSyncToRemoteStatusTest {

  private final IGitCoreLocalBranch coreLocalBranch = PowerMockito.mock(IGitCoreLocalBranch.class);

  private ISyncToRemoteStatus invokeDeriveSyncToRemoteStatus(IGitCoreLocalBranch coreLocalBranch) throws Exception {
    return Whitebox.invokeMethod(PowerMockito.mock(GitMacheteRepositoryBuilder.class),
        "deriveSyncToRemoteStatus",
        coreLocalBranch);
  }

  @Test
  public void deriveSyncToRemoteStatus_Untracked() throws Exception {
    // given
    PowerMockito.doReturn(Optional.empty()).when(coreLocalBranch).deriveRemoteTrackingStatus();

    // when
    ISyncToRemoteStatus status = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(ISyncToRemoteStatus.Relation.Untracked, status.getRelation());
  }

  @Test
  public void deriveSyncToRemoteStatus_Diverged() throws Exception {
    // given
    PowerMockito.doReturn(getTrackingStatusOptional(1, 1, "origin")).when(coreLocalBranch).deriveRemoteTrackingStatus();

    // when
    ISyncToRemoteStatus status = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(ISyncToRemoteStatus.Relation.Diverged, status.getRelation());
  }

  @Test
  public void deriveSyncToRemoteStatus_Ahead() throws Exception {
    // given
    PowerMockito.doReturn(getTrackingStatusOptional(1, 0, "origin")).when(coreLocalBranch).deriveRemoteTrackingStatus();

    // when
    ISyncToRemoteStatus status = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(ISyncToRemoteStatus.Relation.Ahead, status.getRelation());
  }

  @Test
  public void deriveSyncToRemoteStatus_Behind() throws Exception {
    // given
    PowerMockito.doReturn(getTrackingStatusOptional(0, 1, "origin")).when(coreLocalBranch).deriveRemoteTrackingStatus();

    // when
    ISyncToRemoteStatus status = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(ISyncToRemoteStatus.Relation.Behind, status.getRelation());
  }

  @Test
  public void deriveSyncToRemoteStatus_InSync() throws Exception {
    // given
    PowerMockito.doReturn(getTrackingStatusOptional(0, 0, "origin")).when(coreLocalBranch).deriveRemoteTrackingStatus();

    // when
    ISyncToRemoteStatus status = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(ISyncToRemoteStatus.Relation.InSync, status.getRelation());
  }

  private Optional<IGitCoreBranchTrackingStatus> getTrackingStatusOptional(int ahead, int behind, String remoteName) {
    return Optional.of(GitCoreBranchTrackingStatus.of(ahead, behind, remoteName));
  }
}
