package com.virtuslab.gitmachete.backend.root;

import static com.virtuslab.gitmachete.backend.root.TestUtils.TestGitCoreRepository;
import static com.virtuslab.gitmachete.backend.root.TestUtils.TestGitCoreRepositoryFactory;
import static com.virtuslab.gitmachete.backend.root.TestUtils.getCommit;
import static com.virtuslab.gitmachete.backend.root.TestUtils.getGitCoreLocalBranch;

import java.util.Optional;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.BaseGitCoreCommit;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;

public class GitMacheteRepositoryBuilder_deduceForkPointTest {

  private static final GitMacheteRepositoryBuilder gitMacheteRepositoryBuilder = PowerMockito
      .mock(GitMacheteRepositoryBuilder.class);

  private static final TestGitCoreRepositoryFactory repositoryFactory = new TestGitCoreRepositoryFactory();
  private static final TestGitCoreRepository repository = repositoryFactory.getInstance();

  @BeforeClass
  public static void init() {
    Whitebox.setInternalState(gitMacheteRepositoryBuilder, "gitCoreRepositoryFactory", repositoryFactory);
  }

  private Optional<BaseGitCoreCommit> invokeDeduceForkPoint(
      IGitCoreLocalBranch coreLocalBranch,
      IGitCoreLocalBranch parentCoreLocalBranch) throws Exception {
    Class<Object> ancestorityCacheClass = Whitebox.getInnerClassType(GitMacheteRepositoryBuilder.class,
        "AncestorityCache");
    Object ancestorityCheckerInstance = ancestorityCacheClass.getConstructor(IGitCoreRepository.class)
        .newInstance(repository);
    return Whitebox.invokeMethod(PowerMockito.mock(GitMacheteRepositoryBuilder.class),
        "deduceForkPoint", ancestorityCheckerInstance, coreLocalBranch, parentCoreLocalBranch);
  }

  @Test(expected = GitMacheteException.class)
  public void deduceForkPoint_deriveForkPointThrows() throws Exception {
    // given
    BaseGitCoreCommit pointedCommit = getCommit(null);
    IGitCoreLocalBranch coreLocalBranch = getGitCoreLocalBranch(pointedCommit);
    PowerMockito.doThrow(new GitCoreException()).when(coreLocalBranch).deriveForkPoint();
    IGitCoreLocalBranch parentCoreLocalBranch = getGitCoreLocalBranch(pointedCommit);

    // when
    Optional<BaseGitCoreCommit> deducedForkPoint = invokeDeduceForkPoint(coreLocalBranch, parentCoreLocalBranch);

    // then exception is thrown
  }

  @Test
  public void deduceForkPoint_derivedForkPointIsMissingAndParentIsNotAncestorOfChild() throws Exception {
    // given
    BaseGitCoreCommit pointedCommit = getCommit(null);
    IGitCoreLocalBranch coreLocalBranch = getGitCoreLocalBranch(pointedCommit);
    PowerMockito.doReturn(Optional.empty()).when(coreLocalBranch).deriveForkPoint();
    BaseGitCoreCommit parentPointedCommit = getCommit(null);
    IGitCoreLocalBranch parentCoreLocalBranch = getGitCoreLocalBranch(parentPointedCommit);

    // when
    Optional<BaseGitCoreCommit> deducedForkPoint = invokeDeduceForkPoint(coreLocalBranch, parentCoreLocalBranch);

    // then
    Assert.assertTrue(deducedForkPoint.isEmpty());
  }

  @Test
  public void deduceForkPoint_derivedForkPointIsMissingEmptyAndParentIsAncestorOfChild() throws Exception {
    // given
    BaseGitCoreCommit parentPointedCommit = getCommit(null);
    IGitCoreLocalBranch parentCoreLocalBranch = getGitCoreLocalBranch(parentPointedCommit);
    BaseGitCoreCommit pointedCommit = getCommit(parentPointedCommit);
    IGitCoreLocalBranch coreLocalBranch = getGitCoreLocalBranch(pointedCommit);
    PowerMockito.doReturn(Optional.empty()).when(coreLocalBranch).deriveForkPoint();

    // when
    Optional<BaseGitCoreCommit> deducedForkPoint = invokeDeduceForkPoint(coreLocalBranch, parentCoreLocalBranch);

    // then
    Assert.assertTrue(deducedForkPoint.isPresent());
    Assert.assertEquals(parentPointedCommit, deducedForkPoint.get());
  }

