package com.virtuslab.gitmachete.backend.integration;

import static com.virtuslab.gitmachete.backend.integration.IntegrationTestUtils.ensureExpectedCliVersion;
import static org.junit.runners.Parameterized.Parameters;

import io.vavr.collection.Set;
import io.vavr.control.Option;
import lombok.SneakyThrows;
import lombok.val;
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
import com.virtuslab.gitmachete.backend.api.ILocalBranchReference;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositoryCache;
import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedIntegrationTestSuite;

@RunWith(Parameterized.class)
public class ParentInferenceIntegrationTestSuite extends BaseGitRepositoryBackedIntegrationTestSuite {

  private final GitMacheteRepositoryCache gitMacheteRepositoryCache = new GitMacheteRepositoryCache();
  private final IGitMacheteRepository gitMacheteRepository;
  private final IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot;

  private final String forBranch;
  private final String expectedParent;

  @BeforeClass
  public static void doEnsureExpectedCliVersion() {
    ensureExpectedCliVersion();
  }

  @Parameters(name = "{0}: inferred parent of {1} should be {2} (#{index})")
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
  public ParentInferenceIntegrationTestSuite(String scriptName, String forBranch, String expectedParent) {
    super(scriptName);
    this.forBranch = forBranch;
    this.expectedParent = expectedParent;

    val branchLayoutReader = RuntimeBinding.instantiateSoleImplementingClass(IBranchLayoutReader.class);
    val branchLayout = branchLayoutReader.read(repositoryGitDir.resolve("machete"));

    gitMacheteRepository = gitMacheteRepositoryCache.getInstance(repositoryMainDir, repositoryGitDir);
    gitMacheteRepositorySnapshot = gitMacheteRepository.createSnapshotForLayout(branchLayout);
  }

  @Test
  @SneakyThrows
  public void parentIsCorrectlyInferred() {
    Set<String> managedBranchNames = gitMacheteRepositorySnapshot.getManagedBranches().map(b -> b.getName()).toSet();
    Option<ILocalBranchReference> result = gitMacheteRepository.inferParentForLocalBranch(managedBranchNames, forBranch);
    Assert.assertTrue(result.isDefined());
    Assert.assertEquals(expectedParent, result.get().getName());
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
