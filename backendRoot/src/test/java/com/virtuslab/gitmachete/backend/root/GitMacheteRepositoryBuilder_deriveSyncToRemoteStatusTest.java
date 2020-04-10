package com.virtuslab.gitmachete.backend.root;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.IGitCoreBranchTrackingStatus;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;

public class GitMacheteRepositoryBuilder_deriveSyncToRemoteStatusTest {

  private final IGitCoreLocalBranch coreLocalBranch = PowerMockito.mock(IGitCoreLocalBranch.class);

  private SyncToRemoteStatus invokeDeriveSyncToRemoteStatus(IGitCoreLocalBranch coreLocalBranch) throws Exception {
    return Whitebox.invokeMethod(PowerMockito.mock(GitMacheteRepositoryBuilder.class),
        "deriveSyncToRemoteStatus",
        coreLocalBranch);
  }

  @Test
  public void deriveSyncToRemoteStatus_Untracked() throws Exception {
    // given
    PowerMockito.doReturn(Optional.empty()).when(coreLocalBranch).deriveRemoteTrackingStatus();

    // when
    SyncToRemoteStatus status = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Status.Untracked, status.getStatus());
  }

  @Test
  public void deriveSyncToRemoteStatus_Diverged() throws Exception {
    // given
    PowerMockito.doReturn(getTrackingStatusOptional(1, 1, "origin")).when(coreLocalBranch).deriveRemoteTrackingStatus();

    // when
    SyncToRemoteStatus status = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Status.Diverged, status.getStatus());
  }

  @Test
  public void deriveSyncToRemoteStatus_Ahead() throws Exception {
    // given
    PowerMockito.doReturn(getTrackingStatusOptional(1, 0, "origin")).when(coreLocalBranch).deriveRemoteTrackingStatus();

    // when
    SyncToRemoteStatus status = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Status.Ahead, status.getStatus());
  }

  @Test
  public void deriveSyncToRemoteStatus_Behind() throws Exception {
    // given
    PowerMockito.doReturn(getTrackingStatusOptional(0, 1, "origin")).when(coreLocalBranch).deriveRemoteTrackingStatus();

    // when
    SyncToRemoteStatus status = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Status.Behind, status.getStatus());
  }

  @Test
  public void deriveSyncToRemoteStatus_InSync() throws Exception {
    // given
    PowerMockito.doReturn(getTrackingStatusOptional(0, 0, "origin")).when(coreLocalBranch).deriveRemoteTrackingStatus();

    // when
    SyncToRemoteStatus status = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToRemoteStatus.Status.InSync, status.getStatus());
  }

  private Optional<IGitCoreBranchTrackingStatus> getTrackingStatusOptional(int ahead, int behind, String remoteName) {
    return Optional.of(new IGitCoreBranchTrackingStatus() {
      @Override
      public int getAhead() {
        return ahead;
      }

      @Override
      public int getBehind() {
        return behind;
      }

      @Override
      public String getRemoteName() {
        return remoteName;
      }
    });
  }
}
