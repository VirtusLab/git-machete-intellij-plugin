package com.virtuslab.gitmachete.gitmachetejgit;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.virtuslab.branchrelationfile.api.BranchRelationFileException;
import com.virtuslab.branchrelationfile.api.BranchRelationFileFactory;
import com.virtuslab.branchrelationfile.api.IBranchRelationFile;
import com.virtuslab.branchrelationfile.api.IBranchRelationFileEntry;
import com.virtuslab.gitcore.gitcoreapi.*;
import com.virtuslab.gitmachete.gitmacheteapi.*;
import java.io.IOException;
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
  private IBranchRelationFile branchRelationFile;

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
      branchRelationFile = branchRelationFileFactory.create(this.pathToMacheteFile);
    } catch (BranchRelationFileException e) {
      throw new MacheteFileParseException(
          e.getErrorLine().isEmpty()
              ? MessageFormat.format(
                  "Error occurred while parsing machete file: {0}",
                  this.pathToMacheteFile.toString())
              : MessageFormat.format(
                  "Error occurred while parsing machete file on line {1}: {0}",
                  this.pathToMacheteFile.toString(), e.getErrorLine().get()),
          e);
    }

    for (var entry : branchRelationFile.getRootBranches()) {
      var branch = createMacheteBranchOrThrowException(entry);
      branch.upstreamBranch = Optional.empty();
      rootBranches.add(branch);
      processSubtree(branch, entry.getSubbranches());
    }
  }

  private GitMacheteBranch createMacheteBranchOrThrowException(IBranchRelationFileEntry branchEntry)
      throws GitMacheteException, GitException {
    Optional<IGitCoreLocalBranch> coreBranch = getCoreBranchFromName(branchEntry.getName());
    if (coreBranch.isEmpty())
      throw new GitMacheteException(
          MessageFormat.format(
              "Branch \"{0}\" defined in machete file ({1}) does not exist in repository",
              branchEntry.getName(), pathToMacheteFile.toString()));
    var branch = new GitMacheteBranch(coreBranch.get(), this);
    branch.customAnnotation = branchEntry.getCustomAnnotation();

    return branch;
  }

  private void processSubtree(
      GitMacheteBranch subtreeRoot, List<IBranchRelationFileEntry> directDownstreamEntries)
      throws GitMacheteException, GitException {
    for (var entry : directDownstreamEntries) {
      var branch = createMacheteBranchOrThrowException(entry);

      branch.upstreamBranch = Optional.of(subtreeRoot);
      subtreeRoot.childBranches.add(branch);

      processSubtree(branch, entry.getSubbranches());
    }
  }

  // Return empty if branch does not exist in repo
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

  @Override
  public IGitMacheteRepository slideOutBranchWithReinstantiationOfMacheteRepository(
      String branchName) throws GitMacheteException, GitException {
    var macheteFile = getBranchRelationFile();
    var macheteFileBranch = macheteFile.findBranchByName(branchName);
    if (macheteFileBranch.isEmpty())
      throw new GitMacheteException(
          MessageFormat.format(
              "Branch {0} was not found in machete file, so can not be slided out", branchName));

    try {
      macheteFileBranch.get().slideOut();
      macheteFile.saveToFile(true);
    } catch (BranchRelationFileException | IOException e) {
      throw new GitMacheteException(
          MessageFormat.format(
              "Error occurred while sliding out branch {0}: {1}", branchName, e.getMessage()),
          e);
    }

    return new GitMacheteRepository(
        gitCoreRepositoryFactory, branchRelationFileFactory, pathToRepoRoot, repositoryName);
  }

  @Override
  public IGitMacheteRepository slideOutBranchWithReinstantiationOfMacheteRepository(
      IGitMacheteBranch branch) throws GitMacheteException, GitException {
    return slideOutBranchWithReinstantiationOfMacheteRepository(branch.getName());
  }
}
