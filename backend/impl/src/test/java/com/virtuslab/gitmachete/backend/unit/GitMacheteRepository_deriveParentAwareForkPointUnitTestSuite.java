package com.virtuslab.gitmachete.backend.unit;

import static com.virtuslab.gitmachete.backend.unit.UnitTestUtils.createGitCoreCommit;
import static com.virtuslab.gitmachete.backend.unit.UnitTestUtils.createGitCoreLocalBranch;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;

import io.vavr.collection.Stream;
import lombok.SneakyThrows;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreLocalBranchSnapshot;
import com.virtuslab.gitmachete.backend.impl.ForkPointCommitOfManagedBranch;

public class GitMacheteRepository_deriveParentAwareForkPointUnitTestSuite extends BaseGitMacheteRepositoryUnitTestSuite {

  @SneakyThrows
  private @Nullable IGitCoreCommit invokeDeriveParentAwareForkPoint(
      IGitCoreLocalBranchSnapshot childBranch,
      IGitCoreLocalBranchSnapshot parentBranch) {

    PowerMockito.doReturn(null).when(gitCoreRepository).deriveConfigValue(any(), any(), any());

    ForkPointCommitOfManagedBranch forkPoint = Whitebox.invokeMethod(
        aux(childBranch, parentBranch), "deriveParentAwareForkPoint", childBranch, parentBranch);
    if (forkPoint != null) {
      return Whitebox.invokeMethod(forkPoint, "getCoreCommit");
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

    PowerMockito.doReturn(Stream.empty()).when(gitCoreRepository).ancestorsOf(childCommit);
    PowerMockito.doReturn(false).when(gitCoreRepository).isAncestorOrEqual(parentCommit, childCommit);

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

    PowerMockito.doReturn(Stream.empty()).when(gitCoreRepository).ancestorsOf(childCommit);
    PowerMockito.doReturn(true).when(gitCoreRepository).isAncestorOrEqual(parentCommit, childCommit);

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

    PowerMockito.doReturn(Stream.of(forkPointCommit)).when(gitCoreRepository).ancestorsOf(childCommit);
    PowerMockito.doReturn(false).when(gitCoreRepository).isAncestorOrEqual(parentCommit, forkPointCommit);
    PowerMockito.doReturn(true).when(gitCoreRepository).isAncestorOrEqual(parentCommit, childCommit);

    // when
    IGitCoreCommit result = invokeDeriveParentAwareForkPoint(childBranch, parentBranch);

    // then
    assertNotNull(result);
    assertEquals(parentCommit, result);
  }
}
