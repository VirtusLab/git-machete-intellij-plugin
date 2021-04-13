package com.virtuslab.gitmachete.backend.integration;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.AheadOfRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.BehindRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedFromAndNewerThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedFromAndOlderThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.InSyncToRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.NoRemotes;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.Untracked;
import static com.virtuslab.gitmachete.backend.integration.IntegrationTestUtils.ensureExpectedCliVersion;
import static com.virtuslab.gitmachete.testcommon.TestProcessUtils.runProcessAndReturnStdout;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static org.junit.runners.Parameterized.Parameters;

import io.vavr.collection.List;
import io.vavr.collection.Stream;
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
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutReader;
import com.virtuslab.gitmachete.backend.api.IBranchReference;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.IRootManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositoryCache;
import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedIntegrationTestSuite;

@RunWith(Parameterized.class)
public class StatusAndDiscoverIntegrationTestSuite extends BaseGitRepositoryBackedIntegrationTestSuite {

  private final IBranchLayoutReader branchLayoutReader = RuntimeBinding
      .instantiateSoleImplementingClass(IBranchLayoutReader.class);
  private final GitMacheteRepositoryCache gitMacheteRepositoryCache = new GitMacheteRepositoryCache();
  private final IGitMacheteRepository gitMacheteRepository;
  private IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot;

  @BeforeClass
  public static void doEnsureExpectedCliVersion() {
    ensureExpectedCliVersion();
  }

  @Parameters(name = "{0} (#{index})")
  public static String[] getScriptNames() {
    return new String[]{
        SETUP_FOR_NO_REMOTES,
        SETUP_WITH_SINGLE_REMOTE,
        SETUP_WITH_MULTIPLE_REMOTES,
        SETUP_FOR_DIVERGED_AND_OLDER_THAN,
        SETUP_FOR_YELLOW_EDGES,
        SETUP_FOR_OVERRIDDEN_FORK_POINT,
    };
  }

  @SneakyThrows
  public StatusAndDiscoverIntegrationTestSuite(String scriptName) {
    super(scriptName);
    gitMacheteRepository = gitMacheteRepositoryCache.getInstance(repositoryMainDir, repositoryGitDir);
  }

  @Test
  @SneakyThrows
  public void yieldsSameStatusAsCli() {
    String gitMacheteCliStatus = gitMacheteCliStatus();

    IBranchLayout branchLayout = branchLayoutReader.read(repositoryGitDir.resolve("machete"));
    gitMacheteRepositorySnapshot = gitMacheteRepository.createSnapshotForLayout(branchLayout);
    String ourStatus = ourGitMacheteRepositorySnapshotAsString();

    System.out.println("CLI OUTPUT:");
    System.out.println(gitMacheteCliStatus);
    System.out.println();
    System.out.println("OUR OUTPUT:");
    System.out.println(ourStatus);

    Assert.assertEquals(gitMacheteCliStatus, ourStatus);
  }

  @Test
  @SneakyThrows
  public void discoversSameLayoutAsCli() {
    String gitMacheteCliDiscoverOutput = gitMacheteCliDiscover();

    gitMacheteRepositorySnapshot = gitMacheteRepository.discoverLayoutAndCreateSnapshot();
    String ourDiscoverOutput = ourGitMacheteRepositorySnapshotAsString();

    System.out.println("CLI OUTPUT:");
    System.out.println(gitMacheteCliDiscoverOutput);
    System.out.println();
    System.out.println("OUR OUTPUT:");
    System.out.println(ourDiscoverOutput);

    Assert.assertEquals(gitMacheteCliDiscoverOutput, ourDiscoverOutput);
  }

  @Rule(order = Integer.MIN_VALUE)
  public final TestWatcher cleanUpAfterSuccessfulTest = new TestWatcher() {
    @Override
    protected void succeeded(Description description) {
      cleanUpParentDir();
    }

    // After a failed test, keep the parent directory intact for further manual inspection.
  };

  @SneakyThrows
  private String gitMacheteCliStatus() {
    return runProcessAndReturnStdout(/* workingDirectory */ repositoryMainDir, /* timeoutSeconds */ 15,
        /* command */ "git-machete", "status", "--list-commits");
  }

