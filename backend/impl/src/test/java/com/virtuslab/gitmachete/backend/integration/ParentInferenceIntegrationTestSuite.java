package com.virtuslab.gitmachete.backend.integration;

import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_FOR_OVERRIDDEN_FORK_POINT;
import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_FOR_YELLOW_EDGES;
import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_WITH_SINGLE_REMOTE;
import static com.virtuslab.gitmachete.testcommon.TestFileUtils.cleanUpDir;

import java.io.FileInputStream;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
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
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import com.virtuslab.branchlayout.impl.readwrite.BranchLayoutReader;
import com.virtuslab.gitcore.api.IGitCoreRepositoryFactory;
import com.virtuslab.gitcore.impl.jgit.GitCoreRepositoryFactory;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositoryCache;
import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedIntegrationTestSuite;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Parameterized.class)
@PrepareForTest(ApplicationManager.class)
public class ParentInferenceIntegrationTestSuite extends BaseGitRepositoryBackedIntegrationTestSuite {

  private static final IGitCoreRepositoryFactory gitCoreRepositoryFactory = new GitCoreRepositoryFactory();

  private final IGitMacheteRepository gitMacheteRepository;
  private final IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot;

  private final String forBranch;
  private final String expectedParent;

  @BeforeClass
  public static void setUpStatic() {
    PowerMockito.mockStatic(ApplicationManager.class);
  }

  @Parameterized.Parameters(name = "{0}: inferred parent of {1} should be {2} (#{index})")
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

    val branchLayoutReader = new BranchLayoutReader();
    val branchLayout = branchLayoutReader.read(new FileInputStream(mainGitDirectoryPath.resolve("machete").toFile()));

    // This setup needs to happen BEFORE GitMacheteRepositoryCache is created
    val application = PowerMockito.mock(Application.class);
    PowerMockito.when(application.getService(IGitCoreRepositoryFactory.class)).thenReturn(gitCoreRepositoryFactory);
    PowerMockito.stub(PowerMockito.method(ApplicationManager.class, "getApplication")).toReturn(application);

    GitMacheteRepositoryCache gitMacheteRepositoryCache = new GitMacheteRepositoryCache();
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
