package com.virtuslab.gitmachete.backend.integration;

import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_FOR_OVERRIDDEN_FORK_POINT;
import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_FOR_YELLOW_EDGES;
import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_WITH_SINGLE_REMOTE;
import static com.virtuslab.gitmachete.testcommon.TestFileUtils.cleanUpDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.virtuslab.branchlayout.impl.readwrite.BranchLayoutReader;
import com.virtuslab.gitcore.api.IGitCoreRepositoryFactory;
import com.virtuslab.gitcore.impl.jgit.GitCoreRepositoryFactory;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import java.io.FileInputStream;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.BeforeClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutReader;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositoryCache;
import com.virtuslab.gitmachete.testcommon.GitRepositoryBackedIntegrationTestSuiteInitializer;
import org.powermock.api.mockito.PowerMockito;

public class ParentInferenceIntegrationTestSuite {

  private final String forBranch;
  private final String expectedParent;

  @BeforeAll
  public static void setUpStatic() {
    PowerMockito.mockStatic(ApplicationManager.class);
  }

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
    when(application.getService(any<IGitCoreRepositoryFactory>())).thenReturn(gitCoreRepositoryFactory);
    when(ApplicationManager.getApplication()).toReturn(application);

    val gitMacheteRepositoryCache = new GitMacheteRepositoryCache();
    val gitMacheteRepository = gitMacheteRepositoryCache.getInstance(it.rootDirectoryPath, it.mainGitDirectoryPath,
        it.worktreeGitDirectoryPath);
    val gitMacheteRepositorySnapshot = gitMacheteRepository.createSnapshotForLayout(branchLayout);

    val managedBranchNames = gitMacheteRepositorySnapshot.getManagedBranches().map(IManagedBranchSnapshot::getName).toSet();
    val result = gitMacheteRepository.inferParentForLocalBranch(managedBranchNames, forBranch);
    assertNotNull(result);
    assertEquals(expectedParent, result.getName());

    cleanUpDir(it.parentDirectoryPath);
  }

}
