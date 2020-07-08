package com.virtuslab.gitmachete.backend.unit;

import static com.virtuslab.gitmachete.backend.unit.UnitTestUtils.createGitCoreCommit;
import static com.virtuslab.gitmachete.backend.unit.UnitTestUtils.createGitCoreLocalBranch;
import static org.mockito.ArgumentMatchers.any;

import io.vavr.collection.Stream;
import io.vavr.control.Option;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreLocalBranchSnapshot;
import com.virtuslab.gitmachete.backend.impl.GitMacheteForkPointCommit;

public class GitMacheteRepository_deriveUpstreamAwareForkPointUnitTestSuite extends BaseGitMacheteRepositoryUnitTestSuite {

  @SneakyThrows
  private Option<IGitCoreCommit> invokeDeriveUpstreamAwareForkPoint(
      IGitCoreLocalBranchSnapshot downstreamBranch,
      IGitCoreLocalBranchSnapshot upstreamBranch) {

    PowerMockito.doReturn(Option.none()).when(gitCoreRepository).deriveConfigValue(any(), any(), any());

    GitMacheteForkPointCommit forkPoint = Whitebox.invokeMethod(
        aux(downstreamBranch, upstreamBranch), "deriveUpstreamAwareForkPoint", downstreamBranch, upstreamBranch);
    if (forkPoint != null) {
      return Option.some(Whitebox.invokeMethod(forkPoint, "getCoreCommit"));
    } else {
      return Option.none();
    }
  }

  @Test
  @SneakyThrows
  public void upstreamAgnosticForkPointIsMissingAndUpstreamIsNotAncestorOfDownstream() {
    // given
    IGitCoreCommit downstreamCommit = createGitCoreCommit();
    IGitCoreCommit upstreamCommit = createGitCoreCommit();
    IGitCoreLocalBranchSnapshot downstreamBranch = createGitCoreLocalBranch(downstreamCommit);
    IGitCoreLocalBranchSnapshot upstreamBranch = createGitCoreLocalBranch(upstreamCommit);

    PowerMockito.doReturn(Stream.empty()).when(gitCoreRepository).ancestorsOf(downstreamCommit);
    PowerMockito.doReturn(false).when(gitCoreRepository).isAncestor(upstreamCommit, downstreamCommit);

    // when
    Option<IGitCoreCommit> result = invokeDeriveUpstreamAwareForkPoint(downstreamBranch, upstreamBranch);

    // then
    Assert.assertTrue(result.isEmpty());
  }

  @Test
  @SneakyThrows
  public void upstreamAgnosticForkPointIsMissingAndUpstreamIsAncestorOfDownstream() {
    // given
    IGitCoreCommit upstreamCommit = createGitCoreCommit();
    IGitCoreCommit downstreamCommit = createGitCoreCommit();
    IGitCoreLocalBranchSnapshot upstreamBranch = createGitCoreLocalBranch(upstreamCommit);
    IGitCoreLocalBranchSnapshot downstreamBranch = createGitCoreLocalBranch(downstreamCommit);

    PowerMockito.doReturn(Stream.empty()).when(gitCoreRepository).ancestorsOf(downstreamCommit);
    PowerMockito.doReturn(true).when(gitCoreRepository).isAncestor(upstreamCommit, downstreamCommit);

    // when
    Option<IGitCoreCommit> result = invokeDeriveUpstreamAwareForkPoint(downstreamBranch, upstreamBranch);

    // then
    Assert.assertTrue(result.isDefined());
    Assert.assertEquals(upstreamCommit, result.get());
  }

  @Test
  @SneakyThrows
  public void upstreamIsNotAncestorOfForkPointAndUpstreamIsAncestorOfDownstream() {
    // given
    IGitCoreCommit forkPointCommit = createGitCoreCommit();
    IGitCoreCommit upstreamCommit = createGitCoreCommit();
    IGitCoreCommit downstreamCommit = createGitCoreCommit();
    IGitCoreLocalBranchSnapshot upstreamBranch = createGitCoreLocalBranch(upstreamCommit);
    IGitCoreLocalBranchSnapshot downstreamBranch = createGitCoreLocalBranch(downstreamCommit);

    PowerMockito.doReturn(Stream.of(forkPointCommit)).when(gitCoreRepository).ancestorsOf(downstreamCommit);
    PowerMockito.doReturn(false).when(gitCoreRepository).isAncestor(upstreamCommit, forkPointCommit);
    PowerMockito.doReturn(true).when(gitCoreRepository).isAncestor(upstreamCommit, downstreamCommit);

    // when
    Option<IGitCoreCommit> result = invokeDeriveUpstreamAwareForkPoint(downstreamBranch, upstreamBranch);

    // then
    Assert.assertTrue(result.isDefined());
    Assert.assertEquals(upstreamCommit, result.get());
  }
}
