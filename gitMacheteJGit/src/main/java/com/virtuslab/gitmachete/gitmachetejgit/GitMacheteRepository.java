package com.virtuslab.gitmachete.gitmachetejgit;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.virtuslab.branchrelationfile.api.BranchRelationFileException;
import com.virtuslab.branchrelationfile.api.BranchRelationFileFactory;
import com.virtuslab.branchrelationfile.api.IBranchRelationFile;
import com.virtuslab.branchrelationfile.api.IBranchRelationFileEntry;
import com.virtuslab.gitcore.gitcoreapi.GitCoreRepositoryFactory;
import com.virtuslab.gitcore.gitcoreapi.GitException;
import com.virtuslab.gitcore.gitcoreapi.IGitCoreBranch;
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
import javax.annotation.Nullable;
import lombok.Getter;

public class GitMacheteRepository implements IGitMacheteRepository {
  private final String repositoryName;

  private final IGitCoreRepository repo;

  @Getter private final List<IGitMacheteBranch> rootBranches = new LinkedList<>();

  @Getter private final Path pathToRepoRoot;
  @Getter private final Path pathToBranchRelationFile;
  @Getter private final IBranchRelationFile branchRelationFile;

  private IGitMacheteBranch currentBranch = null;
  private final IGitCoreBranch currentCoreBranch;

  private final GitCoreRepositoryFactory gitCoreRepositoryFactory;

  private final BranchRelationFileFactory branchRelationFileFactory;

  @Inject
  public GitMacheteRepository(
      GitCoreRepositoryFactory gitCoreRepositoryFactory,
      BranchRelationFileFactory branchRelationFileFactory,
      @Assisted Path pathToRepoRoot,
      @Assisted @Nullable String repositoryName)
      throws GitMacheteException, GitException {
    this(
        gitCoreRepositoryFactory,
        branchRelationFileFactory,
        pathToRepoRoot,
        repositoryName,
        /*givenBranchRelationFile*/ null);
  }

  private GitMacheteRepository(
      GitCoreRepositoryFactory gitCoreRepositoryFactory,
      BranchRelationFileFactory branchRelationFileFactory,
      Path pathToRepoRoot,
      String repositoryName,
      IBranchRelationFile givenBranchRelationFile)
      throws GitMacheteException, GitException {
    this.gitCoreRepositoryFactory = gitCoreRepositoryFactory;
    this.branchRelationFileFactory = branchRelationFileFactory;

    this.pathToRepoRoot = pathToRepoRoot;
    this.repositoryName = repositoryName;

    this.repo = gitCoreRepositoryFactory.create(pathToRepoRoot);
    this.pathToBranchRelationFile = this.repo.getGitFolderPath().resolve("machete");

    var currentCoreBranchOptional = repo.getCurrentBranch();
    if (currentCoreBranchOptional.isPresent()) {
      currentCoreBranch = currentCoreBranchOptional.get();
    } else {
      currentCoreBranch = null;
    }

    if (givenBranchRelationFile == null) {
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
      branchRelationFile = givenBranchRelationFile;
    }

    for (var entry : branchRelationFile.getRootBranches()) {
      var branch = createMacheteBranchOrThrowException(entry, /*upstreamBranch*/ null);
      rootBranches.add(branch);
      processSubtree(branch, entry.getSubbranches());
    }
  }

  @Override
  public Optional<String> getRepositoryName() {
    return Optional.ofNullable(repositoryName);
  }

  private GitMacheteBranch createMacheteBranchOrThrowException(
      IBranchRelationFileEntry branchEntry, IGitMacheteBranch upstreamBranch)
      throws GitMacheteException, GitException {
    Optional<IGitCoreLocalBranch> coreBranch = getCoreBranchFromName(branchEntry.getName());
    if (coreBranch.isEmpty()) {
      throw new GitMacheteException(
          MessageFormat.format(
              "Branch \"{0}\" defined in machete file ({1}) does not exist in repository",
              branchEntry.getName(), pathToBranchRelationFile.toString()));
    }

    var branch =
        new GitMacheteBranch(
            coreBranch.get(), branchEntry.getCustomAnnotation().orElse(null), upstreamBranch);

    if (coreBranch.get().equals(currentCoreBranch)) {
      currentBranch = branch;
    }

    return branch;
  }

  private void processSubtree(
      GitMacheteBranch subtreeRoot, List<IBranchRelationFileEntry> directDownstreamEntries)
      throws GitMacheteException, GitException {
    for (var entry : directDownstreamEntries) {
      var branch = createMacheteBranchOrThrowException(entry, subtreeRoot);

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
  public Optional<IGitMacheteBranch> getCurrentBranchIfManaged() {
    return Optional.ofNullable(currentBranch);
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
        branchRelationFile);
  }

  private IGitMacheteSubmoduleEntry convertToGitMacheteSubmoduleEntry(IGitCoreSubmoduleEntry m) {
    return new GitMacheteSubmoduleEntry(m.getPath(), m.getName());
  }
}
