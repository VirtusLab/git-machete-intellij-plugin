package com.virtuslab.gitmachete.backend.integration;

import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_FOR_OVERRIDDEN_FORK_POINT;
import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_FOR_YELLOW_EDGES;
import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_WITH_SINGLE_REMOTE;
import static com.virtuslab.gitmachete.testcommon.TestFileUtils.cleanUpDir;
import static org.junit.runners.Parameterized.Parameters;

import lombok.SneakyThrows;
import lombok.val;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutReader;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositoryCache;
import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedIntegrationTestSuite;

@RunWith(Parameterized.class)
public class ParentInferenceIntegrationTestSuite extends BaseGitRepositoryBackedIntegrationTestSuite {

  private final GitMacheteRepositoryCache gitMacheteRepositoryCache = new GitMacheteRepositoryCache();
  private final IGitMacheteRepository gitMacheteRepository;
  private final IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot;

  private final String forBranch;
  private final String expectedParent;

  @Parameters(name = "{0}: inferred parent of {1} should be {2} (#{index})")
  public static String[][] getTestData() {
    return new String[][]{
        // script name, for branch, expected parent
        {SETUP_FOR_OVERRIDDEN_FORK_POINT, "allow-ownership-link", "develop"},
        {SETUP_FOR_YELLOW_EDGES, "allow-ownership-link", "develop"},
        {SETUP_FOR_YELLOW_EDGES, "drop-constraint-02", "call-ws"},
        {SETUP_WITH_SINGLE_REMOTE, "drop-constraint-05", "call-ws"},
    };
  }

  @SneakyThrows
  public ParentInferenceIntegrationTestSuite(String scriptName, String forBranch, String expectedParent) {
    super(scriptName);
    this.forBranch = forBranch;
    this.expectedParent = expectedParent;

    val branchLayoutReader = RuntimeBinding.instantiateSoleImplementingClass(IBranchLayoutReader.class);
    val branchLayout = branchLayoutReader.read(mainGitDirectoryPath.resolve("machete"));

    gitMacheteRepository = gitMacheteRepositoryCache.getInstance(rootDirectoryPath, mainGitDirectoryPath,
        worktreeGitDirectoryPath);
    gitMacheteRepositorySnapshot = gitMacheteRepository.createSnapshotForLayout(branchLayout);
  }

  @Test
  @SneakyThrows
  public void parentIsCorrectlyInferred() {
    val managedBranchNames = gitMacheteRepositorySnapshot.getManagedBranches().map(IManagedBranchSnapshot::getName).toSet();
    val result = gitMacheteRepository.inferParentForLocalBranch(managedBranchNames, forBranch);
    Assert.assertNotNull(result);
    Assert.assertEquals(expectedParent, result.getName());
  }

  @Rule(order = Integer.MIN_VALUE)
  public final TestWatcher cleanUpAfterSuccessfulTest = new TestWatcher() {
    @Override
    protected void succeeded(Description description) {
      cleanUpDir(parentDirectoryPath);
    }

    // After a failed test, keep the parent directory intact for further manual inspection.
  };
}
