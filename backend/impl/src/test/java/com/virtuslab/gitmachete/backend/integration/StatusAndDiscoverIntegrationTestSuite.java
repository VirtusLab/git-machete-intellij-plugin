package com.virtuslab.gitmachete.backend.integration;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.AheadOfRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.BehindRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.DivergedFromAndNewerThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.DivergedFromAndOlderThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.InSyncToRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.NoRemotes;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Untracked;
import static com.virtuslab.gitmachete.testcommon.SetupScripts.ALL_SETUP_SCRIPTS;
import static com.virtuslab.gitmachete.testcommon.TestFileUtils.cleanUpDir;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static org.junit.runners.Parameterized.Parameters;

import java.nio.charset.StandardCharsets;

import io.vavr.collection.List;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutReader;
import com.virtuslab.gitmachete.backend.api.*;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositoryCache;
import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedIntegrationTestSuite;

@RunWith(Parameterized.class)
public class StatusAndDiscoverIntegrationTestSuite extends BaseGitRepositoryBackedIntegrationTestSuite {

  private final IBranchLayoutReader branchLayoutReader = RuntimeBinding
      .instantiateSoleImplementingClass(IBranchLayoutReader.class);
  private final GitMacheteRepositoryCache gitMacheteRepositoryCache = new GitMacheteRepositoryCache();
  private final IGitMacheteRepository gitMacheteRepository;
  private IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot;

  @Parameters(name = "{0} (#{index})")
  public static String[] getScriptNames() {
    return ALL_SETUP_SCRIPTS;
  }

  @SneakyThrows
  public StatusAndDiscoverIntegrationTestSuite(String scriptName) {
    super(scriptName);
    gitMacheteRepository = gitMacheteRepositoryCache.getInstance(rootDirectoryPath, mainGitDirectoryPath,
        worktreeGitDirectoryPath);
  }

  @Test
  @SneakyThrows
  public void yieldsSameStatusAsCli() {
    String gitMacheteCliStatus = gitMacheteCliStatusOutput();

    BranchLayout branchLayout = branchLayoutReader.read(mainGitDirectoryPath.resolve("machete"));
    gitMacheteRepositorySnapshot = gitMacheteRepository.createSnapshotForLayout(branchLayout);
    String ourStatus = ourGitMacheteRepositorySnapshotAsString();

    System.out.println("CLI OUTPUT:");
    System.out.println(gitMacheteCliStatus);
    System.out.println();
    System.out.println("OUR OUTPUT:");
    System.out.println(ourStatus);

    Assert.assertEquals(gitMacheteCliStatus.trim(), ourStatus.trim());
  }

  @Test
  @SneakyThrows
  public void discoversSameLayoutAsCli() {
    String gitMacheteCliDiscoverOutput = gitMacheteCliDiscoverOutput();

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
      cleanUpDir(parentDirectoryPath);
    }

    // After a failed test, keep the parent directory intact for further manual inspection.
  };

  @SneakyThrows
  private String gitMacheteCliStatusOutput() {
    return IOUtils.resourceToString("/${scriptName}-status.txt", StandardCharsets.UTF_8);
  }

  @SneakyThrows
  private String gitMacheteCliDiscoverOutput() {
    return IOUtils.resourceToString("/${scriptName}-discover.txt", StandardCharsets.UTF_8);
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
    sb.append("|");
    sb.append(System.lineSeparator());

    val commits = branch.getUniqueCommits().reverse();
    val forkPoint = branch.getForkPoint();

    for (val c : commits) {
      sb.append("  ");
      sb.append(prefix);
      sb.append("| ");

      sb.append(c.getShortMessage());
      if (c.equals(forkPoint)) {
        sb.append(" -> fork point ??? commit ${forkPoint.getShortHash()} seems to be a part of the unique history of ");
        List<IBranchReference> uniqueBranchesContainingInReflog = forkPoint.getUniqueBranchesContainingInReflog();
        sb.append(uniqueBranchesContainingInReflog.map(IBranchReference::getName).sorted().mkString(" and "));
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
    if (currBranch != null && currBranch == branch)
      sb.append(" *");

    val customAnnotation = branch.getCustomAnnotation();
    if (customAnnotation != null) {
      sb.append("  ");
      sb.append(customAnnotation);
    }
    val relationToRemote = branch.getRelationToRemote();

    SyncToRemoteStatus syncToRemoteStatus = relationToRemote.getSyncToRemoteStatus();
    if (syncToRemoteStatus != NoRemotes && syncToRemoteStatus != InSyncToRemote) {
      val remoteName = relationToRemote.getRemoteName();
      sb.append(" (");
      sb.append(Match(syncToRemoteStatus).of(
          Case($(Untracked), "untracked"),
          Case($(AheadOfRemote), "ahead of " + remoteName),
          Case($(BehindRemote), "behind " + remoteName),
          Case($(DivergedFromAndNewerThanRemote), "diverged from " + remoteName),
          Case($(DivergedFromAndOlderThanRemote), "diverged from & older than " + remoteName)));
      sb.append(")");
    }
    val statusHookOutput = branch.getStatusHookOutput();
    if (statusHookOutput != null) {
      sb.append("  ");
      sb.append(statusHookOutput);
    }
    sb.append(System.lineSeparator());

    for (val childBranch : branch.getChildren()) {
      printNonRootBranch(childBranch, path.append(childBranch), sb);
    }
  }
}
