package com.virtuslab.gitmachete.gitmachetejgit;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.virtuslab.branchrelationfile.api.BranchRelationFileException;
import com.virtuslab.branchrelationfile.api.BranchRelationFileFactory;
import com.virtuslab.branchrelationfile.api.IBranchRelationFile;
import com.virtuslab.branchrelationfile.api.IBranchRelationFileBranchEntry;
import com.virtuslab.gitcore.gitcoreapi.*;
import com.virtuslab.gitmachete.gitmacheteapi.*;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
  private IBranchRelationFile macheteFile;

  @Getter(AccessLevel.NONE)
  private Character indentType = null;

  @Getter(AccessLevel.NONE)
  private int levelWidth = 0;

  @Getter(AccessLevel.NONE)
  private GitCoreRepositoryFactory gitCoreRepositoryFactory;

  @Getter(AccessLevel.NONE)
  private BranchRelationFileFactory branchRelationFileFactory;

  @Inject
  public GitMacheteRepository(
      GitCoreRepositoryFactory gitCoreRepositoryFactory,
      BranchRelationFileFactory branchRelationFileFactory,
      @Assisted Path pathToRepoRoot,
      @Assisted Optional<String> repositoryName)
      throws GitMacheteException, GitException {
    this.gitCoreRepositoryFactory = gitCoreRepositoryFactory;
    this.branchRelationFileFactory = branchRelationFileFactory;

    this.pathToRepoRoot = pathToRepoRoot;
    this.repositoryName = repositoryName;

    this.repo = gitCoreRepositoryFactory.create(pathToRepoRoot);
    this.pathToMacheteFile = this.repo.getGitFolderPath().resolve("machete");

    try {
      macheteFile = branchRelationFileFactory.create(this.pathToMacheteFile);
    } catch (BranchRelationFileException e) {
      throw new MacheteFileParseException(
          MessageFormat.format(
              "Error occur while parsing machete file: {0}", this.pathToMacheteFile.toString()),
          e);
    }

    processMacheteEntries(macheteFile.getRootBranches(), Optional.empty());
  }

  private void processMacheteEntries(
      List<IBranchRelationFileBranchEntry> entries, Optional<GitMacheteBranch> upstream)
      throws GitMacheteException, GitException {
    GitMacheteBranch branch;
    Optional<IGitCoreLocalBranch> coreBranch;
    for (var entry : entries) {
      coreBranch = getCoreBranchFromName(entry.getName());
      if (coreBranch.isEmpty())
        throw new GitMacheteException(
            MessageFormat.format(
                "Branch \"{0}\" defined in machete file ({1}) does not exists in repository",
                entry.getName(), pathToMacheteFile.toString()));

      branch = new GitMacheteBranch(coreBranch.get(), this);
      branch.customAnnotation = entry.getCustomAnnotation();

      if (upstream.isEmpty()) {
        branch.upstreamBranch = Optional.empty();
        rootBranches.add(branch);
      } else {
        branch.upstreamBranch = Optional.of(upstream.get());
        upstream.get().childBranches.add(branch);
      }

      processMacheteEntries(entry.getSubbranches(), Optional.of(branch));
    }
  }

  // Return empty if branch does not exists in repo
  private Optional<IGitCoreLocalBranch> getCoreBranchFromName(String branchName) {
    try {
      IGitCoreLocalBranch coreLocalBranch = this.repo.getLocalBranch(branchName);
      return Optional.of(coreLocalBranch);
    } catch (GitException e) {
      return Optional.empty();
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
    else {
      try {
        return Optional.of(new GitMacheteBranch(branch.get(), this));
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
  public List<IGitMacheteSubmoduleEntry> getSubmodules() throws GitMacheteException {
    List<IGitMacheteSubmoduleEntry> submodules = new LinkedList<>();
    List<IGitCoreSubmoduleEntry> subs;

    try {
      subs = this.repo.getSubmodules();
    } catch (GitException e) {
      throw new GitMacheteJGitException("Error while getting submodules", e);
    }

    submodules =
        subs.stream().map(this::convertToGitMacheteSubmoduleEntry).collect(Collectors.toList());

    return submodules;
  }

  private IGitMacheteSubmoduleEntry convertToGitMacheteSubmoduleEntry(IGitCoreSubmoduleEntry m) {
    return new GitMacheteSubmoduleEntry(m.getPath(), m.getName());
  }
}
