package com.virtuslab.gitmachete.gitmachetejgit;

import com.virtuslab.branchrelationfile.api.IBranchRelationFile;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitcore.api.IGitCoreSubmoduleEntry;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteJGitException;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteSubmoduleEntry;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Getter;

public class GitMacheteRepository implements IGitMacheteRepository {
  private final String repositoryName;

  private final IGitCoreRepository gitCoreRepository;

  @Getter private final List<IGitMacheteBranch> rootBranches;

  @Getter private final Path pathToRepoRoot;
  @Getter private final Path pathToBranchRelationFile;
  @Getter private final IBranchRelationFile branchRelationFile;

  private IGitMacheteBranch currentBranch = null;

  private final Map<String, IGitMacheteBranch> branchByName;

  GitMacheteRepository(GitMacheteRepositoryBuilder builder) {
    this.repositoryName = builder.getRepositoryName();
    this.gitCoreRepository = builder.getGitCoreRepository();
    this.rootBranches = builder.getRootBranches();
    this.pathToRepoRoot = builder.getPathToRepoRoot();
    this.pathToBranchRelationFile = builder.getPathToBranchRelationFile();
    this.branchRelationFile = builder.getBranchRelationFile();
    this.currentBranch = builder.getCurrentBranch();
    this.branchByName = builder.getBranchByName();
  }

  @Override
  public Optional<String> getRepositoryName() {
    return Optional.ofNullable(repositoryName);
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
      subs = this.gitCoreRepository.getSubmodules();
    } catch (GitCoreException e) {
      throw new GitMacheteJGitException("Error while getting submodules", e);
    }

    return subs.stream().map(this::convertToGitMacheteSubmoduleEntry).collect(Collectors.toList());
  }

  private IGitMacheteSubmoduleEntry convertToGitMacheteSubmoduleEntry(IGitCoreSubmoduleEntry m) {
    return new GitMacheteSubmoduleEntry(m.getPath(), m.getName());
  }
}
