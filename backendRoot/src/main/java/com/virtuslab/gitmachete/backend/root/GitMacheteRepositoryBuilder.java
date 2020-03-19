package com.virtuslab.gitmachete.backend.root;

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

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import io.vavr.control.Try;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.IBranchLayoutEntry;
import com.virtuslab.branchlayout.impl.BranchLayoutFileParser;
import com.virtuslab.gitcore.api.IGitCoreBranch;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitcore.api.IGitCoreSubmoduleEntry;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.GitMacheteJGitException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteSubmoduleEntry;
import com.virtuslab.gitmachete.backend.api.MacheteFileParseException;
import com.virtuslab.gitmachete.backend.impl.GitMacheteBranch;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepository;
import com.virtuslab.gitmachete.backend.impl.GitMacheteSubmoduleEntry;

@Accessors(chain = true, fluent = true)
@Getter(AccessLevel.PACKAGE)
public class GitMacheteRepositoryBuilder implements IGitMacheteRepositoryBuilder {
  private IGitCoreRepository gitCoreRepository;
  @Nullable
  private IGitMacheteBranch currentBranch = null;
  @Nullable
  private IGitCoreBranch currentCoreBranch;
  private final Map<String, IGitMacheteBranch> branchByName = new HashMap<>();

  private final GitCoreRepositoryFactory gitCoreRepositoryFactory;

  private final Path pathToRepoRoot;
  @Setter
  @Nullable
  private String repositoryName = null;
  @Setter
  @MonotonicNonNull
  private IBranchLayout branchLayout = null;

  @Inject
  public GitMacheteRepositoryBuilder(
      GitCoreRepositoryFactory gitCoreRepositoryFactory,
      @Assisted Path pathToRepoRoot) {
    this.gitCoreRepositoryFactory = gitCoreRepositoryFactory;
    this.pathToRepoRoot = pathToRepoRoot;
  }

  public GitMacheteRepository build() throws GitMacheteException {
    List<IGitMacheteBranch> rootBranches = new LinkedList<>();

    gitCoreRepository = gitCoreRepositoryFactory.create(pathToRepoRoot);

    currentCoreBranch = Try.of(() -> gitCoreRepository.getCurrentBranch())
        .getOrElseThrow(e -> new GitMacheteException("Can't get current branch", e))
        .orElse(null);

    if (branchLayout == null) {
      Path pathToBranchLayoutFile = gitCoreRepository.getGitFolderPath().resolve("machete");
      branchLayout = Try.of(() -> new BranchLayoutFileParser(pathToBranchLayoutFile).parse())
          .getOrElseThrow(e -> new MacheteFileParseException(((BranchLayoutException) e).getErrorLine().isEmpty()
              ? MessageFormat.format("Error occurred while parsing machete file: {0}",
                  pathToBranchLayoutFile.toString())
              : MessageFormat.format("Error occurred while parsing machete file on line {0}: {1}",
                  ((BranchLayoutException) e).getErrorLine().get(), pathToBranchLayoutFile.toString()),
              e));
    }

    for (var entry : branchLayout.getRootBranches()) {
      var branch = createMacheteBranchOrThrowException(entry, /* upstreamBranch */ null);
      rootBranches.add(branch);
      branchByName.put(branch.getName(), branch);
      processSubtree(branch, entry.getSubbranches().asJava());
    }

    List<IGitMacheteSubmoduleEntry> macheteSubmodules = Try.of(() -> gitCoreRepository.getSubmodules())
        .getOrElseThrow(e -> new GitMacheteJGitException("Error while getting submodules", e))
        .map(this::convertToGitMacheteSubmoduleEntry)
        .collect(Collectors.toList());

    return new GitMacheteRepository(repositoryName, rootBranches, macheteSubmodules, branchLayout, currentBranch,
        branchByName);
  }

  private IGitMacheteSubmoduleEntry convertToGitMacheteSubmoduleEntry(IGitCoreSubmoduleEntry m) {
    return new GitMacheteSubmoduleEntry(m.getPath(), m.getName());
  }

  private GitMacheteBranch createMacheteBranchOrThrowException(IBranchLayoutEntry branchEntry,
      @Nullable IGitMacheteBranch upstreamBranch) throws GitMacheteException {
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

  private void processSubtree(GitMacheteBranch subtreeRoot, List<IBranchLayoutEntry> directDownstreamEntries)
      throws GitMacheteException {
    for (var entry : directDownstreamEntries) {
      var branch = createMacheteBranchOrThrowException(entry, subtreeRoot);

      subtreeRoot.getDownstreamBranches().add(branch);

      branchByName.put(branch.getName(), branch);

      processSubtree(branch, entry.getSubbranches().asJava());
    }
  }

  /**
   * @return Option of {@link IGitCoreLocalBranch} or if branch with given name doesn't exist returns empty Option
   */
  private Optional<IGitCoreLocalBranch> getCoreBranchFromName(String branchName) {
    return Try.of(() -> Optional.of(gitCoreRepository.getLocalBranch(branchName))).getOrElse(() -> Optional.empty());
  }
}
