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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import io.vavr.collection.List;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.virtuslab.gitmachete.backend.api.*;

public class StatusAndDiscoverIntegrationTestSuite extends BaseIntegrationTestSuite {

  public static String[] getScriptNames() {
    return ALL_SETUP_SCRIPTS;
  }

  @ParameterizedTest
  @MethodSource("getScriptNames")
  @SneakyThrows
  public void yieldsSameStatusAsCli(String scriptName) {
    setUp(scriptName);

    String gitMacheteCliStatus = gitMacheteCliStatusOutput(scriptName);

    val gitMacheteRepositorySnapshot = gitMacheteRepository.createSnapshotForLayout(branchLayout);
    String ourStatus = ourGitMacheteRepositorySnapshotAsString(gitMacheteRepositorySnapshot);

    System.out.println("CLI OUTPUT:");
    System.out.println(gitMacheteCliStatus);
    System.out.println();
    System.out.println("OUR OUTPUT:");
    System.out.println(ourStatus);

    assertEquals(gitMacheteCliStatus, ourStatus, "in " + repo.rootDirectoryPath + ", set up using " + scriptName);

    // Deliberately done in the test and in not an @After method, so that the directory is retained in case of test failure.
    cleanUpDir(repo.parentDirectoryPath);
  }

  @ParameterizedTest
  @MethodSource("getScriptNames")
  @SneakyThrows
  public void discoversSameLayoutAsCli(String scriptName) {
    setUp(scriptName);

    String gitMacheteCliDiscoverOutput = gitMacheteCliDiscoverOutput(scriptName);

    val gitMacheteRepositorySnapshot = gitMacheteRepository.discoverLayoutAndCreateSnapshot();
    String ourDiscoverOutput = ourGitMacheteRepositorySnapshotAsString(gitMacheteRepositorySnapshot);

    System.out.println("CLI OUTPUT:");
    System.out.println(gitMacheteCliDiscoverOutput);
    System.out.println();
    System.out.println("OUR OUTPUT:");
    System.out.println(ourDiscoverOutput);

    assertEquals(gitMacheteCliDiscoverOutput, ourDiscoverOutput,
        "in " + repo.rootDirectoryPath + ", set up using " + scriptName);

    // Deliberately done in the test and in not an @After method, so that the directory is retained in case of test failure.
    cleanUpDir(repo.parentDirectoryPath);
  }

  @SneakyThrows
  private String gitMacheteCliStatusOutput(String scriptName) {
    return IOUtils.resourceToString("/${scriptName}-status.txt", StandardCharsets.UTF_8);
  }

  @SneakyThrows
  private String gitMacheteCliDiscoverOutput(String scriptName) {
    return IOUtils.resourceToString("/${scriptName}-discover.txt", StandardCharsets.UTF_8);
  }

  private String ourGitMacheteRepositorySnapshotAsString(IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot) {
    val sb = new StringBuilder();
    val branches = gitMacheteRepositorySnapshot.getRootBranches();
    int lastRootBranchIndex = branches.size() - 1;
    for (int currentRootBranch = 0; currentRootBranch <= lastRootBranchIndex; currentRootBranch++) {
      val b = branches.get(currentRootBranch);
      printRootBranch(gitMacheteRepositorySnapshot, b, sb);
      if (currentRootBranch < lastRootBranchIndex)
        sb.append(System.lineSeparator());
    }

    return sb.toString();
  }

  private void printRootBranch(IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot, IRootManagedBranchSnapshot branch,
      StringBuilder sb) {
    sb.append("  ");
    printCommonParts(gitMacheteRepositorySnapshot, branch, /* path */ List.empty(), sb);
  }

  private void printNonRootBranch(
      IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot,
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

    printCommonParts(gitMacheteRepositorySnapshot, branch, path, sb);
  }

  private void printCommonParts(IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot, IManagedBranchSnapshot branch,
      List<INonRootManagedBranchSnapshot> path, StringBuilder sb) {
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
      printNonRootBranch(gitMacheteRepositorySnapshot, childBranch, path.append(childBranch), sb);
    }
  }
}
