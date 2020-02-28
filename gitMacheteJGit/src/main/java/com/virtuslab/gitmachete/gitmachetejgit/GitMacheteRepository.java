package com.virtuslab.gitmachete.gitmachetejgit;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.virtuslab.branchrelationfile.api.BranchRelationFileException;
import com.virtuslab.branchrelationfile.api.BranchRelationFileFactory;
import com.virtuslab.branchrelationfile.api.IBranchRelationFile;
import com.virtuslab.branchrelationfile.api.IBranchRelationFileEntry;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.GitCoreRepositoryFactory;
import com.virtuslab.gitcore.api.IGitCoreBranch;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitcore.api.IGitCoreSubmoduleEntry;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteJGitException;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteSubmoduleEntry;
import com.virtuslab.gitmachete.gitmacheteapi.MacheteFileParseException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

  private final Map<String, IGitMacheteBranch> branchByName = new HashMap<>();

  @Inject
  public GitMacheteRepository(
      GitCoreRepositoryFactory gitCoreRepositoryFactory,
      BranchRelationFileFactory branchRelationFileFactory,
      @Assisted Path pathToRepoRoot,
      @Assisted @Nullable String repositoryName)
      throws GitMacheteException, GitCoreException {
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
      throws GitMacheteException, GitCoreException {

    this.pathToRepoRoot = pathToRepoRoot;
    this.repositoryName = repositoryName;

    this.repo = gitCoreRepositoryFactory.create(pathToRepoRoot);
    this.pathToBranchRelationFile = this.repo.getGitFolderPath().resolve("machete");

    var currentCoreBranchOptional = repo.getCurrentBranch();
    currentCoreBranch = currentCoreBranchOptional.orElse(null);

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
      branchByName.put(branch.getName(), branch);
      processSubtree(branch, entry.getSubbranches());
    }
  }

  @Override
  public Optional<String> getRepositoryName() {
    return Optional.ofNullable(repositoryName);
  }

  private GitMacheteBranch createMacheteBranchOrThrowException(
      IBranchRelationFileEntry branchEntry, IGitMacheteBranch upstreamBranch)
      throws GitMacheteException {
    Optional<IGitCoreLocalBranch> coreBranch = getCoreBranchFromName(branchEntry.getName());
    if (coreBranch.isEmpty()) {
      throw new GitMacheteException(
          MessageFormat.format(
              "Branch \"{0}\" defined in machete file ({1}) does not exist in repository",
              branchEntry.getName(), pathToBranchRelationFile.toString()));
    }

    String customAnnotation = branchEntry.getCustomAnnotation().orElse(null);
    var branch = new GitMacheteBranch(coreBranch.get(), upstreamBranch, customAnnotation, repo);

    if (coreBranch.get().equals(currentCoreBranch)) {
      currentBranch = branch;
    }

    return branch;
  }

  private void processSubtree(
      GitMacheteBranch subtreeRoot, List<IBranchRelationFileEntry> directDownstreamEntries)
      throws GitMacheteException {
    for (var entry : directDownstreamEntries) {
      var branch = createMacheteBranchOrThrowException(entry, subtreeRoot);

      subtreeRoot.getDownstreamBranches().add(branch);

      branchByName.put(branch.getName(), branch);

      processSubtree(branch, entry.getSubbranches());
    }
  }

  // Return empty if branch does not exist in repo
  private Optional<IGitCoreLocalBranch> getCoreBranchFromName(String branchName) {
    try {
      IGitCoreLocalBranch coreLocalBranch = this.repo.getLocalBranch(branchName);
      return Optional.of(coreLocalBranch);
    } catch (GitCoreException e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<IGitMacheteBranch> getCurrentBranchIfManaged() {
    return Optional.ofNullable(currentBranch);
  }

  @Override
  public Optional<IGitMacheteBranch> getBranchByName(String branchName) {
    return Optional.ofNullable(branchByName.getOrDefault(branchName, null));
  }

  @Override
  public List<IGitMacheteSubmoduleEntry> getSubmodules() throws GitMacheteException {
    List<IGitCoreSubmoduleEntry> subs;

    try {
      subs = this.repo.getSubmodules();
    } catch (GitCoreException e) {
      throw new GitMacheteJGitException("Error while getting submodules", e);
    }

    return subs.stream().map(this::convertToGitMacheteSubmoduleEntry).collect(Collectors.toList());
  }

  private IGitMacheteSubmoduleEntry convertToGitMacheteSubmoduleEntry(IGitCoreSubmoduleEntry m) {
    return new GitMacheteSubmoduleEntry(m.getPath(), m.getName());
  }
}
