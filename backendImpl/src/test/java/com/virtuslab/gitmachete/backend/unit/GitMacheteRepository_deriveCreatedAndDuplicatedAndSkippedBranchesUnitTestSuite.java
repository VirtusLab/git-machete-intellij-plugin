package com.virtuslab.gitmachete.backend.unit;

import static com.virtuslab.gitmachete.backend.unit.UnitTestUtils.createGitCoreCommit;
import static com.virtuslab.gitmachete.backend.unit.UnitTestUtils.createGitCoreLocalBranch;

import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.IBranchLayoutEntry;
import com.virtuslab.gitcore.api.IGitCoreLocalBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;

public class GitMacheteRepository_deriveCreatedAndDuplicatedAndSkippedBranchesUnitTestSuite
    extends
      BaseGitMacheteRepositoryUnitTestSuite {

  @SneakyThrows
  private IGitMacheteRepositorySnapshot invokeCreateSnapshot(
      IBranchLayout branchLayout, IGitCoreLocalBranchSnapshot... localBranchSnapshots) {
    PowerMockito.doReturn(List.empty()).when(gitCoreRepository).deriveAllRemoteNames();
    return Whitebox.invokeMethod(aux(localBranchSnapshots), "createSnapshot", branchLayout);
  }

  @Test
  public void singleBranch() {
    // given
    var branchAndEntry = createBranchAndEntry("main", List.empty());
    var branchLayout = PowerMockito.mock(IBranchLayout.class);
    PowerMockito.doReturn(List.of(branchAndEntry.entry)).when(branchLayout).getRootEntries();

    // when
    var repositorySnapshot = invokeCreateSnapshot(branchLayout, branchAndEntry.branch);

    // then
    Assert.assertEquals(List.of(branchAndEntry.entry).map(IBranchLayoutEntry::getName),
        repositorySnapshot.getManagedBranches().map(IManagedBranchSnapshot::getName));
    Assert.assertTrue(repositorySnapshot.getDuplicatedBranchNames().isEmpty());
    Assert.assertTrue(repositorySnapshot.getSkippedBranchNames().isEmpty());
  }

  @Test
  public void duplicatedBranch() {
    // given
    var mainBranchName = "main";
    var duplicatedEntry = createEntry(mainBranchName, List.empty());
    var branchAndEntry = createBranchAndEntry(mainBranchName, List.of(duplicatedEntry));
    var branchLayout = PowerMockito.mock(IBranchLayout.class);
    PowerMockito.doReturn(List.of(branchAndEntry.entry)).when(branchLayout).getRootEntries();

    // when
    var repositorySnapshot = invokeCreateSnapshot(branchLayout, branchAndEntry.branch);

    // then
    Assert.assertEquals(
        List.of(branchAndEntry.entry).map(IBranchLayoutEntry::getName),
        repositorySnapshot.getManagedBranches().map(IManagedBranchSnapshot::getName));
    Assert.assertEquals(List.of(mainBranchName).toSet(), repositorySnapshot.getDuplicatedBranchNames());
    Assert.assertTrue(repositorySnapshot.getSkippedBranchNames().isEmpty());
  }

  @Test
  public void skippedBranch() {
    // given
    var skippedBranchName = "skipped";
    var skippedEntry = createEntry(skippedBranchName, List.empty());
    var branchAndEntry = createBranchAndEntry("main", List.of(skippedEntry));
    var branchLayout = PowerMockito.mock(IBranchLayout.class);
    PowerMockito.doReturn(List.of(branchAndEntry.entry)).when(branchLayout).getRootEntries();

    // when
    var repositorySnapshot = invokeCreateSnapshot(branchLayout, branchAndEntry.branch);

    // then
    Assert.assertEquals(
        List.of(branchAndEntry.entry).map(IBranchLayoutEntry::getName),
        repositorySnapshot.getManagedBranches().map(IManagedBranchSnapshot::getName));
    Assert.assertTrue(repositorySnapshot.getDuplicatedBranchNames().isEmpty());
    Assert.assertEquals(List.of(skippedBranchName).toSet(), repositorySnapshot.getSkippedBranchNames());
  }

  @Test
  public void sameBranchDuplicatedAndSkipped() {
    // given
    var duplicatedAndSkippedBranchName = "duplicatedAndSkipped";
    var duplicatedAndSkippedBranchName2 = createEntry(duplicatedAndSkippedBranchName, List.empty());
    var duplicatedAndSkippedBranchName1 = createEntry(duplicatedAndSkippedBranchName, List.of(duplicatedAndSkippedBranchName2));
    var branchAndEntry = createBranchAndEntry("main", List.of(duplicatedAndSkippedBranchName1));
    var branchLayout = PowerMockito.mock(IBranchLayout.class);
    PowerMockito.doReturn(List.of(branchAndEntry.entry)).when(branchLayout).getRootEntries();

    // when
    var repositorySnapshot = invokeCreateSnapshot(branchLayout, branchAndEntry.branch);

    // then
    Assert.assertEquals(
        List.of(branchAndEntry.entry).map(IBranchLayoutEntry::getName),
        repositorySnapshot.getManagedBranches().map(IManagedBranchSnapshot::getName));
    Assert.assertTrue(repositorySnapshot.getDuplicatedBranchNames().isEmpty());
    Assert.assertEquals(List.of(duplicatedAndSkippedBranchName).toSet(), repositorySnapshot.getSkippedBranchNames());
  }

  private BranchAndEntry createBranchAndEntry(String name, List<IBranchLayoutEntry> childEntries) {
    var entry = createEntry(name, childEntries);
    var commit = createGitCoreCommit();
    var branch = createGitCoreLocalBranch(commit);
    PowerMockito.doReturn(name).when(branch).getName();
    return new BranchAndEntry(branch, entry);
  }

  private IBranchLayoutEntry createEntry(String name, List<IBranchLayoutEntry> childEntries) {
    var entry = PowerMockito.mock(IBranchLayoutEntry.class);
    PowerMockito.doReturn(name).when(entry).getName();
    PowerMockito.doReturn(Option.none()).when(entry).getCustomAnnotation();
    PowerMockito.doReturn(childEntries).when(entry).getChildren();
    return entry;
  }
}

@AllArgsConstructor
class BranchAndEntry {
  IGitCoreLocalBranchSnapshot branch;
  IBranchLayoutEntry entry;
}
