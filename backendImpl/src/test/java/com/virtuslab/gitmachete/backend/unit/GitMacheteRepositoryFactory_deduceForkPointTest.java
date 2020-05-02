package com.virtuslab.gitmachete.backend.unit;

import static com.virtuslab.gitmachete.backend.unit.TestUtils.TestGitCoreRepository;
import static com.virtuslab.gitmachete.backend.unit.TestUtils.TestGitCoreRepositoryFactory;
import static com.virtuslab.gitmachete.backend.unit.TestUtils.getCommit;

import io.vavr.control.Option;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.BaseGitCoreCommit;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositoryFactory;

public class GitMacheteRepositoryFactory_deduceForkPointTest {

  private static final GitMacheteRepositoryFactory gitMacheteRepositoryFactory = PowerMockito
      .mock(GitMacheteRepositoryFactory.class);

  private static final TestGitCoreRepositoryFactory gitCoreRepositoryFactory = new TestGitCoreRepositoryFactory();
  private static final TestGitCoreRepository gitCoreRepository = gitCoreRepositoryFactory.getInstance();

  @BeforeClass
  public static void init() {
    Whitebox.setInternalState(gitMacheteRepositoryFactory, "gitCoreRepositoryFactory", gitCoreRepositoryFactory);
  }

  private Option<BaseGitCoreCommit> invokeDeduceForkPoint(
      IGitCoreLocalBranch coreLocalBranch,
      IGitCoreLocalBranch parentCoreLocalBranch) throws Exception {
    return Whitebox.invokeMethod(PowerMockito.mock(GitMacheteRepositoryFactory.class),
        "deduceForkPoint", gitCoreRepository, coreLocalBranch, parentCoreLocalBranch);
  }

  @Test(expected = GitMacheteException.class)
  public void deduceForkPoint_deriveForkPointThrows() throws Exception {
    // given
    BaseGitCoreCommit pointedCommit = getCommit(null);
    IGitCoreLocalBranch coreLocalBranch = TestUtils.getGitCoreLocalBranch(pointedCommit);
    PowerMockito.doThrow(new GitCoreException()).when(coreLocalBranch).deriveForkPoint();
    IGitCoreLocalBranch parentCoreLocalBranch = TestUtils.getGitCoreLocalBranch(pointedCommit);

    // when
    Option<BaseGitCoreCommit> deducedForkPoint = invokeDeduceForkPoint(coreLocalBranch, parentCoreLocalBranch);

    // then exception is thrown
  }

  @Test
  public void deduceForkPoint_derivedForkPointIsMissingAndParentIsNotAncestorOfChild() throws Exception {
    // given
    BaseGitCoreCommit pointedCommit = getCommit(null);
    IGitCoreLocalBranch coreLocalBranch = TestUtils.getGitCoreLocalBranch(pointedCommit);
    PowerMockito.doReturn(Option.none()).when(coreLocalBranch).deriveForkPoint();
    BaseGitCoreCommit parentPointedCommit = getCommit(null);
    IGitCoreLocalBranch parentCoreLocalBranch = TestUtils.getGitCoreLocalBranch(parentPointedCommit);

    // when
    Option<BaseGitCoreCommit> deducedForkPoint = invokeDeduceForkPoint(coreLocalBranch, parentCoreLocalBranch);

    // then
    Assert.assertTrue(deducedForkPoint.isEmpty());
  }

  @Test
  public void deduceForkPoint_derivedForkPointIsMissingEmptyAndParentIsAncestorOfChild() throws Exception {
    // given
    BaseGitCoreCommit parentPointedCommit = getCommit(null);
    IGitCoreLocalBranch parentCoreLocalBranch = TestUtils.getGitCoreLocalBranch(parentPointedCommit);
    BaseGitCoreCommit pointedCommit = getCommit(parentPointedCommit);
    IGitCoreLocalBranch coreLocalBranch = TestUtils.getGitCoreLocalBranch(pointedCommit);
    PowerMockito.doReturn(Option.none()).when(coreLocalBranch).deriveForkPoint();

    // when
    Option<BaseGitCoreCommit> deducedForkPoint = invokeDeduceForkPoint(coreLocalBranch, parentCoreLocalBranch);

    // then
    Assert.assertTrue(deducedForkPoint.isDefined());
    Assert.assertEquals(parentPointedCommit, deducedForkPoint.get());
  }

