package com.virtuslab.gitmachete.backend.root;

import io.vavr.control.Option;
import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.IGitCoreBranchTrackingStatus;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.impl.jgit.GitCoreBranchTrackingStatus;
import com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus;

public class GitMacheteRepositoryFactory_deriveSyncToRemoteStatusTest {

  private final IGitCoreLocalBranch coreLocalBranch = PowerMockito.mock(IGitCoreLocalBranch.class);

  private ISyncToRemoteStatus invokeDeriveSyncToRemoteStatus(IGitCoreLocalBranch coreLocalBranch) throws Exception {
    return Whitebox.invokeMethod(PowerMockito.mock(GitMacheteRepositoryFactory.class),
        "deriveSyncToRemoteStatus",
        coreLocalBranch);
  }

  @Test
  public void deriveSyncToRemoteStatus_Untracked() throws Exception {
    // given
    PowerMockito.doReturn(Option.none()).when(coreLocalBranch).deriveRemoteTrackingStatus();

    // when
    ISyncToRemoteStatus status = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(ISyncToRemoteStatus.Relation.Untracked, status.getRelation());
  }

  @Test
  public void deriveSyncToRemoteStatus_Diverged() throws Exception {
    // given
    PowerMockito.doReturn(getTrackingStatusOption(1, 1, "origin")).when(coreLocalBranch).deriveRemoteTrackingStatus();

    // when
    ISyncToRemoteStatus status = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(ISyncToRemoteStatus.Relation.Diverged, status.getRelation());
  }

  @Test
  public void deriveSyncToRemoteStatus_Ahead() throws Exception {
    // given
    PowerMockito.doReturn(getTrackingStatusOption(1, 0, "origin")).when(coreLocalBranch).deriveRemoteTrackingStatus();

    // when
    ISyncToRemoteStatus status = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(ISyncToRemoteStatus.Relation.Ahead, status.getRelation());
  }

  @Test
  public void deriveSyncToRemoteStatus_Behind() throws Exception {
    // given
    PowerMockito.doReturn(getTrackingStatusOption(0, 1, "origin")).when(coreLocalBranch).deriveRemoteTrackingStatus();

    // when
    ISyncToRemoteStatus status = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(ISyncToRemoteStatus.Relation.Behind, status.getRelation());
  }

  @Test
  public void deriveSyncToRemoteStatus_InSync() throws Exception {
    // given
    PowerMockito.doReturn(getTrackingStatusOption(0, 0, "origin")).when(coreLocalBranch).deriveRemoteTrackingStatus();

    // when
    ISyncToRemoteStatus status = invokeDeriveSyncToRemoteStatus(coreLocalBranch);

    // then
    Assert.assertEquals(ISyncToRemoteStatus.Relation.InSync, status.getRelation());
  }

  private Option<IGitCoreBranchTrackingStatus> getTrackingStatusOption(int ahead, int behind, String remoteName) {
    return Option.of(GitCoreBranchTrackingStatus.of(ahead, behind, remoteName));
  }
}
