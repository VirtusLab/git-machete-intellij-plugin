package com.virtuslab.gitmachete.backend.integration;

import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_FOR_OVERRIDDEN_FORK_POINT;
import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_FOR_YELLOW_EDGES;
import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_WITH_SINGLE_REMOTE;
import static com.virtuslab.gitmachete.testcommon.TestFileUtils.cleanUpDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutReader;
import com.virtuslab.gitcore.impl.jgit.GitCoreRepositoryFactory;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositoryCache;
import com.virtuslab.gitmachete.testcommon.GitRepositoryBackedIntegrationTestSuiteInitializer;

public class ParentInferenceIntegrationTestSuite {

  public static String[][] getTestData() {
    return new String[][]{
        // script name, for branch, expected parent
        {SETUP_FOR_OVERRIDDEN_FORK_POINT, "allow-ownership-link", "develop"},
        {SETUP_FOR_YELLOW_EDGES, "allow-ownership-link", "develop"},
        {SETUP_FOR_YELLOW_EDGES, "drop-constraint", "call-ws"},
        {SETUP_WITH_SINGLE_REMOTE, "drop-constraint", "call-ws"},
    };
  }

  @SneakyThrows
  @ParameterizedTest
  @MethodSource("getTestData")
  public void parentIsCorrectlyInferred(String scriptName, String forBranch, String expectedParent) {
    val it = new GitRepositoryBackedIntegrationTestSuiteInitializer(scriptName);

    // This setup needs to happen BEFORE GitMacheteRepositoryCache is created
    val application = mock(Application.class);
    val gitCoreRepositoryFactory = new GitCoreRepositoryFactory();
    when(application.getService(any())).thenReturn(gitCoreRepositoryFactory);
    when(ApplicationManager.getApplication()).thenReturn(application);

    val gitMacheteRepositoryCache = new GitMacheteRepositoryCache();
    val gitMacheteRepository = gitMacheteRepositoryCache.getInstance(it.rootDirectoryPath, it.mainGitDirectoryPath,
        it.worktreeGitDirectoryPath);
    val branchLayoutReader = ApplicationManager.getApplication().getService(IBranchLayoutReader.class);
    val branchLayout = branchLayoutReader.read(new FileInputStream(it.mainGitDirectoryPath.resolve("machete").toFile()));
    val gitMacheteRepositorySnapshot = gitMacheteRepository.createSnapshotForLayout(branchLayout);

    val managedBranchNames = gitMacheteRepositorySnapshot.getManagedBranches().map(IManagedBranchSnapshot::getName).toSet();
    val result = gitMacheteRepository.inferParentForLocalBranch(managedBranchNames, forBranch);
    assertNotNull(result);
    assertEquals(expectedParent, result.getName());

    cleanUpDir(it.parentDirectoryPath);
  }

}
