package com.virtuslab.gitcore.impl.jgit;

import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_WITH_SINGLE_REMOTE;
import static com.virtuslab.gitmachete.testcommon.TestFileUtils.cleanUpDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Objects;

import io.vavr.collection.List;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.simplify4u.slf4jmock.LoggerMock;
import org.slf4j.Logger;

import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitmachete.testcommon.TestGitRepository;

public class GitCoreRepositoryIntegrationTest {

  private TestGitRepository repo;
  private GitCoreRepository gitCoreRepository;

  @BeforeEach
  @SneakyThrows
  public void setUp() {
    repo = new TestGitRepository(SETUP_WITH_SINGLE_REMOTE);

    gitCoreRepository = new GitCoreRepository(repo.rootDirectoryPath);
  }

  // See https://github.com/VirtusLab/git-machete-intellij-plugin/issues/1029 for the origin of this test
  @Test
  @SneakyThrows
  public void shouldContainExceptionsInsideOptionReturningMethods() {
    // Let's check against a non-existent commit.
    // No exception should be thrown, just a null returned.
    assertNull(gitCoreRepository.parseRevision("0".repeat(40)));

    // Deliberately done in the test and not in an @AfterEach method, so that the directory is retained in case of test failure.
    cleanUpDir(repo.parentDirectoryPath);
  }

  // See https://github.com/VirtusLab/git-machete-intellij-plugin/issues/1298 for the origin of this test
  @Test
  public void shouldNeverLeadToLogErrorCalledWithThrowable() {
    assertTrue(gitCoreRepository.isBranchPresent("refs/heads/develop"));

    Logger logger = mock(Logger.class);
    LoggerMock.setMock(org.eclipse.jgit.internal.storage.file.FileSnapshot.class, logger);

    assertFalse(gitCoreRepository.isBranchPresent("refs/heads/develop/something-else"));

    // In test setup, a call to `LOG.error(String, Throwable)` doesn't crash the test
    // (and we aren't able to simply catch an exception to detect whether such a call took place).
    // In fact, with slf4j-simple (rather than slf4j-mock) on classpath, we'll just see stack trace printed out
    // (unless stderr is suppressed, which is the default when running tests under Gradle).
    // In IntelliJ, however, the situation is different, as IntelliJ provides an SLF4J implementation
    // which opens an error notification for each `LOG.error(String, Throwable)` (but not `LOG.error(String)`) call.
    // In this particular case, we want to avoid an `LOG.error(String, Throwable)` call in FileSnapshot c'tor
    // ending up in a user-visible, confusing error notification.
    // See the issue and PR #1304 for more details.
    verify(logger, never()).error(anyString(), any(Throwable.class));

    // Deliberately done in the test and not in an @AfterEach method, so that the directory is retained in case of test failure.
    cleanUpDir(repo.parentDirectoryPath);
  }

  @Test
  @SneakyThrows
  public void shouldCorrectlyHandleSyntacticallyInvalidGitRefs() {
    assertFalse(gitCoreRepository.isBranchPresent("refs/heads/./foo"));
    assertFalse(gitCoreRepository.isBranchPresent("refs/heads/."));
    assertNull(gitCoreRepository.parseRevision("refs/remotes/./foo"));
    assertNull(gitCoreRepository.parseRevision("refs/remotes/."));

    // Deliberately done in the test and not in an @AfterEach method, so that the directory is retained in case of test failure.
    cleanUpDir(repo.parentDirectoryPath);
  }

  @Test
  @SneakyThrows
  public void deriveCommitRange_descendantUntil_returnsCommitsBetween() {
    // `update-icons` was created at the tip of `allow-ownership-link` (after `1st round of fixes` was added
    // to the upstream of `allow-ownership-link`) and added one commit (`Use new icons`) on top.
    // After `git reset --keep HEAD~1` on `allow-ownership-link`, the local `allow-ownership-link` points back
    // to `Allow ownership links`, so `update-icons` IS a descendant of local `allow-ownership-link`.
    val updateIconsTip = Objects.requireNonNull(gitCoreRepository.parseRevision("refs/heads/update-icons"));
    val allowOwnershipLinkTip = Objects.requireNonNull(gitCoreRepository.parseRevision("refs/heads/allow-ownership-link"));

    val range = gitCoreRepository.deriveCommitRange(updateIconsTip, allowOwnershipLinkTip);
    val messages = range.map(IGitCoreCommit::getShortMessage);

    assertEquals(List.of("Use new icons", "1st round of fixes"), messages);

    cleanUpDir(repo.parentDirectoryPath);
  }

  // Regression test: with `RevSort.BOUNDARY` enabled, JGit yielded the merge-base as a phantom extra
  // commit when `fromInclusive` and `untilExclusive` had diverged (as for red edges in `git machete status`).
  // See https://github.com/VirtusLab/git-machete-intellij-plugin/pull/2277 for context.
  @Test
  @SneakyThrows
  public void deriveCommitRange_divergentUntil_doesNotIncludeMergeBase() {
    // After `git reset --keep HEAD~1` rolled local `allow-ownership-link` back from "1st round of fixes" to
    // "Allow ownership links", `allow-ownership-link` and `develop` have diverged:
    //   develop:              Root -> Develop commit -> Other develop commit
    //   allow-ownership-link: Root -> Develop commit -> Allow ownership links
    // Hence merge-base(allow-ownership-link, develop) = "Develop commit", which is reachable from `develop` and so
    // must NOT appear in `deriveCommitRange(allow-ownership-link, develop)`.
    val allowOwnershipLinkTip = Objects.requireNonNull(gitCoreRepository.parseRevision("refs/heads/allow-ownership-link"));
    val developTip = Objects.requireNonNull(gitCoreRepository.parseRevision("refs/heads/develop"));

    val range = gitCoreRepository.deriveCommitRange(allowOwnershipLinkTip, developTip);
    val messages = range.map(IGitCoreCommit::getShortMessage);

    assertEquals(List.of("Allow ownership links"), messages);

    cleanUpDir(repo.parentDirectoryPath);
  }
}
