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
    for (var line : lines) {
      int level = getLevel(getIndent(line));

      if (level - currentLevel > 1)
        throw new MacheteFileParseException(
            MessageFormat.format(
                "One of branches in machete file ({0}) has incorrect level in relation to its parent branch",
                pathToMacheteFile.toAbsolutePath().toString()));

      String trimmedLine = line.trim();

      String branchName;
      Optional<String> customAnnotation;
      int indexOfSpace = trimmedLine.indexOf(' ');
      if (indexOfSpace > -1) {
        branchName = trimmedLine.substring(0, indexOfSpace);
        customAnnotation = Optional.of(trimmedLine.substring(indexOfSpace + 1).trim());
      } else {
        branchName = trimmedLine;
        customAnnotation = Optional.empty();
      }

      IGitCoreLocalBranch coreLocalBranch;
      try {
        // Checking if local branch of this name really exists in this repository
        coreLocalBranch = this.repo.getLocalBranch(branchName);
      } catch (GitException e) {
        throw new GitImplementationException(e);
      }

      GitMacheteBranch branch;

      try {
        branch = new GitMacheteBranch(coreLocalBranch);
      } catch (GitException e) {
        throw new GitMacheteException(
            MessageFormat.format("Error while creating git machete branch ({0})", branchName), e);
      }

      macheteBranchesLevelsMap.put(level, branch);

      branch.customAnnotation = customAnnotation;

      if (level == 0) {
        branch.upstreamBranch = Optional.empty();
        addRootBranch(branch);
      } else {
        branch.upstreamBranch = Optional.of(macheteBranchesLevelsMap.get(level - 1));
        macheteBranchesLevelsMap.get(level - 1).childBranches.add(branch);
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

  @Override
  public Optional<IGitMacheteBranch> getCurrentBranch() throws GitMacheteException {
    Optional<IGitCoreLocalBranch> branch;
    try {
      branch = repo.getCurrentBranch();
    } catch (GitException e) {
      throw new GitMacheteJGitException("Error occurred while getting current branch object", e);
    }

    if (branch.isEmpty()) return Optional.empty();
    else {
      try {
        return Optional.of(new GitMacheteBranch(branch.get()));
      } catch (GitException e) {
        throw new GitMacheteException("Error while creating current git machete branch");
      }
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
