package com.virtuslab.gitcore.impl.jgit.integration;

import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_WITH_SINGLE_REMOTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;

import com.intellij.openapi.application.ApplicationManager;
import lombok.val;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutReader;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositoryCache;
import com.virtuslab.gitmachete.testcommon.GitRepositoryBackedIntegrationTestSuiteInitializer;

@ExtendWith(MockitoExtension.class)
public class GitCoreRepositoryTest {

  @Test
  public void shouldContainExceptionsInsideOptionReturningMethods() throws Exception {
    val revWalk = mock(RevWalk.class);
    val it = new GitRepositoryBackedIntegrationTestSuiteInitializer(SETUP_WITH_SINGLE_REMOTE);

    val branchLayoutReader = ApplicationManager.getApplication().getService(IBranchLayoutReader.class);
    val branchLayout = branchLayoutReader.read(new FileInputStream(it.mainGitDirectoryPath.resolve("machete").toFile()));
    val gitMacheteRepositoryCache = new GitMacheteRepositoryCache();

    when(revWalk.parseCommit(any())).thenThrow(new Exception("Mock"));

    val gitMacheteRepository = gitMacheteRepositoryCache.getInstance(it.rootDirectoryPath, it.mainGitDirectoryPath,
        it.worktreeGitDirectoryPath);
  }
}
