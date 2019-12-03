package com.virtuslab.gitmachete.gitmachetejgit;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.virtuslab.gitcore.gitcoreapi.*;
import com.virtuslab.gitmachete.gitmacheteapi.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class GitMacheteRepository implements IGitMacheteRepository {
  private Optional<String> repositoryName;

  @Getter(AccessLevel.NONE)
  private IGitCoreRepository repo;

  List<IGitMacheteBranch> rootBranches = new LinkedList<>();

  private Path pathToRepoRoot;
  private Path pathToMacheteFile;

  @Getter(AccessLevel.NONE)
  private Character indentType = null;

  @Getter(AccessLevel.NONE)
  private int levelWidth = 0;

  @Getter(AccessLevel.NONE)
  private GitCoreRepositoryFactory gitCoreRepositoryFactory;

  @Inject
  public GitMacheteRepository(
      GitCoreRepositoryFactory gitCoreRepositoryFactory,
      @Assisted Path pathToRepoRoot,
      @Assisted Optional<String> repositoryName)
      throws GitMacheteException {
    this.gitCoreRepositoryFactory = gitCoreRepositoryFactory;

    this.pathToRepoRoot = pathToRepoRoot;
    this.repositoryName = repositoryName;

    this.repo = gitCoreRepositoryFactory.create(pathToRepoRoot);
    this.pathToMacheteFile = this.repo.getGitFolderPath().resolve("machete");

    List<String> lines;
    try {
      lines = Files.readAllLines(pathToMacheteFile);
    } catch (IOException e) {
      throw new GitMacheteException(
          MessageFormat.format(
              "Error while loading machete file ({0})",
              pathToMacheteFile.toAbsolutePath().toString()),
          e);
    }

    lines.removeIf(this::isEmptyLine);

    if (lines.size() < 1) return;

    if (getIndent(lines.get(0)) > 0)
      throw new MacheteFileParseException(
          MessageFormat.format(
              "The initial line of machete file ({0}) cannot be indented",
              pathToMacheteFile.toAbsolutePath().toString()));

    int currentLevel = 0;
    Map<Integer, GitMacheteBranch> macheteBranchesLevelsMap = new HashMap<>();
    Map<Integer, IGitCoreLocalBranch> coreBranchesLevelsMap = new HashMap<>();
    for (var line : lines) {
      int level = getLevel(getIndent(line));

      if (level - currentLevel > 1)
        throw new MacheteFileParseException(
            MessageFormat.format(
                "One of branches in machete file ({0}) has incorrect level in relation to its parent branch",
                pathToMacheteFile.toAbsolutePath().toString()));

      String branchName = line.trim();
      IGitCoreLocalBranch coreLocalBranch;
      try {
        // Checking if local branch of this name really exists in this repository
        coreLocalBranch = this.repo.getLocalBranch(branchName);
      } catch (GitException e) {
        throw new GitImplementationException(e);
      }

      var branch = new GitMacheteBranch(branchName);

      macheteBranchesLevelsMap.put(level, branch);
      coreBranchesLevelsMap.put(level, coreLocalBranch);

      try {
        branch.syncToOriginStatus =
            getSyncToOriginByTrackingStatus(coreLocalBranch.getTrackingStatus());

        if (level == 0) {
          branch.commits = List.of();
          branch.upstreamBranch = Optional.empty();
          branch.syncToParentStatus = SyncToParentStatus.InSync;
          addRootBranch(branch);
        } else {
          branch.commits =
              getCommitsBelongingSpecificallyToBranch(
                  coreLocalBranch, coreBranchesLevelsMap.get(level - 1));
          branch.upstreamBranch = Optional.of(macheteBranchesLevelsMap.get(level - 1));
          branch.syncToParentStatus =
              getSyncToParentStatus(coreLocalBranch, coreBranchesLevelsMap.get(level - 1));
          macheteBranchesLevelsMap.get(level - 1).childBranches.add(branch);
        }
      } catch (GitException e) {
        throw new GitImplementationException(e);
      }

      currentLevel = level;
    }
  }

  private boolean isEmptyLine(String l) {
    return l.trim().length() < 1;
  }

  private int getIndent(String l) {
    int indent = 0;
    for (int i = 0; i < l.length(); i++) {
      if (indentType == null) {
        if (l.charAt(i) != ' ' && l.charAt(i) != '\t') {
          break;
        } else {
          indent++;
          indentType = l.charAt(i);
        }
      } else {
        if (l.charAt(i) == indentType) indent++;
        else break;
      }
    }

    return indent;
  }

  private int getLevel(int indent) throws MacheteFileParseException {
    if (levelWidth == 0 && indent > 0) {
      levelWidth = indent;
      return 1;
    } else if (indent == 0) {
      return 0;
    }

    if (indent % levelWidth != 0)
      throw new MacheteFileParseException(
          MessageFormat.format(
              "Levels of indentations are not matching in machete file ({0})",
              pathToMacheteFile.toAbsolutePath().toString()));

    return indent / levelWidth;
  }

  private SyncToOriginStatus getSyncToOriginByTrackingStatus(
      Optional<IGitCoreBranchTrackingStatus> ts) {
    if (ts.isEmpty()) return SyncToOriginStatus.Untracked;

    if (ts.get().getAhead() > 0 && ts.get().getBehind() > 0) return SyncToOriginStatus.Diverged;
    else if (ts.get().getAhead() > 0) return SyncToOriginStatus.Ahead;
    else if (ts.get().getBehind() > 0) return SyncToOriginStatus.Behind;
    else return SyncToOriginStatus.InSync;
  }

  private List<IGitMacheteCommit> translateIGitCoreCommitsToIGitMacheteCommits(
      List<IGitCoreCommit> list) throws GitException {
    var l = new LinkedList<IGitMacheteCommit>();

    for (var c : list) {
      l.add(new GitMacheteCommit(c.getMessage()));
    }

    return l;
  }

  private List<IGitMacheteCommit> getCommitsBelongingSpecificallyToBranch(
      IGitCoreLocalBranch childBranch, IGitCoreLocalBranch parentBranch) throws GitException {
    Optional<IGitCoreCommit> forkPoint = childBranch.getForkPoint(parentBranch);
    if (forkPoint.isEmpty()) return List.of();

    return translateIGitCoreCommitsToIGitMacheteCommits(
        childBranch.getCommitsUntil(forkPoint.get()));
  }

  private SyncToParentStatus getSyncToParentStatus(
      IGitCoreLocalBranch childBranch, IGitCoreLocalBranch parentBranch) throws GitException {
    if (childBranch.getPointedCommit().equals(parentBranch.getPointedCommit())) {
      if (childBranch.hasJustBeenCreated()) return SyncToParentStatus.InSync;
      else return SyncToParentStatus.Merged;
    } else {
      Optional<IGitCoreCommit> forkPoint = childBranch.getForkPoint(parentBranch);
      boolean isParentAncestorOfChild =
          parentBranch.getPointedCommit().isAncestorOf(childBranch.getPointedCommit());

      if (isParentAncestorOfChild) {
        if (forkPoint.isEmpty() || !forkPoint.get().equals(parentBranch.getPointedCommit()))
          return SyncToParentStatus.NotADirectDescendant;
        else return SyncToParentStatus.InSync;
      } else {
        boolean isChildAncestorOfParent =
            childBranch.getPointedCommit().isAncestorOf(parentBranch.getPointedCommit());

        if (isChildAncestorOfParent) return SyncToParentStatus.Merged;
        else return SyncToParentStatus.OutOfSync;
      }
    }
  }

  @Override
  public String toString() {
    var sb = new StringBuilder();
    for (var b : rootBranches) {
      printBranch(b, 0, sb);
    }

    return sb.toString();
  }

  private void printBranch(IGitMacheteBranch branch, int level, StringBuilder sb) {
    sb.append("\t".repeat(level));
    sb.append(branch.getName());
    sb.append(" - (Remote: ");
    sb.append(branch.getSyncToOriginStatus());
    sb.append("; Parent: ");
    sb.append(branch.getSyncToParentStatus());
    sb.append(") - UPSTREAM: ");
    sb.append(
        branch.getUpstreamBranch().isEmpty() ? "none" : branch.getUpstreamBranch().get().getName());
    sb.append(" - ");
    for (var c : branch.getCommits()) {
      sb.append("; ");
      sb.append(c.getMessage().split("\n", 2)[0]);
    }
    sb.append(System.lineSeparator());

    for (var b : branch.getBranches()) {
      printBranch(b, level + 1, sb);
    }
  }

  @Override
  public Optional<IGitMacheteBranch> getCurrentBranch() throws GitMacheteException {
    Optional<IGitCoreLocalBranch> branch;
    try {
      branch = repo.getCurrentBranch();
    } catch (GitException e) {
      throw new GitMacheteJGitException("Error occurred while getting current branch object", e);
    }

    if (branch.isEmpty()) return Optional.empty();
    else
      try {
        return Optional.of(new GitMacheteBranch(branch.get().getName()));
      } catch (GitException e) {
        throw new GitMacheteJGitException("Error occurred while getting current branch name", e);
      }
  }

  @Override
  public void addRootBranch(IGitMacheteBranch branch) {
    rootBranches.add(branch);
  }

  @Override
  public Map<String, IGitMacheteRepository> getSubmoduleRepositories() throws GitMacheteException {
    Map<String, IGitMacheteRepository> submodules = new HashMap<String, IGitMacheteRepository>();

    Map<String, IGitCoreSubmoduleEntry> subs;

    try {
      subs = this.repo.getSubmodules();
    } catch (GitException e) {
      throw new GitMacheteJGitException("Error while getting submodules", e);
    }

    for (var e : subs.entrySet()) {
      IGitMacheteRepository r =
          new GitMacheteRepository(
              this.gitCoreRepositoryFactory,
              e.getValue().getPath(),
              Optional.of(e.getValue().getName()));
      submodules.put(e.getKey(), r);
    }

    return submodules;
  }
}
