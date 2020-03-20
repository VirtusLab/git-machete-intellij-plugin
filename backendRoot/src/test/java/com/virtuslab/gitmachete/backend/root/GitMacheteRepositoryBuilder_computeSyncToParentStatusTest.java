package com.virtuslab.gitmachete.backend.root;
import java.nio.file.Path;
import java.util.Date;
import java.util.Optional;

import io.vavr.collection.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.BaseGitCoreCommit;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreBranch;
import com.virtuslab.gitcore.api.IGitCoreCommitHash;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCorePersonIdentity;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranch;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitcore.api.IGitCoreSubmoduleEntry;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;

public class GitMacheteRepositoryBuilder_computeSyncToParentStatusTest {

  private static final GitMacheteRepositoryBuilder gitMacheteRepositoryBuilder = PowerMockito
      .mock(GitMacheteRepositoryBuilder.class);

  @BeforeClass
  public static void init() {
    Whitebox.setInternalState(gitMacheteRepositoryBuilder, "gitCoreRepository", new TestGitCoreRepository());
  }

  @Test
  public void branchIsARoot_inSync() throws Exception {
    // given
    BaseGitCoreCommit pointedCommit = getCommit(null);
    IGitCoreLocalBranch parent = null;
    IGitCoreLocalBranch branch = getGitCoreLocalBranch(pointedCommit);

    // when
    SyncToParentStatus syncToParentStatus = invokeComputeSyncToParentStatus(branch, parent);

    // then
    Assert.assertEquals(SyncToParentStatus.InSync, syncToParentStatus);
  }

  @Test
  public void branchAndParentPointingSameCommitAndBranchJustCreated_inSync() throws Exception {
    // given
    BaseGitCoreCommit pointedCommit = getCommit(null);
    IGitCoreLocalBranch parent = getGitCoreLocalBranch(pointedCommit);
    IGitCoreLocalBranch branch = getGitCoreLocalBranch(pointedCommit, /* forkPoint */null,
        /* hasJustBeenCreated */true);

    // when
    SyncToParentStatus syncToParentStatus = invokeComputeSyncToParentStatus(branch, parent);

    // then
    Assert.assertEquals(SyncToParentStatus.InSync, syncToParentStatus);
  }

  @Test
  public void branchAndParentPointingSameCommitAndBranchNotJustCreated_merged() throws Exception {
    // given
    BaseGitCoreCommit pointedCommit = getCommit(null);
    IGitCoreLocalBranch parent = getGitCoreLocalBranch(pointedCommit);
    IGitCoreLocalBranch branch = getGitCoreLocalBranch(pointedCommit);
    // when
    SyncToParentStatus syncToParentStatus = invokeComputeSyncToParentStatus(branch, parent);

    // then
    Assert.assertEquals(SyncToParentStatus.Merged, syncToParentStatus);
  }

  @Test
  public void parentPointedCommitIsAncestorOfBranchPointedCommitAndItsForkPoint_inSync() throws Exception {
    // given
    BaseGitCoreCommit parentPointedCommit = getCommit(null);
    BaseGitCoreCommit branchPointedCommit = getCommit(parentPointedCommit);
    IGitCoreLocalBranch parent = getGitCoreLocalBranch(parentPointedCommit);
    IGitCoreLocalBranch branch = getGitCoreLocalBranch(branchPointedCommit, /* forkPoint */ parentPointedCommit);

    // when
    SyncToParentStatus syncToParentStatus = invokeComputeSyncToParentStatus(branch, parent);

    // then
    Assert.assertEquals(SyncToParentStatus.InSync, syncToParentStatus);
  }

  @Test
  public void parentPointedCommitIsAncestorOfBranchPointedCommitButNotItsForkPoint_inSyncButOffForkPoint()
      throws Exception {
    // given
    BaseGitCoreCommit forkPointCommit = getCommit(null);
    BaseGitCoreCommit parentPointedCommit = getCommit(forkPointCommit);
    BaseGitCoreCommit branchPointedCommit = getCommit(parentPointedCommit);
    IGitCoreLocalBranch parent = getGitCoreLocalBranch(parentPointedCommit);
    IGitCoreLocalBranch branch = getGitCoreLocalBranch(branchPointedCommit, forkPointCommit);

    // when
    SyncToParentStatus syncToParentStatus = invokeComputeSyncToParentStatus(branch, parent);

    // then
    Assert.assertEquals(SyncToParentStatus.InSyncButForkPointOff, syncToParentStatus);
  }

  @Test
  public void branchPointedCommitIsAncestorOfParentPointedCommit_merged() throws Exception {
    // given
    BaseGitCoreCommit branchPointedCommit = getCommit(null);
    BaseGitCoreCommit parentPointedCommit = getCommit(branchPointedCommit);
    IGitCoreLocalBranch parent = getGitCoreLocalBranch(parentPointedCommit);
    IGitCoreLocalBranch branch = getGitCoreLocalBranch(branchPointedCommit);

    // when
    SyncToParentStatus syncToParentStatus = invokeComputeSyncToParentStatus(branch, parent);

    // then
    Assert.assertEquals(SyncToParentStatus.Merged, syncToParentStatus);
  }

