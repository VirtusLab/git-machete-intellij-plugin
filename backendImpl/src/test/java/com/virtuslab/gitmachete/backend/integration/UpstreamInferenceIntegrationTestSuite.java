package com.virtuslab.gitmachete.backend.integration;

import static com.virtuslab.gitmachete.backend.integration.IntegrationTestUtils.ensureCliVersionIs;
import static org.junit.runners.Parameterized.Parameters;

import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.BeforeClass;
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
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositoryCache;
import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedIntegrationTestSuite;

@RunWith(Parameterized.class)
public class UpstreamInferenceIntegrationTestSuite extends BaseGitRepositoryBackedIntegrationTestSuite {

  private final GitMacheteRepositoryCache gitMacheteRepositoryCache = new GitMacheteRepositoryCache();
  private final IGitMacheteRepository gitMacheteRepository;
  private final IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot;

  private final String forBranch;
  private final String expectedUpstream;

  @BeforeClass
  public static void ensureExpectedCliVersion() {
    ensureCliVersionIs("2.14.0");
  }

  @Parameters(name = "{0}: inferred upstream of {1} should be {2} (#{index})")
  public static String[][] getTestData() {
    return new String[][]{
        // script name, for branch, expected upstream
        {SETUP_FOR_OVERRIDDEN_FORK_POINT, "allow-ownership-link", "develop"},
        {SETUP_FOR_YELLOW_EDGES, "allow-ownership-link", "develop"},
        {SETUP_FOR_YELLOW_EDGES, "drop-constraint", "call-ws"},
        {SETUP_WITH_SINGLE_REMOTE, "drop-constraint", "call-ws"},
    };
  }

  @SneakyThrows
  public UpstreamInferenceIntegrationTestSuite(String scriptName, String forBranch, String expectedUpstream) {
    super(scriptName);
    this.forBranch = forBranch;
    this.expectedUpstream = expectedUpstream;

    var branchLayoutReader = RuntimeBinding.instantiateSoleImplementingClass(IBranchLayoutReader.class);
    var branchLayout = branchLayoutReader.read(repositoryGitDir.resolve("machete"));

    gitMacheteRepository = gitMacheteRepositoryCache.getInstance(repositoryMainDir, repositoryGitDir);
    gitMacheteRepositorySnapshot = gitMacheteRepository.createSnapshotForLayout(branchLayout);
  }

  @Test
  @SneakyThrows
  public void upstreamIsCorrectlyInferred() {
    var managedBranchNames = gitMacheteRepositorySnapshot.getManagedBranches().map(b -> b.getName()).toSet();
    var result = gitMacheteRepository.inferUpstreamForLocalBranch(managedBranchNames, forBranch);
    Assert.assertTrue(result.isDefined());
    Assert.assertEquals(expectedUpstream, result.get());
  }

  @Rule(order = Integer.MIN_VALUE)
  public final TestWatcher cleanUpAfterSuccessfulTest = new TestWatcher() {
    @Override
    protected void succeeded(Description description) {
      cleanUpParentDir();
    }

    // After a failed test, keep the parent directory intact for further manual inspection.
  };
}
