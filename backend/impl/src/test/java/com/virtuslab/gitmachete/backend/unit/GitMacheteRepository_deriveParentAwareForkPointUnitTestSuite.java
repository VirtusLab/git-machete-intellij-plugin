package com.virtuslab.gitmachete.backend.unit;

import static com.virtuslab.gitmachete.backend.unit.UnitTestUtils.createGitCoreCommit;
import static com.virtuslab.gitmachete.backend.unit.UnitTestUtils.createGitCoreLocalBranch;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.vavr.collection.Stream;
import lombok.SneakyThrows;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;

import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreLocalBranchSnapshot;
import com.virtuslab.gitmachete.backend.impl.ForkPointCommitOfManagedBranch;

public class GitMacheteRepository_deriveParentAwareForkPointUnitTestSuite extends BaseGitMacheteRepositoryUnitTestSuite {

  @SneakyThrows
  private @Nullable IGitCoreCommit invokeDeriveParentAwareForkPoint(
      IGitCoreLocalBranchSnapshot childBranch,
      IGitCoreLocalBranchSnapshot parentBranch) {

    when(gitCoreRepository.deriveConfigValue(any(), any(), any())).thenReturn(null);

    ForkPointCommitOfManagedBranch forkPoint = aux(childBranch, parentBranch).deriveParentAwareForkPoint(childBranch,
        parentBranch);
    if (forkPoint != null) {
      return forkPoint.getCoreCommit();
    } else {
      return null;
    }
  }

  @Test
  @SneakyThrows
  public void parentAgnosticForkPointIsMissingAndParentIsNotAncestorOfChild() {
    // given
    IGitCoreCommit childCommit = createGitCoreCommit();
    IGitCoreCommit parentCommit = createGitCoreCommit();
    IGitCoreLocalBranchSnapshot childBranch = createGitCoreLocalBranch(childCommit);
    IGitCoreLocalBranchSnapshot parentBranch = createGitCoreLocalBranch(parentCommit);

    when(gitCoreRepository.ancestorsOf(childCommit)).thenReturn(Stream.empty());
    when(gitCoreRepository.isAncestorOrEqual(parentCommit, childCommit)).thenReturn(false);

    // when
    IGitCoreCommit result = invokeDeriveParentAwareForkPoint(childBranch, parentBranch);

    // then
    assertNull(result);
  }

  @Test
  @SneakyThrows
  public void parentAgnosticForkPointIsMissingAndParentIsAncestorOfChild() {
    // given
    IGitCoreCommit parentCommit = createGitCoreCommit();
    IGitCoreCommit childCommit = createGitCoreCommit();
    IGitCoreLocalBranchSnapshot parentBranch = createGitCoreLocalBranch(parentCommit);
    IGitCoreLocalBranchSnapshot childBranch = createGitCoreLocalBranch(childCommit);

    when(gitCoreRepository.ancestorsOf(childCommit)).thenReturn(Stream.empty());
    when(gitCoreRepository.isAncestorOrEqual(parentCommit, childCommit)).thenReturn(true);

    // when
    IGitCoreCommit result = invokeDeriveParentAwareForkPoint(childBranch, parentBranch);

    // then
    assertNotNull(result);
    assertEquals(parentCommit, result);
  }

  @Test
  @SneakyThrows
  public void parentIsNotAncestorOfForkPointAndParentIsAncestorOfChild() {
    // given
    IGitCoreCommit forkPointCommit = createGitCoreCommit();
    IGitCoreCommit parentCommit = createGitCoreCommit();
    IGitCoreCommit childCommit = createGitCoreCommit();
    IGitCoreLocalBranchSnapshot parentBranch = createGitCoreLocalBranch(parentCommit);
    IGitCoreLocalBranchSnapshot childBranch = createGitCoreLocalBranch(childCommit);

    when(gitCoreRepository.ancestorsOf(childCommit)).thenReturn(Stream.of(forkPointCommit));
    when(gitCoreRepository.isAncestorOrEqual(parentCommit, forkPointCommit)).thenReturn(false);
    when(gitCoreRepository.isAncestorOrEqual(parentCommit, childCommit)).thenReturn(true);

    // when
    IGitCoreCommit result = invokeDeriveParentAwareForkPoint(childBranch, parentBranch);

    // then
    assertNotNull(result);
    assertEquals(parentCommit, result);
  }
}
