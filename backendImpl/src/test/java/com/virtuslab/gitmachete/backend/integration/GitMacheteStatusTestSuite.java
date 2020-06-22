package com.virtuslab.gitmachete.backend.integration;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.AheadOfRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.BehindRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedFromAndNewerThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedFromAndOlderThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.InSyncToRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.NoRemotes;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.Untracked;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
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
import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRootBranch;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositoryFactory;
import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedTestSuite;

@RunWith(Parameterized.class)
public class GitMacheteStatusTestSuite extends BaseGitRepositoryBackedTestSuite {

  private final GitMacheteRepositoryFactory gitMacheteRepositoryFactory = new GitMacheteRepositoryFactory();
  private final IBranchLayoutReader branchLayoutReader = RuntimeBinding
      .instantiateSoleImplementingClass(IBranchLayoutReader.class);
  private final IGitMacheteRepository gitMacheteRepository;

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
  public GitMacheteStatusTestSuite(String scriptName) {
    super(scriptName);
    IBranchLayout branchLayout = branchLayoutReader.read(repositoryGitDir.resolve("machete"));
    gitMacheteRepository = gitMacheteRepositoryFactory.create(repositoryMainDir, repositoryGitDir, branchLayout);
  }

  @Test
  public void hasSameOutputAsCli() {
    String gitMacheteCliStatus = gitMacheteCliStatus();
    String ourStatus = ourStatus();

    System.out.println("CLI OUTPUT:");
    System.out.println(gitMacheteCliStatus);
    System.out.println("OUR OUTPUT:");
    System.out.println(ourStatus);

    Assert.assertEquals(gitMacheteCliStatus, ourStatus);
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

  private String ourStatus() {
    var sb = new StringBuilder();
    var branches = gitMacheteRepository.getRootBranches();
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
    printCommonParts(branch, /* level */ 0, sb);
  }

  private void printNonRootBranch(IGitMacheteNonRootBranch branch, int level, StringBuilder sb) {
    sb.append("  ");

    sb.append("| ".repeat(level));

    sb.append(System.lineSeparator());

    var commits = branch.getCommits().reverse();
    var forkPoint = branch.getForkPoint().getOrNull();

    for (var c : commits) {
      sb.append("  ");
      sb.append("| ".repeat(level));
      sb.append(c.getShortMessage());
      if (c.equals(forkPoint)) {
        sb.append(" -> fork point ??? commit ${forkPoint.getShortHash()} has been found in reflog of ");
        sb.append(forkPoint.getBranchesContainingInReflog().sorted().mkString(", "));
      }
      sb.append(System.lineSeparator());
    }

    sb.append("  ");
    sb.append("| ".repeat(level - 1));

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

    printCommonParts(branch, level, sb);
  }

  private void printCommonParts(IGitMacheteBranch branch, int level, StringBuilder sb) {
    sb.append(branch.getName());

    var currBranch = gitMacheteRepository.getCurrentBranchIfManaged();
    if (currBranch.isDefined() && currBranch.get().equals(branch))
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

    for (var b : branch.getDownstreamBranches()) {
      printNonRootBranch(b, /* level */ level + 1, sb);
    }
  }
}
