package com.virtuslab.gitmachete.backend.integration;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.AheadOfRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.BehindRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedFromAndNewerThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedFromAndOlderThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.InSyncToRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.NoRemotes;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.Untracked;
import static com.virtuslab.gitmachete.backend.integration.IntegrationTestUtils.ensureExpectedCliVersion;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static org.junit.runners.Parameterized.Parameters;

import io.vavr.collection.List;
import io.vavr.collection.Set;
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
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutReader;
import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRootBranch;
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
    var process = new ProcessBuilder()
        .command("git", "machete", "status", "--list-commits")
        .directory(repositoryMainDir.toFile())
        .start();
    return new String(process.getInputStream().readAllBytes());
  }

  @SneakyThrows
  private String gitMacheteCliDiscover() {
    var process = new ProcessBuilder()
        .command("git", "machete", "discover", "--list-commits", "--yes")
        .directory(repositoryMainDir.toFile())
        .start();

    return new String(process.getInputStream().readAllBytes())
        .lines()
        .collect(List.collector())
        .drop(2) // Let's skip the informational output at the beginning and at the end.
        .dropRight(2)
        .mkString(System.lineSeparator());
  }

  private String ourGitMacheteRepositorySnapshotAsString() {
    var sb = new StringBuilder();
    var branches = gitMacheteRepositorySnapshot.getRootBranches();
    int lastRootBranchIndex = branches.size() - 1;
    for (int currentRootBranch = 0; currentRootBranch <= lastRootBranchIndex; currentRootBranch++) {
      var b = branches.get(currentRootBranch);
      printRootBranch(b, sb);
      if (currentRootBranch < lastRootBranchIndex)
        sb.append(System.lineSeparator());
    }

    return sb.toString();
  }

  private void printRootBranch(IGitMacheteRootBranch branch, StringBuilder sb) {
    sb.append("  ");
    printCommonParts(branch, /* path */ List.empty(), sb);
  }

  private void printNonRootBranch(IGitMacheteNonRootBranch branch, List<IGitMacheteNonRootBranch> path, StringBuilder sb) {
    String prefix = path.init()
        .map(anc -> anc == anc.getParentBranch().getChildBranches().last() ? "  " : "| ")
        .mkString();

    sb.append("  ");
    sb.append(prefix);
    sb.append("| ");
    sb.append(System.lineSeparator());

    var commits = branch.getCommits().reverse();
    var forkPoint = branch.getForkPoint().getOrNull();

    for (var c : commits) {
      sb.append("  ");
      sb.append(prefix);
      sb.append("| ");

      sb.append(c.getShortMessage());
      if (c.equals(forkPoint)) {
        sb.append(" -> fork point ??? commit ${forkPoint.getShortHash()} has been found in reflog of ");
        Set<String> branchesContainingInReflog = forkPoint.getBranchesContainingInReflog().toSet();
        // This is a hack necessary to keep consistent with CLI, which skips remote branch from the fork point hint
        // in case a corresponding local branch is already present.
        // In the IntelliJ plugin, we list both local and its remote tracking branch
        // if both contain the given commit in their reflogs.
        Set<String> withoutBothLocalAndItsRemoteTracking = branchesContainingInReflog
            .reject(b -> b.startsWith("origin/") && branchesContainingInReflog.contains(b.substring("origin/".length())));
        sb.append(withoutBothLocalAndItsRemoteTracking.toList().sorted().mkString(", "));
      }
      sb.append(System.lineSeparator());
    }

    sb.append("  ");
    sb.append(prefix);

    var parentStatus = branch.getSyncToParentStatus();
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

  private void printCommonParts(IGitMacheteBranch branch, List<IGitMacheteNonRootBranch> path, StringBuilder sb) {
    sb.append(branch.getName());

    var currBranch = gitMacheteRepositorySnapshot.getCurrentBranchIfManaged();
    if (currBranch.isDefined() && currBranch.get() == branch)
      sb.append(" *");

    var customAnnotation = branch.getCustomAnnotation();
    if (customAnnotation.isDefined()) {
      sb.append("  ");
      sb.append(customAnnotation.get());
    }
    var syncToRemote = branch.getSyncToRemoteStatus();

    SyncToRemoteStatus.Relation relation = syncToRemote.getRelation();
    if (relation != NoRemotes && relation != InSyncToRemote) {
      var remoteName = syncToRemote.getRemoteName();
      sb.append(" (");
      sb.append(Match(relation).of(
          Case($(Untracked), "untracked"),
          Case($(AheadOfRemote), "ahead of " + remoteName),
          Case($(BehindRemote), "behind " + remoteName),
          Case($(DivergedFromAndNewerThanRemote), "diverged from " + remoteName),
          Case($(DivergedFromAndOlderThanRemote), "diverged from & older than " + remoteName)));
      sb.append(")");
    }
    var statusHookOutput = branch.getStatusHookOutput();
    if (statusHookOutput.isDefined()) {
      sb.append("  ");
      sb.append(statusHookOutput.get());
    }
    sb.append(System.lineSeparator());

    for (var childBranch : branch.getChildBranches()) {
      printNonRootBranch(childBranch, path.append(childBranch), sb);
    }
  }
}