  @Test
  public void neitherBranchPointedCommitIsAncestorOfParentPointedCommitNorTheOtherWay_outOffSync() throws Exception {
    // given
    BaseGitCoreCommit someCommit = getCommit(null);
    BaseGitCoreCommit parentPointedCommit = getCommit(someCommit);
    BaseGitCoreCommit branchPointedCommit = getCommit(someCommit);
    IGitCoreLocalBranch parent = getGitCoreLocalBranch(parentPointedCommit);
    IGitCoreLocalBranch branch = getGitCoreLocalBranch(branchPointedCommit);

    // when
    SyncToParentStatus syncToParentStatus = invokeComputeSyncToParentStatus(branch, parent);

    // then
    Assert.assertEquals(SyncToParentStatus.OutOfSync, syncToParentStatus);
  }

  TestGitCoreCommit getCommit(BaseGitCoreCommit parentCommit) {
    // assert parentCommit instanceof TestGitCoreCommit;
    return new TestGitCoreCommit((TestGitCoreCommit) parentCommit);
  }

  SyncToParentStatus invokeComputeSyncToParentStatus(IGitCoreLocalBranch coreLocalBranch,
      IGitCoreBranch parentCoreLocalBranch) throws Exception {
    return Whitebox.invokeMethod(gitMacheteRepositoryBuilder,
        "computeSyncToParentStatus",
        coreLocalBranch,
        parentCoreLocalBranch);
  }

  IGitCoreLocalBranch getGitCoreLocalBranch(BaseGitCoreCommit pointedCommit)
      throws GitCoreException {
    return getGitCoreLocalBranch(pointedCommit, null, false);
  }

  IGitCoreLocalBranch getGitCoreLocalBranch(BaseGitCoreCommit pointedCommit, BaseGitCoreCommit forkPoint)
      throws GitCoreException {
    return getGitCoreLocalBranch(pointedCommit, forkPoint, false);
  }

  IGitCoreLocalBranch getGitCoreLocalBranch(BaseGitCoreCommit pointedCommit, BaseGitCoreCommit forkPoint,
      boolean hasJustBeenCreated)
      throws GitCoreException {
    IGitCoreLocalBranch mock = PowerMockito.mock(IGitCoreLocalBranch.class);
    PowerMockito.doReturn(pointedCommit).when(mock).getPointedCommit();
    PowerMockito.doReturn(Optional.ofNullable(forkPoint)).when(mock).computeForkPoint();
    PowerMockito.doReturn(hasJustBeenCreated).when(mock).hasJustBeenCreated();
    return mock;
  }
}

class TestGitCoreCommit extends BaseGitCoreCommit {
  private static int counter;

  private final int id;
  private final TestGitCoreCommit parentCommit;

  TestGitCoreCommit(TestGitCoreCommit parentCommit) {
    this.parentCommit = parentCommit;
    id = counter++;
  }

  TestGitCoreCommit getParentCommit() {
    return parentCommit;
  }
  @Override
  public String getMessage() {
    return null;
  }

  @Override
  public IGitCorePersonIdentity getAuthor() {
    return null;
  }

  @Override
  public IGitCorePersonIdentity getCommitter() {
    return null;
  }

  @Override
  public Date getCommitTime() {
    return null;
  }

  @Override
  public IGitCoreCommitHash getHash() {
    return () -> String.valueOf(id);
  }
}

class TestGitCoreRepository implements IGitCoreRepository {
  @Override
  public boolean isAncestor(BaseGitCoreCommit presumedAncestor, BaseGitCoreCommit presumedDescendant) {
    assert presumedAncestor instanceof TestGitCoreCommit;
    assert presumedDescendant instanceof TestGitCoreCommit;
    while (presumedDescendant != null && !presumedDescendant.equals(presumedAncestor)) {
      presumedDescendant = ((TestGitCoreCommit) presumedDescendant).getParentCommit();
    }
    return presumedDescendant != null;
  }

  @Override
  public Optional<IGitCoreLocalBranch> getCurrentBranch() {
    return Optional.empty();
  }

  @Override
  public IGitCoreLocalBranch getLocalBranch(String branchName) {
    return null;
  }

  @Override
  public IGitCoreRemoteBranch getRemoteBranch(String branchName) {
    return null;
  }

  @Override
  public List<IGitCoreLocalBranch> getLocalBranches() {
    return List.empty();
  }

  @Override
  public List<IGitCoreRemoteBranch> getRemoteBranches() {
    return List.empty();
  }

  @Override
  public Path getRepositoryPath() {
    return null;
  }

  @Override
  public Path getGitFolderPath() {
    return null;
  }

  @Override
  public List<IGitCoreSubmoduleEntry> getSubmodules() {
    return List.empty();
  }
}
