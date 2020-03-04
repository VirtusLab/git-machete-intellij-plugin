package com.virtuslab.gitmachete.backendroot;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import com.virtuslab.branchrelationfile.api.BranchRelationFileException;
import com.virtuslab.branchrelationfile.api.IBranchRelationFile;
import com.virtuslab.branchrelationfile.api.IBranchRelationFileEntry;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreBranch;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitcore.api.IGitCoreSubmoduleEntry;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteJGitException;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteSubmoduleEntry;
import com.virtuslab.gitmachete.gitmacheteapi.MacheteFileParseException;
import com.virtuslab.gitmachete.gitmachetejgit.GitMacheteBranch;
import com.virtuslab.gitmachete.gitmachetejgit.GitMacheteRepository;
import com.virtuslab.gitmachete.gitmachetejgit.GitMacheteSubmoduleEntry;

@Accessors(chain = true, fluent = true)
@Getter(AccessLevel.PACKAGE)
public class GitMacheteRepositoryBuilder implements IGitMacheteRepositoryBuilder {
  private IGitCoreRepository gitCoreRepository;
  private IGitMacheteBranch currentBranch = null;
  private IGitCoreBranch currentCoreBranch;
  private final Map<String, IGitMacheteBranch> branchByName = new HashMap<>();

  private final GitCoreRepositoryFactory gitCoreRepositoryFactory;
  private final BranchRelationFileFactory branchRelationFileFactory;

  private final Path pathToRepoRoot;
  @Setter
  private String repositoryName = null;
  @Setter
  private IBranchRelationFile branchRelationFile = null;

  @Inject
  public GitMacheteRepositoryBuilder(
      GitCoreRepositoryFactory gitCoreRepositoryFactory,
      BranchRelationFileFactory branchRelationFileFactory,
      @Assisted Path pathToRepoRoot) {
    this.gitCoreRepositoryFactory = gitCoreRepositoryFactory;
    this.branchRelationFileFactory = branchRelationFileFactory;
    this.pathToRepoRoot = pathToRepoRoot;
  }

  public GitMacheteRepository build() throws GitMacheteException {
    List<IGitMacheteBranch> rootBranches = new LinkedList<>();

    gitCoreRepository = gitCoreRepositoryFactory.create(pathToRepoRoot);
    Path pathToBranchRelationFile = gitCoreRepository.getGitFolderPath().resolve("machete");

    Optional<IGitCoreLocalBranch> currentCoreBranchOptional = null;
    try {
      currentCoreBranchOptional = gitCoreRepository.getCurrentBranch();
    } catch (GitCoreException e) {
      throw new GitMacheteException("Can't get current branch", e);
    }
    currentCoreBranch = currentCoreBranchOptional.orElse(null);

    if (branchRelationFile == null) {
      try {
        branchRelationFile = branchRelationFileFactory.create(pathToBranchRelationFile);
      } catch (BranchRelationFileException e) {
        throw new MacheteFileParseException(e.getErrorLine().isEmpty()
            ? MessageFormat.format("Error occurred while parsing machete file: {0}",
                pathToBranchRelationFile.toString())
            : MessageFormat.format("Error occurred while parsing machete file on line {1}: {0}",
                pathToBranchRelationFile.toString(), e.getErrorLine().get()),
            e);
      }
    }

    for (var entry : branchRelationFile.getRootBranches()) {
      var branch = createMacheteBranchOrThrowException(entry, /* upstreamBranch */ null);
      rootBranches.add(branch);
      branchByName.put(branch.getName(), branch);
      processSubtree(branch, entry.getSubbranches());
    }

    List<IGitCoreSubmoduleEntry> coreSubmodules;

    try {
      coreSubmodules = gitCoreRepository.getSubmodules();
    } catch (GitCoreException e) {
      throw new GitMacheteJGitException("Error while getting submodules", e);
    }

    List<IGitMacheteSubmoduleEntry> macheteSubmodules = coreSubmodules.stream()
        .map(this::convertToGitMacheteSubmoduleEntry).collect(Collectors.toList());

    return new GitMacheteRepository(repositoryName, rootBranches, macheteSubmodules, branchRelationFile, currentBranch,
        branchByName);
  }

  private IGitMacheteSubmoduleEntry convertToGitMacheteSubmoduleEntry(IGitCoreSubmoduleEntry m) {
    return new GitMacheteSubmoduleEntry(m.getPath(), m.getName());
  }

  private GitMacheteBranch createMacheteBranchOrThrowException(IBranchRelationFileEntry branchEntry,
      IGitMacheteBranch upstreamBranch) throws GitMacheteException {
    Optional<IGitCoreLocalBranch> coreBranch = getCoreBranchFromName(branchEntry.getName());
    if (coreBranch.isEmpty()) {
      throw new GitMacheteException(MessageFormat
          .format("Branch \"{0}\" defined in machete file does not exist in repository", branchEntry.getName()));
    }

    String customAnnotation = branchEntry.getCustomAnnotation().orElse(null);
    var branch = new GitMacheteBranch(coreBranch.get(), upstreamBranch, customAnnotation, gitCoreRepository);

    if (coreBranch.get().equals(currentCoreBranch)) {
      currentBranch = branch;
    }

    return branch;
  }

  private void processSubtree(GitMacheteBranch subtreeRoot, List<IBranchRelationFileEntry> directDownstreamEntries)
      throws GitMacheteException {
    for (var entry : directDownstreamEntries) {
      var branch = createMacheteBranchOrThrowException(entry, subtreeRoot);

      subtreeRoot.getDownstreamBranches().add(branch);

      branchByName.put(branch.getName(), branch);

      processSubtree(branch, entry.getSubbranches());
    }
  }

  /**
   * @return Optional of {@link IGitCoreLocalBranch} or if branch with given name doesn't exist returns empty Optional
   */
  private Optional<IGitCoreLocalBranch> getCoreBranchFromName(String branchName) {
    try {
      IGitCoreLocalBranch coreLocalBranch = gitCoreRepository.getLocalBranch(branchName);
      return Optional.of(coreLocalBranch);
    } catch (GitCoreException e) {
      return Optional.empty();
    }
  }
}
