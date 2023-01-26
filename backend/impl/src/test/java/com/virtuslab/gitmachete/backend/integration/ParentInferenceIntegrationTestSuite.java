package com.virtuslab.gitmachete.backend.integration;

import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_FOR_OVERRIDDEN_FORK_POINT;
import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_FOR_YELLOW_EDGES;
import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_WITH_SINGLE_REMOTE;
import static com.virtuslab.gitmachete.testcommon.TestFileUtils.cleanUpDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;

public class ParentInferenceIntegrationTestSuite extends BaseIntegrationTestSuite {

  public static String[][] getTestData() {
    return new String[][]{
        // script name, for branch, expected parent
        {SETUP_FOR_OVERRIDDEN_FORK_POINT, "allow-ownership-link", "develop"},
        {SETUP_FOR_YELLOW_EDGES, "allow-ownership-link", "develop"},
        {SETUP_FOR_YELLOW_EDGES, "drop-constraint", "call-ws"},
        {SETUP_WITH_SINGLE_REMOTE, "drop-constraint", "call-ws"},
    };
  }

  @ParameterizedTest
  @MethodSource("getTestData")
  @SneakyThrows
  public void parentIsCorrectlyInferred(String scriptName, String forBranch, String expectedParent) {
    setUp(scriptName);

    val gitMacheteRepositorySnapshot = gitMacheteRepository.createSnapshotForLayout(branchLayout);

    val managedBranchNames = gitMacheteRepositorySnapshot.getManagedBranches().map(IManagedBranchSnapshot::getName).toSet();
    val result = gitMacheteRepository.inferParentForLocalBranch(managedBranchNames, forBranch);
    assertNotNull(result);
    assertEquals(expectedParent, result.getName());

    // Deliberately done in the test and in not an @After method, so that the directory is retained in case of test failure.
    cleanUpDir(it.parentDirectoryPath);
  }

}
