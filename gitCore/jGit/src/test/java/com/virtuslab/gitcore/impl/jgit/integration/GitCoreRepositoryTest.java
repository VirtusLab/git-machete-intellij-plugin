package com.virtuslab.gitcore.impl.jgit.integration;

import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_WITH_SINGLE_REMOTE;
import static org.junit.jupiter.api.Assertions.assertNull;

import lombok.val;
import org.junit.jupiter.api.Test;

import com.virtuslab.gitcore.impl.jgit.GitCoreRepository;
import com.virtuslab.gitmachete.testcommon.TestGitRepository;

public class GitCoreRepositoryTest {

  // See https://github.com/VirtusLab/git-machete-intellij-plugin/issues/1029 for the origin of this test
  @Test
  public void shouldContainExceptionsInsideOptionReturningMethods() throws Exception {
    val repo = new TestGitRepository(SETUP_WITH_SINGLE_REMOTE);

    val gitCoreRepository = new GitCoreRepository(repo.rootDirectoryPath, repo.mainGitDirectoryPath,
        repo.worktreeGitDirectoryPath);
    // Let's check against a non-existent commit.
    // No exception should be thrown, just a null returned.
    assertNull(gitCoreRepository.parseRevision("0".repeat(40)));
  }
}