  @SneakyThrows
  private String gitMacheteCliDiscover() {
    String output = runProcessAndReturnStdout(/* workingDirectory */ repositoryMainDir, /* timeoutSeconds */ 15,
        /* command */ "git-machete", "discover", "--list-commits", "--yes");

    return Stream.of(output.split(System.lineSeparator()))
        .drop(2) // Let's skip the informational output at the beginning and at the end.
        .dropRight(2)
        .mkString(System.lineSeparator());
  }

  private String ourGitMacheteRepositorySnapshotAsString() {
    val sb = new StringBuilder();
    val branches = gitMacheteRepositorySnapshot.getRootBranches();
    int lastRootBranchIndex = branches.size() - 1;
    for (int currentRootBranch = 0; currentRootBranch <= lastRootBranchIndex; currentRootBranch++) {
      val b = branches.get(currentRootBranch);
      printRootBranch(b, sb);
      if (currentRootBranch < lastRootBranchIndex)
        sb.append(System.lineSeparator());
    }

    return sb.toString();
  }

  private void printRootBranch(IRootManagedBranchSnapshot branch, StringBuilder sb) {
    sb.append("  ");
    printCommonParts(branch, /* path */ List.empty(), sb);
  }

  private void printNonRootBranch(
      INonRootManagedBranchSnapshot branch,
      List<INonRootManagedBranchSnapshot> path,
      StringBuilder sb) {

    String prefix = path.init()
        .map(anc -> anc == anc.getParent().getChildren().last() ? "  " : "| ")
        .mkString();

    sb.append("  ");
    sb.append(prefix);
    sb.append("| ");
    sb.append(System.lineSeparator());

    val commits = branch.getCommits().reverse();
    val forkPoint = branch.getForkPoint().getOrNull();

    for (val c : commits) {
      sb.append("  ");
      sb.append(prefix);
      sb.append("| ");

      sb.append(c.getShortMessage());
      if (c.equals(forkPoint)) {
        sb.append(" -> fork point ??? commit ${forkPoint.getShortHash()} seems to be a part of the unique history of ");
        List<IBranchReference> uniqueBranchesContainingInReflog = forkPoint.getUniqueBranchesContainingInReflog();
        sb.append(uniqueBranchesContainingInReflog.map(b -> b.getName()).sorted().mkString(" and "));
      }
      sb.append(System.lineSeparator());
    }

    sb.append("  ");
    sb.append(prefix);

    val parentStatus = branch.getSyncToParentStatus();
    if (parentStatus == SyncToParentStatus.InSync)
      sb.append("o");
    else if (parentStatus == SyncToParentStatus.OutOfSync)
      sb.append("x");
    else if (parentStatus == SyncToParentStatus.InSyncButForkPointOff)
      sb.append("?");
    else if (parentStatus == SyncToParentStatus.MergedToParent)
      sb.append("m");
    sb.append("-");

    printCommonParts(branch, path, sb);
  }

  private void printCommonParts(IManagedBranchSnapshot branch, List<INonRootManagedBranchSnapshot> path, StringBuilder sb) {
    sb.append(branch.getName());

    val currBranch = gitMacheteRepositorySnapshot.getCurrentBranchIfManaged();
    if (currBranch.isDefined() && currBranch.get() == branch)
      sb.append(" *");

    val customAnnotation = branch.getCustomAnnotation();
    if (customAnnotation.isDefined()) {
      sb.append("  ");
      sb.append(customAnnotation.get());
    }
    val syncToRemote = branch.getSyncToRemoteStatus();

    SyncToRemoteStatus.Relation relation = syncToRemote.getRelation();
    if (relation != NoRemotes && relation != InSyncToRemote) {
      val remoteName = syncToRemote.getRemoteName();
      sb.append(" (");
      sb.append(Match(relation).of(
          Case($(Untracked), "untracked"),
          Case($(AheadOfRemote), "ahead of " + remoteName),
          Case($(BehindRemote), "behind " + remoteName),
          Case($(DivergedFromAndNewerThanRemote), "diverged from " + remoteName),
          Case($(DivergedFromAndOlderThanRemote), "diverged from & older than " + remoteName)));
      sb.append(")");
    }
    val statusHookOutput = branch.getStatusHookOutput();
    if (statusHookOutput.isDefined()) {
      sb.append("  ");
      sb.append(statusHookOutput.get());
    }
    sb.append(System.lineSeparator());

    for (val childBranch : branch.getChildren()) {
      printNonRootBranch(childBranch, path.append(childBranch), sb);
    }
  }
}
