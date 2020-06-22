package com.virtuslab.gitmachete.backend.integration;

import static org.junit.runners.Parameterized.Parameters;

import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.manager.IBranchLayoutReader;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositoryFactory;
import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedTestSuite;

@RunWith(Parameterized.class)
public class GitMacheteUpstreamInferenceTestSuite extends BaseGitRepositoryBackedTestSuite {

  private final GitMacheteRepositoryFactory gitMacheteRepositoryFactory = new GitMacheteRepositoryFactory();
  private final IBranchLayoutReader branchLayoutReader = RuntimeBinding
      .instantiateSoleImplementingClass(IBranchLayoutReader.class);
  private final IGitMacheteRepository gitMacheteRepository;
  private final String forBranch;
  private final String expectedUpstream;

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
  public GitMacheteUpstreamInferenceTestSuite(String scriptName, String forBranch, String expectedUpstream) {
    super(scriptName);
    this.forBranch = forBranch;
    this.expectedUpstream = expectedUpstream;

    IBranchLayout branchLayout = branchLayoutReader.read(repositoryGitDir.resolve("machete"));
    gitMacheteRepository = gitMacheteRepositoryFactory.create(repositoryMainDir, repositoryGitDir, branchLayout);
  }

  @Test
  @SneakyThrows
  public void upstreamIsCorrectlyInferred() {
    var managedBranchNames = gitMacheteRepository.getManagedBranches().map(b -> b.getName()).toSet();
    var result = gitMacheteRepositoryFactory.inferUpstreamForLocalBranch(
        repositoryMainDir, repositoryGitDir, managedBranchNames, forBranch);
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
