package com.virtuslab.gitmachete.backend.unit;

import static com.virtuslab.gitmachete.backend.unit.UnitTestUtils.createGitCoreCommit;
import static com.virtuslab.gitmachete.backend.unit.UnitTestUtils.createGitCoreLocalBranch;

import io.vavr.collection.List;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.BranchLayoutEntry;
import com.virtuslab.gitcore.api.IGitCoreLocalBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;

public class GitMacheteRepository_deriveCreatedAndDuplicatedAndSkippedBranchesUnitTestSuite
    extends
      BaseGitMacheteRepositoryUnitTestSuite {

  @SneakyThrows
  private IGitMacheteRepositorySnapshot invokeCreateSnapshot(
      BranchLayout branchLayout, IGitCoreLocalBranchSnapshot... localBranchSnapshots) {
    PowerMockito.doReturn(List.empty()).when(gitCoreRepository).deriveAllRemoteNames();
    return Whitebox.invokeMethod(aux(localBranchSnapshots), "createSnapshot", branchLayout);
  }

  @Test
  public void singleBranch() {
    // given
    val branchAndEntry = createBranchAndEntry("main", List.empty());
    val branchLayout = new BranchLayout(List.of(branchAndEntry.entry));

    // when
    val repositorySnapshot = invokeCreateSnapshot(branchLayout, branchAndEntry.branch);

    // then
    Assert.assertEquals(List.of(branchAndEntry.entry).map(BranchLayoutEntry::getName),
        repositorySnapshot.getManagedBranches().map(IManagedBranchSnapshot::getName));
    Assert.assertTrue(repositorySnapshot.getDuplicatedBranchNames().isEmpty());
    Assert.assertTrue(repositorySnapshot.getSkippedBranchNames().isEmpty());
  }

  @Test
  public void duplicatedBranch() {
    // given
    val mainBranchName = "main";
    val duplicatedEntry = createEntry(mainBranchName, List.empty());
    val branchAndEntry = createBranchAndEntry(mainBranchName, List.of(duplicatedEntry));
    val branchLayout = new BranchLayout(List.of(branchAndEntry.entry));

    // when
    val repositorySnapshot = invokeCreateSnapshot(branchLayout, branchAndEntry.branch);

    // then
    Assert.assertEquals(
        List.of(branchAndEntry.entry).map(BranchLayoutEntry::getName),
        repositorySnapshot.getManagedBranches().map(IManagedBranchSnapshot::getName));
    Assert.assertEquals(List.of(mainBranchName).toSet(), repositorySnapshot.getDuplicatedBranchNames());
    Assert.assertTrue(repositorySnapshot.getSkippedBranchNames().isEmpty());
  }

  @Test
  public void skippedBranch() {
    // given
    val skippedBranchName = "skipped";
    val skippedEntry = createEntry(skippedBranchName, List.empty());
    val branchAndEntry = createBranchAndEntry("main", List.of(skippedEntry));
    val branchLayout = new BranchLayout(List.of(branchAndEntry.entry));

    // when
    val repositorySnapshot = invokeCreateSnapshot(branchLayout, branchAndEntry.branch);

    // then
    Assert.assertEquals(
        List.of(branchAndEntry.entry).map(BranchLayoutEntry::getName),
        repositorySnapshot.getManagedBranches().map(IManagedBranchSnapshot::getName));
    Assert.assertTrue(repositorySnapshot.getDuplicatedBranchNames().isEmpty());
    Assert.assertEquals(List.of(skippedBranchName).toSet(), repositorySnapshot.getSkippedBranchNames());
  }

  @Test
  public void sameBranchDuplicatedAndSkipped() {
    // given
    val duplicatedAndSkippedBranchName = "duplicatedAndSkipped";
    val duplicatedAndSkippedBranchName2 = createEntry(duplicatedAndSkippedBranchName, List.empty());
    val duplicatedAndSkippedBranchName1 = createEntry(duplicatedAndSkippedBranchName, List.of(duplicatedAndSkippedBranchName2));
    val branchAndEntry = createBranchAndEntry("main", List.of(duplicatedAndSkippedBranchName1));
    val branchLayout = new BranchLayout(List.of(branchAndEntry.entry));

    // when
    val repositorySnapshot = invokeCreateSnapshot(branchLayout, branchAndEntry.branch);

    // then
    Assert.assertEquals(
        List.of(branchAndEntry.entry).map(BranchLayoutEntry::getName),
        repositorySnapshot.getManagedBranches().map(IManagedBranchSnapshot::getName));
    Assert.assertTrue(repositorySnapshot.getDuplicatedBranchNames().isEmpty());
    Assert.assertEquals(List.of(duplicatedAndSkippedBranchName).toSet(), repositorySnapshot.getSkippedBranchNames());
  }

  private BranchAndEntry createBranchAndEntry(String name, List<BranchLayoutEntry> childEntries) {
    val entry = createEntry(name, childEntries);
    val commit = createGitCoreCommit();
    val branch = createGitCoreLocalBranch(commit);
    PowerMockito.doReturn(name).when(branch).getName();
    return new BranchAndEntry(branch, entry);
  }

  private BranchLayoutEntry createEntry(String name, List<BranchLayoutEntry> childEntries) {
    return new BranchLayoutEntry(name, /* customAnnotation */ null, childEntries);
  }
}

@AllArgsConstructor
class BranchAndEntry {
  IGitCoreLocalBranchSnapshot branch;
  BranchLayoutEntry entry;
}
