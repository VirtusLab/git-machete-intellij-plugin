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
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepositoryBuilder;
import com.virtuslab.gitmachete.gitmacheteapi.MacheteFileParseException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@Getter(AccessLevel.PACKAGE)
public class GitMacheteRepositoryBuilder implements IGitMacheteRepositoryBuilder {
  @Setter private String repositoryName = null;

  private IGitCoreRepository gitCoreRepository;

  private final List<IGitMacheteBranch> rootBranches = new LinkedList<>();

  private final Path pathToRepoRoot;
  private Path pathToBranchRelationFile;
  @Setter private IBranchRelationFile branchRelationFile = null;

  private IGitMacheteBranch currentBranch = null;
  private IGitCoreBranch currentCoreBranch;

  private final Map<String, IGitMacheteBranch> branchByName = new HashMap<>();

  private final GitCoreRepositoryFactory gitCoreRepositoryFactory;

  private final BranchRelationFileFactory branchRelationFileFactory;

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
    this.gitCoreRepository = gitCoreRepositoryFactory.create(pathToRepoRoot);
    this.pathToBranchRelationFile = this.gitCoreRepository.getGitFolderPath().resolve("machete");

    Optional<IGitCoreLocalBranch> currentCoreBranchOptional = null;
    try {
      currentCoreBranchOptional = gitCoreRepository.getCurrentBranch();
    } catch (GitCoreException e) {
      throw new GitMacheteException("Can't get current branch", e);
    }
    currentCoreBranch = currentCoreBranchOptional.orElse(null);

    if (branchRelationFile == null) {
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
    }

    for (var entry : branchRelationFile.getRootBranches()) {
      var branch = createMacheteBranchOrThrowException(entry, /*upstreamBranch*/ null);
      rootBranches.add(branch);
      branchByName.put(branch.getName(), branch);
      processSubtree(branch, entry.getSubbranches());
    }

    return new GitMacheteRepository(this);
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
    var branch = new GitMacheteBranch(coreBranch.get(), upstreamBranch, customAnnotation);

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
      IGitCoreLocalBranch coreLocalBranch = this.gitCoreRepository.getLocalBranch(branchName);
      return Optional.of(coreLocalBranch);
    } catch (GitCoreException e) {
      return Optional.empty();
    }
  }
}
