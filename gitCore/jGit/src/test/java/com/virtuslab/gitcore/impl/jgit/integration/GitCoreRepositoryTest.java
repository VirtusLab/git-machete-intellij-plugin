package com.virtuslab.gitcore.impl.jgit.integration;

import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_WITH_SINGLE_REMOTE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.simplify4u.slf4jmock.LoggerMock;
import org.slf4j.Logger;

import com.virtuslab.gitcore.impl.jgit.GitCoreRepository;
import com.virtuslab.gitmachete.testcommon.TestGitRepository;

public class GitCoreRepositoryTest {

  private GitCoreRepository gitCoreRepository;

  @BeforeEach
  @SneakyThrows
  public void setUp() {
    val repo = new TestGitRepository(SETUP_WITH_SINGLE_REMOTE);

    gitCoreRepository = new GitCoreRepository(repo.rootDirectoryPath, repo.mainGitDirectoryPath, repo.worktreeGitDirectoryPath);
  }

  // See https://github.com/VirtusLab/git-machete-intellij-plugin/issues/1029 for the origin of this test
  @Test
  @SneakyThrows
  public void shouldContainExceptionsInsideOptionReturningMethods() {
    // Let's check against a non-existent commit.
    // No exception should be thrown, just a null returned.
    assertNull(gitCoreRepository.parseRevision("0".repeat(40)));
  }

  // See https://github.com/VirtusLab/git-machete-intellij-plugin/issues/1298 for the origin of this test
  @Test
  public void shouldNeverLeadToLogErrorCalledWithThrowable() {
    assertTrue(gitCoreRepository.isBranchPresent("refs/heads/develop"));

    Logger logger = mock(Logger.class);
    LoggerMock.setMock(org.eclipse.jgit.internal.storage.file.FileSnapshot.class, logger);

    assertFalse(gitCoreRepository.isBranchPresent("refs/heads/develop/something-else"));

    verify(logger, never()).error(anyString(), any(Throwable.class));
  }
}
