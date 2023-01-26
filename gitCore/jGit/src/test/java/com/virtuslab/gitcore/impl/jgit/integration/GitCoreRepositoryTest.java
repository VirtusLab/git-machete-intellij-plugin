package com.virtuslab.gitcore.impl.jgit.integration;

import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_WITH_SINGLE_REMOTE;
import static org.junit.jupiter.api.Assertions.assertNull;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.virtuslab.gitcore.impl.jgit.GitCoreRepository;
import com.virtuslab.gitmachete.testcommon.GitRepositoryBackedIntegrationTestSuiteInitializer;

@ExtendWith(MockitoExtension.class)
public class GitCoreRepositoryTest {

  // See https://github.com/VirtusLab/git-machete-intellij-plugin/issues/1029 for the origin of this test
  @Test
  public void shouldContainExceptionsInsideOptionReturningMethods() throws Exception {
    val it = new GitRepositoryBackedIntegrationTestSuiteInitializer(SETUP_WITH_SINGLE_REMOTE);

    val gitCoreRepository = new GitCoreRepository(it.rootDirectoryPath, it.mainGitDirectoryPath, it.worktreeGitDirectoryPath);
    // Let's check against a non-existent commit.
    // No exception should be thrown, just a null returned.
    assertNull(gitCoreRepository.parseRevision("0".repeat(40)));
  }
}
