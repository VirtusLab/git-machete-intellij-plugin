package com.virtuslab.gitmachete.backend.unit;

import static com.virtuslab.gitmachete.backend.unit.TestUtils.createGitCoreCommit;
import static com.virtuslab.gitmachete.backend.unit.TestUtils.createGitCoreLocalBranch;
import static org.mockito.ArgumentMatchers.any;

import io.vavr.collection.Stream;
import io.vavr.control.Option;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitmachete.backend.impl.GitMacheteForkPointCommit;

public class GitMacheteRepositoryFactory_deriveParentAwareForkPointTestSuite extends BaseGitMacheteRepositoryFactoryTestSuite {

  @SneakyThrows
  private Option<IGitCoreCommit> invokeDeriveParentAwareForkPoint(
      IGitCoreLocalBranch childBranch,
      IGitCoreLocalBranch parentBranch) {

    PowerMockito.doReturn(Option.none()).when(gitCoreRepository).deriveConfigValue(any(), any(), any());

    GitMacheteForkPointCommit forkPoint = Whitebox.invokeMethod(
        aux(childBranch, parentBranch), "deriveParentAwareForkPoint", childBranch, parentBranch);
    if (forkPoint != null) {
      return Option.some(Whitebox.invokeMethod(forkPoint, "getCoreCommit"));
    } else {
      return Option.none();
    }
  }

  @Test
  @SneakyThrows
  public void parentAgnosticForkPointIsMissingAndParentIsNotAncestorOfChild() {
    // given
    IGitCoreCommit childCommit = createGitCoreCommit();
    IGitCoreCommit parentCommit = createGitCoreCommit();
    IGitCoreLocalBranch childBranch = createGitCoreLocalBranch(childCommit);
    IGitCoreLocalBranch parentBranch = createGitCoreLocalBranch(parentCommit);

    PowerMockito.doReturn(Stream.empty()).when(gitCoreRepository).ancestorsOf(childCommit);
    PowerMockito.doReturn(false).when(gitCoreRepository).isAncestor(parentCommit, childCommit);

    // when
    Option<IGitCoreCommit> result = invokeDeriveParentAwareForkPoint(childBranch, parentBranch);

    // then
    Assert.assertTrue(result.isEmpty());
  }

  @Test
  @SneakyThrows
  public void parentAgnosticForkPointIsMissingAndParentIsAncestorOfChild() {
    // given
    IGitCoreCommit parentCommit = createGitCoreCommit();
    IGitCoreCommit childCommit = createGitCoreCommit();
    IGitCoreLocalBranch parentBranch = createGitCoreLocalBranch(parentCommit);
    IGitCoreLocalBranch childBranch = createGitCoreLocalBranch(childCommit);

    PowerMockito.doReturn(Stream.empty()).when(gitCoreRepository).ancestorsOf(childCommit);
    PowerMockito.doReturn(true).when(gitCoreRepository).isAncestor(parentCommit, childCommit);

    // when
    Option<IGitCoreCommit> result = invokeDeriveParentAwareForkPoint(childBranch, parentBranch);

    // then
    Assert.assertTrue(result.isDefined());
    Assert.assertEquals(parentCommit, result.get());
  }

  @Test
  @SneakyThrows
  public void parentIsNotAncestorOfForkPointAndParentIsAncestorOfChild() {
    // given
    IGitCoreCommit forkPointCommit = createGitCoreCommit();
    IGitCoreCommit parentCommit = createGitCoreCommit();
    IGitCoreCommit childCommit = createGitCoreCommit();
    IGitCoreLocalBranch parentBranch = createGitCoreLocalBranch(parentCommit);
    IGitCoreLocalBranch childBranch = createGitCoreLocalBranch(childCommit);

    PowerMockito.doReturn(Stream.of(forkPointCommit)).when(gitCoreRepository).ancestorsOf(childCommit);
    PowerMockito.doReturn(false).when(gitCoreRepository).isAncestor(parentCommit, forkPointCommit);
    PowerMockito.doReturn(true).when(gitCoreRepository).isAncestor(parentCommit, childCommit);

    // when
    Option<IGitCoreCommit> result = invokeDeriveParentAwareForkPoint(childBranch, parentBranch);

    // then
    Assert.assertTrue(result.isDefined());
    Assert.assertEquals(parentCommit, result.get());
  }
}
