package com.virtuslab.gitmachete.backend.unit;

import static com.virtuslab.gitmachete.backend.unit.TestUtils.createGitCoreCommit;
import static com.virtuslab.gitmachete.backend.unit.TestUtils.createGitCoreLocalBranch;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;

public class GitMacheteRepositoryFactory_deriveParentAwareForkPointTest extends BaseGitMacheteRepositoryFactoryTest {

  @SneakyThrows
  private Option<IGitCoreCommit> invokeDeriveParentAwareForkPoint(
      IGitCoreLocalBranch childBranch,
      IGitCoreLocalBranch parentBranch) {
    Object coreForkPoint = Whitebox.invokeMethod(aux, "deriveParentAwareForkPoint", childBranch, parentBranch);
    if (coreForkPoint != null)
      return Option.some(Whitebox.getInternalState(coreForkPoint, "coreCommit"));
    else
      return Option.none();
  }

  @Test(expected = GitMacheteException.class)
  @SneakyThrows
  public void gitCoreThrows() {
    // given
    IGitCoreCommit commit = createGitCoreCommit();
    IGitCoreLocalBranch childBranch = createGitCoreLocalBranch(commit);
    IGitCoreLocalBranch parentBranch = createGitCoreLocalBranch(commit);

    PowerMockito.doThrow(new GitCoreException()).when(gitCoreRepository).deriveAllLocalBranches();

    // when
    invokeDeriveParentAwareForkPoint(childBranch, parentBranch);

    // then exception is thrown
  }

  @Test
  @SneakyThrows
  public void parentAgnosticForkPointIsMissingAndParentIsNotAncestorOfChild() {
    // given
    IGitCoreCommit childCommit = createGitCoreCommit();
    IGitCoreCommit parentCommit = createGitCoreCommit();
    IGitCoreLocalBranch childBranch = createGitCoreLocalBranch(childCommit);
    IGitCoreLocalBranch parentBranch = createGitCoreLocalBranch(parentCommit);

    PowerMockito.doReturn(List.of(childBranch, parentBranch)).when(gitCoreRepository).deriveAllLocalBranches();
    PowerMockito.doReturn(List.empty()).when(gitCoreRepository).deriveAllRemoteBranches();
    PowerMockito.doReturn(Option.none()).when(gitCoreRepository).findFirstAncestor(eq(childCommit), any());

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

    PowerMockito.doReturn(List.of(childBranch, parentBranch)).when(gitCoreRepository).deriveAllLocalBranches();
    PowerMockito.doReturn(List.empty()).when(gitCoreRepository).deriveAllRemoteBranches();
    PowerMockito.doReturn(Option.none()).when(gitCoreRepository).findFirstAncestor(eq(childCommit), any());

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

    PowerMockito.doReturn(List.of(childBranch, parentBranch)).when(gitCoreRepository).deriveAllLocalBranches();
    PowerMockito.doReturn(List.empty()).when(gitCoreRepository).deriveAllRemoteBranches();
    PowerMockito.doReturn(Option.some(forkPointCommit)).when(gitCoreRepository).findFirstAncestor(eq(childCommit), any());

    PowerMockito.doReturn(false).when(gitCoreRepository).isAncestor(parentCommit, forkPointCommit);
    PowerMockito.doReturn(true).when(gitCoreRepository).isAncestor(parentCommit, childCommit);

    // when
    Option<IGitCoreCommit> result = invokeDeriveParentAwareForkPoint(childBranch, parentBranch);

    // then
    Assert.assertTrue(result.isDefined());
    Assert.assertEquals(parentCommit, result.get());
  }

