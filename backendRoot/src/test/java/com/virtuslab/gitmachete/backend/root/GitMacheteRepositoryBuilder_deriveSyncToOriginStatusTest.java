package com.virtuslab.gitmachete.backend.root;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.IGitCoreBranchTrackingStatus;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitmachete.backend.api.SyncToOriginStatus;

public class GitMacheteRepositoryBuilder_deriveSyncToOriginStatusTest {

  private final IGitCoreLocalBranch coreLocalBranch = PowerMockito.mock(IGitCoreLocalBranch.class);

  private SyncToOriginStatus invokeDeriveSyncToOriginStatus(IGitCoreLocalBranch coreLocalBranch) throws Exception {
    return Whitebox.invokeMethod(PowerMockito.mock(GitMacheteRepositoryBuilder.class),
        "deriveSyncToOriginStatus",
        coreLocalBranch);
  }

  @Test
  public void deriveSyncToOriginStatus_Untracked() throws Exception {
    // given
    PowerMockito.doReturn(Optional.empty()).when(coreLocalBranch).deriveRemoteTrackingStatus();

    // when
    SyncToOriginStatus status = invokeDeriveSyncToOriginStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToOriginStatus.Untracked, status);
  }

  @Test
  public void deriveSyncToOriginStatus_Diverged() throws Exception {
    // given
    PowerMockito.doReturn(getTrackingStatusOptional(1, 1)).when(coreLocalBranch).deriveRemoteTrackingStatus();

    // when
    SyncToOriginStatus status = invokeDeriveSyncToOriginStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToOriginStatus.Diverged, status);
  }

  @Test
  public void deriveSyncToOriginStatus_Ahead() throws Exception {
    // given
    PowerMockito.doReturn(getTrackingStatusOptional(1, 0)).when(coreLocalBranch).deriveRemoteTrackingStatus();

    // when
    SyncToOriginStatus status = invokeDeriveSyncToOriginStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToOriginStatus.Ahead, status);
  }

  @Test
  public void deriveSyncToOriginStatus_Behind() throws Exception {
    // given
    PowerMockito.doReturn(getTrackingStatusOptional(0, 1)).when(coreLocalBranch).deriveRemoteTrackingStatus();

    // when
    SyncToOriginStatus status = invokeDeriveSyncToOriginStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToOriginStatus.Behind, status);
  }

  @Test
  public void deriveSyncToOriginStatus_InSync() throws Exception {
    // given
    PowerMockito.doReturn(getTrackingStatusOptional(0, 0)).when(coreLocalBranch).deriveRemoteTrackingStatus();

    // when
    SyncToOriginStatus status = invokeDeriveSyncToOriginStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToOriginStatus.InSync, status);
  }

  private Optional<IGitCoreBranchTrackingStatus> getTrackingStatusOptional(int ahead, int behind) {
    return Optional.of(new IGitCoreBranchTrackingStatus() {
      @Override
      public int getAhead() {
        return ahead;
      }

      @Override
      public int getBehind() {
        return behind;
      }
    });
  }
}
