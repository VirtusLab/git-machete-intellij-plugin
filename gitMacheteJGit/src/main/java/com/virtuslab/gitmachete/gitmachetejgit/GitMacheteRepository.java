package com.virtuslab.gitmachete.gitmachetejgit;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.virtuslab.branchrelationfile.api.BranchRelationFileException;
import com.virtuslab.branchrelationfile.api.BranchRelationFileFactory;
import com.virtuslab.branchrelationfile.api.IBranchRelationFile;
import com.virtuslab.branchrelationfile.api.IBranchRelationFileEntry;
import com.virtuslab.gitcore.gitcoreapi.GitCoreRepositoryFactory;
import com.virtuslab.gitcore.gitcoreapi.GitException;
import com.virtuslab.gitcore.gitcoreapi.IGitCoreLocalBranch;
import com.virtuslab.gitcore.gitcoreapi.IGitCoreRepository;
import com.virtuslab.gitcore.gitcoreapi.IGitCoreSubmoduleEntry;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteJGitException;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteSubmoduleEntry;
import com.virtuslab.gitmachete.gitmacheteapi.MacheteFileParseException;
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
  private Path pathToBranchRelationFile;
  private IBranchRelationFile branchRelationFile;

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
    this(
        gitCoreRepositoryFactory,
        branchRelationFileFactory,
        pathToRepoRoot,
        repositoryName,
        Optional.empty());
  }

  private GitMacheteRepository(
      GitCoreRepositoryFactory gitCoreRepositoryFactory,
      BranchRelationFileFactory branchRelationFileFactory,
      Path pathToRepoRoot,
      Optional<String> repositoryName,
      Optional<IBranchRelationFile> givenBranchRelationFile)
      throws GitMacheteException, GitException {
    this.gitCoreRepositoryFactory = gitCoreRepositoryFactory;
    this.branchRelationFileFactory = branchRelationFileFactory;

    this.pathToRepoRoot = pathToRepoRoot;
    this.repositoryName = repositoryName;

    this.repo = gitCoreRepositoryFactory.create(pathToRepoRoot);
    this.pathToBranchRelationFile = this.repo.getGitFolderPath().resolve("machete");

    if (givenBranchRelationFile.isEmpty()) {
      try {
        branchRelationFile = branchRelationFileFactory.create(this.pathToBranchRelationFile);
      } catch (BranchRelationFileException e) {
        throw new MacheteFileParseException(
            e.getErrorLine().isEmpty()
                ? MessageFormat.format(
                    "Error occurred while parsing machete file: {0}",
                    this.pathToBranchRelationFile.toString())
                : MessageFormat.format(
                    "Error occurred while parsing machete file on line {1}: {0}",
                    this.pathToBranchRelationFile.toString(), e.getErrorLine().get()),
            e);
      }
    } else {
      branchRelationFile = givenBranchRelationFile.get();
    }

    for (var entry : branchRelationFile.getRootBranches()) {
      var branch = createMacheteBranchOrThrowException(entry, Optional.empty());
      rootBranches.add(branch);
      processSubtree(branch, entry.getSubbranches());
    }
  }

  private GitMacheteBranch createMacheteBranchOrThrowException(
      IBranchRelationFileEntry branchEntry, Optional<IGitMacheteBranch> upstreamBranch)
      throws GitMacheteException, GitException {
    Optional<IGitCoreLocalBranch> coreBranch = getCoreBranchFromName(branchEntry.getName());
    if (coreBranch.isEmpty()) {
      throw new GitMacheteException(
          MessageFormat.format(
              "Branch \"{0}\" defined in machete file ({1}) does not exist in repository",
              branchEntry.getName(), pathToBranchRelationFile.toString()));
    }
    var branch =
        new GitMacheteBranch(coreBranch.get(), branchEntry.getCustomAnnotation(), upstreamBranch);

    return branch;
  }

  private void processSubtree(
      GitMacheteBranch subtreeRoot, List<IBranchRelationFileEntry> directDownstreamEntries)
      throws GitMacheteException, GitException {
    for (var entry : directDownstreamEntries) {
      var branch = createMacheteBranchOrThrowException(entry, Optional.of(subtreeRoot));

      subtreeRoot.getDownstreamBranches().add(branch);

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

    if (branch.isEmpty()) {
      return Optional.empty();
    } else {
      try {
        return Optional.of(new GitMacheteBranch(branch.get()));
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

  @Override
  public IGitMacheteRepository withBranchRelationFile(IBranchRelationFile branchRelationFile)
      throws GitException, GitMacheteException {
    return new GitMacheteRepository(
        gitCoreRepositoryFactory,
        branchRelationFileFactory,
        pathToRepoRoot,
        repositoryName,
        Optional.ofNullable(branchRelationFile));
  }

  private IGitMacheteSubmoduleEntry convertToGitMacheteSubmoduleEntry(IGitCoreSubmoduleEntry m) {
    return new GitMacheteSubmoduleEntry(m.getPath(), m.getName());
  }
}
