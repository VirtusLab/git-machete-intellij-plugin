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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import io.vavr.collection.List;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutReader;
import com.virtuslab.gitcore.api.IGitCoreRepositoryFactory;
import com.virtuslab.gitcore.impl.jgit.GitCoreRepositoryFactory;
import com.virtuslab.gitmachete.backend.api.*;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositoryCache;
import com.virtuslab.gitmachete.testcommon.GitRepositoryBackedIntegrationTestSuiteInitializer;

public class StatusAndDiscoverIntegrationTestSuite {

  private static final IGitCoreRepositoryFactory gitCoreRepositoryFactory = new GitCoreRepositoryFactory();

  private final IBranchLayoutReader branchLayoutReader = ApplicationManager.getApplication()
      .getService(IBranchLayoutReader.class);
  private final GitMacheteRepositoryCache gitMacheteRepositoryCache = new GitMacheteRepositoryCache();

  public static String[] getScriptNames() {
    return ALL_SETUP_SCRIPTS;
  }

  @SneakyThrows
  @ParameterizedTest
  @MethodSource("getScriptNames")
  public void yieldsSameStatusAsCli(String scriptName) {
    val it = new GitRepositoryBackedIntegrationTestSuiteInitializer(scriptName);
    // This setup needs to happen BEFORE GitMacheteRepositoryCache is created
    val application = mock(Application.class);
    when(application.getService(any())).thenReturn(gitCoreRepositoryFactory);
    when(ApplicationManager.getApplication()).thenReturn(application);

    val gitMacheteRepository = gitMacheteRepositoryCache.getInstance(it.rootDirectoryPath, it.mainGitDirectoryPath,
        it.worktreeGitDirectoryPath);

    String gitMacheteCliStatus = gitMacheteCliStatusOutput(scriptName);

    BranchLayout branchLayout = branchLayoutReader
        .read(new FileInputStream(it.mainGitDirectoryPath.resolve("machete").toFile()));
    val gitMacheteRepositorySnapshot = gitMacheteRepository.createSnapshotForLayout(branchLayout);
    String ourStatus = ourGitMacheteRepositorySnapshotAsString(gitMacheteRepositorySnapshot);

    System.out.println("CLI OUTPUT:");
    System.out.println(gitMacheteCliStatus);
    System.out.println();
    System.out.println("OUR OUTPUT:");
    System.out.println(ourStatus);

    assertEquals(gitMacheteCliStatus.trim(), ourStatus.trim());

    cleanUpDir(it.parentDirectoryPath);
  }

  @SneakyThrows
  @ParameterizedTest
  @MethodSource("getScriptNames")
  public void discoversSameLayoutAsCli(String scriptName) {
    val it = new GitRepositoryBackedIntegrationTestSuiteInitializer(scriptName);
    // This setup needs to happen BEFORE GitMacheteRepositoryCache is created
    val application = mock(Application.class);
    when(application.getService(any())).thenReturn(gitCoreRepositoryFactory);
    when(ApplicationManager.getApplication()).thenReturn(application);

    val gitMacheteRepository = gitMacheteRepositoryCache.getInstance(it.rootDirectoryPath, it.mainGitDirectoryPath,
        it.worktreeGitDirectoryPath);

    String gitMacheteCliDiscoverOutput = gitMacheteCliDiscoverOutput(scriptName);

    val gitMacheteRepositorySnapshot = gitMacheteRepository.discoverLayoutAndCreateSnapshot();
    String ourDiscoverOutput = ourGitMacheteRepositorySnapshotAsString(gitMacheteRepositorySnapshot);

    System.out.println("CLI OUTPUT:");
    System.out.println(gitMacheteCliDiscoverOutput);
    System.out.println();
    System.out.println("OUR OUTPUT:");
    System.out.println(ourDiscoverOutput);

    assertEquals(gitMacheteCliDiscoverOutput, ourDiscoverOutput);

    cleanUpDir(it.parentDirectoryPath);
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
