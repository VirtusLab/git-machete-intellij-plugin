package com.virtuslab.gitmachete.backend.unit;

import static com.virtuslab.gitmachete.backend.unit.TestUtils.createGitCoreCommit;
import static com.virtuslab.gitmachete.backend.unit.TestUtils.createGitCoreLocalBranch;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import io.vavr.collection.List;
import io.vavr.control.Option;
import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositoryFactory;

public class GitMacheteRepositoryFactory_deriveParentAwareForkPointTest {

  private final IGitCoreRepository gitCoreRepository = PowerMockito.mock(IGitCoreRepository.class);

  private Option<IGitCoreCommit> invokeDeriveParentAwareForkPoint(
      IGitCoreLocalBranch childBranch,
      IGitCoreLocalBranch parentBranch) throws Exception {
    return Whitebox.invokeMethod(PowerMockito.mock(GitMacheteRepositoryFactory.class),
        "deriveParentAwareForkPoint", gitCoreRepository, childBranch, parentBranch);
  }

  @Test(expected = GitMacheteException.class)
  public void gitCoreThrows() throws Exception {
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
  public void parentAgnosticForkPointIsMissingAndParentIsNotAncestorOfChild() throws Exception {
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
  public void parentAgnosticForkPointIsMissingAndParentIsAncestorOfChild() throws Exception {
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
  public void parentIsNotAncestorOfForkPointAndParentIsAncestorOfChild() throws Exception {
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
  public void parentIsNotAncestorOfForkPointAndParentIsNotAncestorOfChild() throws Exception {
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
  public void parentIsAncestorOfForkPointAndParentIsNotAncestorOfChild() throws Exception {
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
  public void parentIsAncestorOfForkPointAndParentIsAncestorOfChild() throws Exception {
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
