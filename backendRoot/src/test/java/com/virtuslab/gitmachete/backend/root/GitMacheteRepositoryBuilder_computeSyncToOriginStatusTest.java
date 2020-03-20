package com.virtuslab.gitmachete.backend.root;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.IGitCoreBranchTrackingStatus;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitmachete.backend.api.SyncToOriginStatus;

public class GitMacheteRepositoryBuilder_computeSyncToOriginStatusTest {

  private SyncToOriginStatus invokeComputeSyncToOriginStatus(IGitCoreLocalBranch coreLocalBranch) throws Exception {
    return Whitebox.invokeMethod(PowerMockito.mock(GitMacheteRepositoryBuilder.class),
        "computeSyncToOriginStatus",
        coreLocalBranch);
  }

  @Test
  public void computeSyncToOriginStatus_Untracked() throws Exception {
    // given
    IGitCoreLocalBranch coreLocalBranch = PowerMockito.mock(IGitCoreLocalBranch.class);
    PowerMockito.doReturn(Optional.empty()).when(coreLocalBranch).computeRemoteTrackingStatus();

    // when
    SyncToOriginStatus status = invokeComputeSyncToOriginStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToOriginStatus.Untracked, status);
  }

  @Test
  public void computeSyncToOriginStatus_Diverged() throws Exception {
    // given
    IGitCoreLocalBranch coreLocalBranch = PowerMockito.mock(IGitCoreLocalBranch.class);
    PowerMockito.doReturn(getTrackingStatusOptional(1, 1)).when(coreLocalBranch).computeRemoteTrackingStatus();

    // when
    SyncToOriginStatus status = invokeComputeSyncToOriginStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToOriginStatus.Diverged, status);
  }

  @Test
  public void computeSyncToOriginStatus_Ahead() throws Exception {
    // given
    IGitCoreLocalBranch coreLocalBranch = PowerMockito.mock(IGitCoreLocalBranch.class);
    PowerMockito.doReturn(getTrackingStatusOptional(1, 0)).when(coreLocalBranch).computeRemoteTrackingStatus();

    // when
    SyncToOriginStatus status = invokeComputeSyncToOriginStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToOriginStatus.Ahead, status);
  }

  @Test
  public void computeSyncToOriginStatus_Behind() throws Exception {
    // given
    IGitCoreLocalBranch coreLocalBranch = PowerMockito.mock(IGitCoreLocalBranch.class);
    PowerMockito.doReturn(getTrackingStatusOptional(0, 1)).when(coreLocalBranch).computeRemoteTrackingStatus();

    // when
    SyncToOriginStatus status = invokeComputeSyncToOriginStatus(coreLocalBranch);

    // then
    Assert.assertEquals(SyncToOriginStatus.Behind, status);
  }

  @Test
  public void computeSyncToOriginStatus_InSync() throws Exception {
    // given
    IGitCoreLocalBranch coreLocalBranch = PowerMockito.mock(IGitCoreLocalBranch.class);
    PowerMockito.doReturn(getTrackingStatusOptional(0, 0)).when(coreLocalBranch).computeRemoteTrackingStatus();

    // when
    SyncToOriginStatus status = invokeComputeSyncToOriginStatus(coreLocalBranch);

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
