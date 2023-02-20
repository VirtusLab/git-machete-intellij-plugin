package com.virtuslab.gitcore.impl.jgit.integration;

import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_WITH_SINGLE_REMOTE;
import static com.virtuslab.gitmachete.testcommon.TestFileUtils.cleanUpDir;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.simplify4u.slf4jmock.LoggerMock;
import org.slf4j.Logger;

import com.virtuslab.gitcore.impl.jgit.GitCoreRepository;
import com.virtuslab.gitmachete.testcommon.TestGitRepository;

public class GitCoreRepositoryTest {

  private TestGitRepository repo;
  private GitCoreRepository gitCoreRepository;

  @BeforeEach
  @SneakyThrows
  public void setUp() {
    repo = new TestGitRepository(SETUP_WITH_SINGLE_REMOTE);

    gitCoreRepository = new GitCoreRepository(repo.rootDirectoryPath, repo.mainGitDirectoryPath, repo.worktreeGitDirectoryPath);
  }

  // See https://github.com/VirtusLab/git-machete-intellij-plugin/issues/1029 for the origin of this test
  @Test
  @SneakyThrows
  public void shouldContainExceptionsInsideOptionReturningMethods() {
    // Let's check against a non-existent commit.
    // No exception should be thrown, just a null returned.
    assertNull(gitCoreRepository.parseRevision("0".repeat(40)));

    // Deliberately done in the test and in not an @After method, so that the directory is retained in case of test failure.
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
    // ending up in an user-visible, confusing error notification.
    // See the issue and PR #1304 for more details.
    verify(logger, never()).error(anyString(), any(Throwable.class));

    // Deliberately done in the test and in not an @After method, so that the directory is retained in case of test failure.
    cleanUpDir(repo.parentDirectoryPath);
  }
}