  @Test
  public void deduceForkPoint_parentIsNotAncestorOfForkPointAndParentIsAncestorOfChild() throws Exception {
    // given
    BaseGitCoreCommit derivedForkPoint = getCommit(null);
    BaseGitCoreCommit parentPointedCommit = getCommit(derivedForkPoint);
    BaseGitCoreCommit childPointedCommit = getCommit(parentPointedCommit);

    IGitCoreLocalBranch coreLocalBranch = TestUtils.getGitCoreLocalBranch(childPointedCommit);
    PowerMockito.doReturn(Option.of(derivedForkPoint)).when(coreLocalBranch).deriveForkPoint();
    IGitCoreLocalBranch parentCoreLocalBranch = TestUtils.getGitCoreLocalBranch(parentPointedCommit);

    // when
    Option<BaseGitCoreCommit> deducedForkPoint = invokeDeduceForkPoint(coreLocalBranch, parentCoreLocalBranch);

    // then
    Assert.assertTrue(deducedForkPoint.isDefined());
    Assert.assertEquals(parentPointedCommit, deducedForkPoint.get());
  }

  @Test
  public void deduceForkPoint_parentIsNotAncestorOfForkPointAndParentIsNotAncestorOfChild() throws Exception {
    // given
    BaseGitCoreCommit derivedForkPoint = getCommit(null);
    BaseGitCoreCommit parentPointedCommit = getCommit(null);
    BaseGitCoreCommit childPointedCommit = getCommit(null);

    IGitCoreLocalBranch coreLocalBranch = TestUtils.getGitCoreLocalBranch(childPointedCommit);
    PowerMockito.doReturn(Option.of(derivedForkPoint)).when(coreLocalBranch).deriveForkPoint();
    IGitCoreLocalBranch parentCoreLocalBranch = TestUtils.getGitCoreLocalBranch(parentPointedCommit);

    // when
    Option<BaseGitCoreCommit> deducedForkPoint = invokeDeduceForkPoint(coreLocalBranch, parentCoreLocalBranch);

    // then
    Assert.assertTrue(deducedForkPoint.isDefined());
    Assert.assertEquals(derivedForkPoint, deducedForkPoint.get());
  }

  @Test
  public void deduceForkPoint_parentIsAncestorOfForkPointAndParentIsNotAncestorOfChild() throws Exception {
    // given
    BaseGitCoreCommit parentPointedCommit = getCommit(null);
    BaseGitCoreCommit derivedForkPoint = getCommit(parentPointedCommit);
    BaseGitCoreCommit childPointedCommit = getCommit(null);

    IGitCoreLocalBranch coreLocalBranch = TestUtils.getGitCoreLocalBranch(childPointedCommit);
    PowerMockito.doReturn(Option.of(derivedForkPoint)).when(coreLocalBranch).deriveForkPoint();
    IGitCoreLocalBranch parentCoreLocalBranch = TestUtils.getGitCoreLocalBranch(parentPointedCommit);

    // when
    Option<BaseGitCoreCommit> deducedForkPoint = invokeDeduceForkPoint(coreLocalBranch, parentCoreLocalBranch);

    // then
    Assert.assertTrue(deducedForkPoint.isDefined());
    Assert.assertEquals(derivedForkPoint, deducedForkPoint.get());
  }

  @Test
  public void deduceForkPoint_parentIsAncestorOfForkPointAndParentIsAncestorOfChild() throws Exception {
    // given
    BaseGitCoreCommit parentPointedCommit = getCommit(null);
    BaseGitCoreCommit derivedForkPoint = getCommit(parentPointedCommit);
    BaseGitCoreCommit childPointedCommit = getCommit(parentPointedCommit);

    IGitCoreLocalBranch coreLocalBranch = TestUtils.getGitCoreLocalBranch(childPointedCommit);
    PowerMockito.doReturn(Option.of(derivedForkPoint)).when(coreLocalBranch).deriveForkPoint();
    IGitCoreLocalBranch parentCoreLocalBranch = TestUtils.getGitCoreLocalBranch(parentPointedCommit);

    // when
    Option<BaseGitCoreCommit> deducedForkPoint = invokeDeduceForkPoint(coreLocalBranch, parentCoreLocalBranch);

    // then
    Assert.assertTrue(deducedForkPoint.isDefined());
    Assert.assertEquals(derivedForkPoint, deducedForkPoint.get());
  }
}
