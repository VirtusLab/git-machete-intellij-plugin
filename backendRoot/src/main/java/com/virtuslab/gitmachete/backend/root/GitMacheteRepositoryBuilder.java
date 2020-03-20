package com.virtuslab.gitmachete.backend.root;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import io.vavr.collection.List;
import io.vavr.control.Try;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.IBranchLayoutEntry;
import com.virtuslab.branchlayout.impl.BranchLayoutFileParser;
import com.virtuslab.gitcore.api.BaseGitCoreCommit;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreBranch;
import com.virtuslab.gitcore.api.IGitCoreBranchTrackingStatus;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitcore.api.IGitCoreSubmoduleEntry;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.GitMacheteJGitException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteSubmoduleEntry;
import com.virtuslab.gitmachete.backend.api.MacheteFileParseException;
import com.virtuslab.gitmachete.backend.api.SyncToOriginStatus;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.impl.GitMacheteBranch;
import com.virtuslab.gitmachete.backend.impl.GitMacheteCommit;
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

  private final Map<String, GitMacheteBranchData> branchNameToGitMacheteBranchData = new TreeMap<>();

  @Inject
  public GitMacheteRepositoryBuilder(
      GitCoreRepositoryFactory gitCoreRepositoryFactory,
      @Assisted Path pathToRepoRoot) {
    this.gitCoreRepositoryFactory = gitCoreRepositoryFactory;
    this.pathToRepoRoot = pathToRepoRoot;
  }

  public GitMacheteRepository build() throws GitMacheteException {
    gitCoreRepository = gitCoreRepositoryFactory.create(pathToRepoRoot);

    currentCoreBranch = Try.of(() -> gitCoreRepository.getCurrentBranch())
        .getOrElseThrow(e -> new GitMacheteException("Can't get current branch", e))
        .orElse(null);

    if (branchLayout == null) {
      Path pathToBranchLayoutFile = gitCoreRepository.getGitFolderPath().resolve("machete");
      branchLayout = Try.of(() -> new BranchLayoutFileParser(pathToBranchLayoutFile).parse())
          .getOrElseThrow(e -> {
            var errorLine = ((BranchLayoutException) e).getErrorLine();
            return new MacheteFileParseException(errorLine.isPresent()
                ? MessageFormat.format("Error occurred while parsing machete file {0} in line {1}",
                    pathToBranchLayoutFile.toString(), errorLine.get())
                : MessageFormat.format("Error occurred while parsing machete file {0}",
                    pathToBranchLayoutFile.toString()),
                e);
          });
    }

    verifyBranchLayoutEntriesAndPrepareGitMacheteBranchData(/* parentEntryCoreLocalBranch */ null,
        branchLayout.getRootBranches());

    List<IGitMacheteBranch> macheteRootBranches = branchLayout.getRootBranches()
        .map(entry -> createMacheteBranch(entry, deriveDownstreamBranches(entry.getSubbranches())))
        .peek(branch -> branchByName.put(branch.getName(), branch));

    List<IGitMacheteSubmoduleEntry> macheteSubmodules = Try.of(() -> gitCoreRepository.getSubmodules())
        .getOrElseThrow(e -> new GitMacheteJGitException("Error while getting submodules", e))
        .map(this::convertToGitMacheteSubmoduleEntry)
        .collect(List.collector());

    return new GitMacheteRepository(repositoryName, macheteRootBranches, macheteSubmodules, branchLayout, currentBranch,
        branchByName);
  }

  private IGitMacheteSubmoduleEntry convertToGitMacheteSubmoduleEntry(IGitCoreSubmoduleEntry m) {
    return new GitMacheteSubmoduleEntry(m.getPath(), m.getName());
  }

  private IGitMacheteBranch createMacheteBranch(IBranchLayoutEntry branchEntry,
      List<IGitMacheteBranch> downstreamBranches) throws GitMacheteException {
    Optional<IGitCoreLocalBranch> coreBranch = getCoreBranchFromName(branchEntry.getName());
    if (!coreBranch.isPresent()) {
      throw new GitMacheteException(MessageFormat
          .format("Branch \"{0}\" defined in machete file does not exist in repository", branchEntry.getName()));
    }

    String customAnnotation = branchEntry.getCustomAnnotation().orElse(null);

    GitMacheteBranchData gitMacheteBranchData = branchNameToGitMacheteBranchData.get(branchEntry.getName());

    var branch = new GitMacheteBranch(coreBranch.get(), coreBranch.get().getName(), customAnnotation,
        downstreamBranches,
        gitMacheteBranchData.pointedCommit,
        gitMacheteBranchData.commits,
        gitMacheteBranchData.syncToOriginStatus,
        gitMacheteBranchData.syncToParentStatus);

    if (coreBranch.get().equals(currentCoreBranch)) {
      currentBranch = branch;
    }

    return branch;
  }

  /**
   * Recursively verifies whether all branches entries of the provided list exists within {@code coreRepository}.
   * Additionally some git machete branch data is collected and stored. Those operations are performed in advance to
   * simplify further process of a machete repository construction.
   */
  private void verifyBranchLayoutEntriesAndPrepareGitMacheteBranchData(IGitCoreLocalBranch parentEntryCoreLocalBranch,
      List<IBranchLayoutEntry> entries)
      throws GitMacheteException {
    for (var entry : entries) {
      Optional<IGitCoreLocalBranch> coreBranch = getCoreBranchFromName(entry.getName());
      if (coreBranch.isEmpty()) {
        throw new GitMacheteException(MessageFormat
            .format("Branch \"{0}\" defined in machete file does not exist in repository", entry.getName()));
      } else {

        try {
          Optional<BaseGitCoreCommit> forkPoint = coreBranch.get().computeForkPoint();

          // translate IGitCoreCommit list to IGitMacheteCommit list
          List<IGitMacheteCommit> commits = forkPoint.isEmpty()
              ? List.empty()
              : coreBranch.get().computeCommitsUntil(forkPoint.get())
                  .map(GitMacheteCommit::new)
                  .collect(List.collector());

          var pointedCommit = new GitMacheteCommit(coreBranch.get().getPointedCommit());
          var syncToOriginStatus = computeSyncToOriginStatus(coreBranch.get());
          var syncToParentStatus = computeSyncToParentStatus(coreBranch.get(), parentEntryCoreLocalBranch);

          var gitMacheteBranchData = GitMacheteBranchData.of(pointedCommit, commits, syncToOriginStatus,
              syncToParentStatus);

          branchNameToGitMacheteBranchData.put(entry.getName(), gitMacheteBranchData);

        } catch (GitCoreException e) {
          throw new GitMacheteException(e);
        }

      }

      verifyBranchLayoutEntriesAndPrepareGitMacheteBranchData(coreBranch.get(), entry.getSubbranches());
    }
  }

  private SyncToOriginStatus computeSyncToOriginStatus(IGitCoreLocalBranch coreLocalBranch) throws GitMacheteException {
    try {
      Optional<IGitCoreBranchTrackingStatus> ts = coreLocalBranch.computeRemoteTrackingStatus();
      if (ts.isEmpty()) {
        return SyncToOriginStatus.Untracked;
      }

      if (ts.get().getAhead() > 0 && ts.get().getBehind() > 0) {
        return SyncToOriginStatus.Diverged;
      } else if (ts.get().getAhead() > 0) {
        return SyncToOriginStatus.Ahead;
      } else if (ts.get().getBehind() > 0) {
        return SyncToOriginStatus.Behind;
      } else {
        return SyncToOriginStatus.InSync;
      }

    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

  private SyncToParentStatus computeSyncToParentStatus(IGitCoreLocalBranch coreLocalBranch,
      IGitCoreLocalBranch parentCoreLocalBranch) throws GitMacheteException {
    try {
      if (parentCoreLocalBranch == null) {
        return SyncToParentStatus.InSync;
      }

      BaseGitCoreCommit parentPointedCommit = parentCoreLocalBranch.getPointedCommit();
      BaseGitCoreCommit pointedCommit = coreLocalBranch.getPointedCommit();

      if (pointedCommit.equals(parentPointedCommit)) {
        if (coreLocalBranch.hasJustBeenCreated()) {
          return SyncToParentStatus.InSync;
        } else {
          return SyncToParentStatus.Merged;
        }
      } else {
        var isParentAncestorOfChild = gitCoreRepository.isAncestor(
            /* presumedAncestor */ parentPointedCommit, /* presumedDescendant */ pointedCommit);

        if (isParentAncestorOfChild) {
          Optional<BaseGitCoreCommit> forkPoint = coreLocalBranch.computeForkPoint();
          if (forkPoint.isEmpty() || !forkPoint.get().equals(parentPointedCommit)) {
            return SyncToParentStatus.InSyncButForkPointOff;
          } else {
            return SyncToParentStatus.InSync;
          }
        } else {
          var isChildAncestorOfParent = gitCoreRepository.isAncestor(
              /* presumedAncestor */ pointedCommit, /* presumedDescendant */ parentPointedCommit);

          if (isChildAncestorOfParent) {
            return SyncToParentStatus.Merged;
          } else {
            return SyncToParentStatus.OutOfSync;
          }
        }
      }

    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

  private List<IGitMacheteBranch> deriveDownstreamBranches(List<IBranchLayoutEntry> directDownstreamEntries) {
    return List.ofAll(directDownstreamEntries)
        .map(entry -> createMacheteBranch(entry, deriveDownstreamBranches(entry.getSubbranches())))
        .peek(branch -> branchByName.put(branch.getName(), branch))
        .collect(List.collector());
  }

  /**
   * @return Option of {@link IGitCoreLocalBranch} or if branch with given name doesn't exist returns empty Option
   */
  private Optional<IGitCoreLocalBranch> getCoreBranchFromName(String branchName) {
    return Try.of(() -> Optional.of(gitCoreRepository.getLocalBranch(branchName))).getOrElse(() -> Optional.empty());
  }

  @AllArgsConstructor(staticName = "of")
  static private class GitMacheteBranchData {
    final IGitMacheteCommit pointedCommit;
    final List<IGitMacheteCommit> commits;
    final SyncToOriginStatus syncToOriginStatus;
    final SyncToParentStatus syncToParentStatus;
  }
}