  @Test
  @SneakyThrows
  public void parentIsNotAncestorOfForkPointAndParentIsNotAncestorOfChild() {
    // given
    IGitCoreCommit forkPointCommit = createGitCoreCommit();
    IGitCoreCommit parentCommit = createGitCoreCommit();
    IGitCoreCommit childCommit = createGitCoreCommit();
    IGitCoreLocalBranch parentBranch = createGitCoreLocalBranch(parentCommit);
    IGitCoreLocalBranch childBranch = createGitCoreLocalBranch(childCommit);

    PowerMockito.doReturn(List.of(childBranch, parentBranch)).when(gitCoreRepository).deriveAllLocalBranches();
    PowerMockito.doReturn(List.empty()).when(gitCoreRepository).deriveAllRemoteBranches();
    PowerMockito.doReturn(Option.some(forkPointCommit)).when(gitCoreRepository).findFirstAncestor(eq(childCommit), any());

    PowerMockito.doReturn(false).when(gitCoreRepository).isAncestor(parentCommit, forkPointCommit);
    PowerMockito.doReturn(false).when(gitCoreRepository).isAncestor(parentCommit, childCommit);

    // when
    Option<IGitCoreCommit> result = invokeDeriveParentAwareForkPoint(childBranch, parentBranch);

    // then
    Assert.assertTrue(result.isDefined());
    Assert.assertEquals(forkPointCommit, result.get());
  }

  @Test
  @SneakyThrows
  public void parentIsAncestorOfForkPointAndParentIsNotAncestorOfChild() {
    // given
    IGitCoreCommit forkPointCommit = createGitCoreCommit();
    IGitCoreCommit parentCommit = createGitCoreCommit();
    IGitCoreCommit childCommit = createGitCoreCommit();
    IGitCoreLocalBranch parentBranch = createGitCoreLocalBranch(parentCommit);
    IGitCoreLocalBranch childBranch = createGitCoreLocalBranch(childCommit);

    PowerMockito.doReturn(List.of(childBranch, parentBranch)).when(gitCoreRepository).deriveAllLocalBranches();
    PowerMockito.doReturn(List.empty()).when(gitCoreRepository).deriveAllRemoteBranches();
    PowerMockito.doReturn(Option.some(forkPointCommit)).when(gitCoreRepository).findFirstAncestor(eq(childCommit), any());

    PowerMockito.doReturn(true).when(gitCoreRepository).isAncestor(parentCommit, forkPointCommit);
    PowerMockito.doReturn(false).when(gitCoreRepository).isAncestor(parentCommit, childCommit);

    // when
    Option<IGitCoreCommit> result = invokeDeriveParentAwareForkPoint(childBranch, parentBranch);

    // then
    Assert.assertTrue(result.isDefined());
    Assert.assertEquals(forkPointCommit, result.get());
  }

  @Test
  @SneakyThrows
  public void parentIsAncestorOfForkPointAndParentIsAncestorOfChild() {
    // given
    IGitCoreCommit forkPointCommit = createGitCoreCommit();
    IGitCoreCommit parentCommit = createGitCoreCommit();
    IGitCoreCommit childCommit = createGitCoreCommit();
    IGitCoreLocalBranch parentBranch = createGitCoreLocalBranch(parentCommit);
    IGitCoreLocalBranch childBranch = createGitCoreLocalBranch(childCommit);

    PowerMockito.doReturn(List.of(childBranch, parentBranch)).when(gitCoreRepository).deriveAllLocalBranches();
    PowerMockito.doReturn(List.empty()).when(gitCoreRepository).deriveAllRemoteBranches();
    PowerMockito.doReturn(Option.some(forkPointCommit)).when(gitCoreRepository).findFirstAncestor(eq(childCommit), any());

    PowerMockito.doReturn(true).when(gitCoreRepository).isAncestor(parentCommit, forkPointCommit);
    PowerMockito.doReturn(true).when(gitCoreRepository).isAncestor(parentCommit, childCommit);

    // when
    Option<IGitCoreCommit> result = invokeDeriveParentAwareForkPoint(childBranch, parentBranch);

    // then
    Assert.assertTrue(result.isDefined());
    Assert.assertEquals(forkPointCommit, result.get());
  }
}