  @Test
  public void deduceForkPoint_parentIsNotAncestorOfForkPointAndParentIsAncestorOfChild() throws Exception {
    // given
    BaseGitCoreCommit derivedForkPoint = getCommit(null);
    BaseGitCoreCommit parentPointedCommit = getCommit(derivedForkPoint);
    BaseGitCoreCommit childPointedCommit = getCommit(parentPointedCommit);

    IGitCoreLocalBranch coreLocalBranch = getGitCoreLocalBranch(childPointedCommit);
    PowerMockito.doReturn(Optional.of(derivedForkPoint)).when(coreLocalBranch).deriveForkPoint();
    IGitCoreLocalBranch parentCoreLocalBranch = getGitCoreLocalBranch(parentPointedCommit);

    // when
    Optional<BaseGitCoreCommit> deducedForkPoint = invokeDeduceForkPoint(coreLocalBranch, parentCoreLocalBranch);

    // then
    Assert.assertTrue(deducedForkPoint.isPresent());
    Assert.assertEquals(parentPointedCommit, deducedForkPoint.get());
  }

  @Test
  public void deduceForkPoint_parentIsNotAncestorOfForkPointAndParentIsNotAncestorOfChild() throws Exception {
    // given
    BaseGitCoreCommit derivedForkPoint = getCommit(null);
    BaseGitCoreCommit parentPointedCommit = getCommit(null);
    BaseGitCoreCommit childPointedCommit = getCommit(null);

    IGitCoreLocalBranch coreLocalBranch = getGitCoreLocalBranch(childPointedCommit);
    PowerMockito.doReturn(Optional.of(derivedForkPoint)).when(coreLocalBranch).deriveForkPoint();
    IGitCoreLocalBranch parentCoreLocalBranch = getGitCoreLocalBranch(parentPointedCommit);

    // when
    Optional<BaseGitCoreCommit> deducedForkPoint = invokeDeduceForkPoint(coreLocalBranch, parentCoreLocalBranch);

    // then
    Assert.assertTrue(deducedForkPoint.isPresent());
    Assert.assertEquals(derivedForkPoint, deducedForkPoint.get());
  }

  @Test
  public void deduceForkPoint_parentIsAncestorOfForkPointAndParentIsNotAncestorOfChild() throws Exception {
    // given
    BaseGitCoreCommit parentPointedCommit = getCommit(null);
    BaseGitCoreCommit derivedForkPoint = getCommit(parentPointedCommit);
    BaseGitCoreCommit childPointedCommit = getCommit(null);

    IGitCoreLocalBranch coreLocalBranch = getGitCoreLocalBranch(childPointedCommit);
    PowerMockito.doReturn(Optional.of(derivedForkPoint)).when(coreLocalBranch).deriveForkPoint();
    IGitCoreLocalBranch parentCoreLocalBranch = getGitCoreLocalBranch(parentPointedCommit);

    // when
    Optional<BaseGitCoreCommit> deducedForkPoint = invokeDeduceForkPoint(coreLocalBranch, parentCoreLocalBranch);

    // then
    Assert.assertTrue(deducedForkPoint.isPresent());
    Assert.assertEquals(derivedForkPoint, deducedForkPoint.get());
  }

  @Test
  public void deduceForkPoint_parentIsAncestorOfForkPointAndParentIsAncestorOfChild() throws Exception {
    // given
    BaseGitCoreCommit parentPointedCommit = getCommit(null);
    BaseGitCoreCommit derivedForkPoint = getCommit(parentPointedCommit);
    BaseGitCoreCommit childPointedCommit = getCommit(parentPointedCommit);

    IGitCoreLocalBranch coreLocalBranch = getGitCoreLocalBranch(childPointedCommit);
    PowerMockito.doReturn(Optional.of(derivedForkPoint)).when(coreLocalBranch).deriveForkPoint();
    IGitCoreLocalBranch parentCoreLocalBranch = getGitCoreLocalBranch(parentPointedCommit);

    // when
    Optional<BaseGitCoreCommit> deducedForkPoint = invokeDeduceForkPoint(coreLocalBranch, parentCoreLocalBranch);

    // then
    Assert.assertTrue(deducedForkPoint.isPresent());
    Assert.assertEquals(derivedForkPoint, deducedForkPoint.get());
  }
}
