package com.virtuslab.gitmachete.backend.root;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import io.vavr.Tuple;
import io.vavr.collection.List;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

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
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteRootBranch;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.GitMacheteJGitException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteSubmoduleEntry;
import com.virtuslab.gitmachete.backend.api.MacheteFileParseException;
import com.virtuslab.gitmachete.backend.api.SyncToOriginStatus;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.impl.GitMacheteCommit;
import com.virtuslab.gitmachete.backend.impl.GitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepository;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRootBranch;
import com.virtuslab.gitmachete.backend.impl.GitMacheteSubmoduleEntry;

@Accessors(chain = true, fluent = true)
@Getter(AccessLevel.PACKAGE)
public class GitMacheteRepositoryBuilder implements IGitMacheteRepositoryBuilder {
  @MonotonicNonNull
  private BaseGitMacheteBranch currentBranch = null;
  @Nullable
  private IGitCoreBranch currentCoreBranch;
  private final Map<String, BaseGitMacheteBranch> branchByName = new HashMap<>();

  private final IGitCoreRepositoryFactory gitCoreRepositoryFactory;

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
      IGitCoreRepositoryFactory gitCoreRepositoryFactory,
      @Assisted Path pathToRepoRoot) {
    this.gitCoreRepositoryFactory = gitCoreRepositoryFactory;
    this.pathToRepoRoot = pathToRepoRoot;
  }

  public GitMacheteRepository build() throws GitMacheteException {
    var gitCoreRepository = gitCoreRepositoryFactory.create(pathToRepoRoot);

    currentCoreBranch = Try.of(() -> gitCoreRepository.getCurrentBranch())
        .getOrElseThrow(e -> new GitMacheteException("Can't get current branch", e))
        .orElse(null);

    if (branchLayout == null) {
      Path pathToBranchLayoutFile = gitCoreRepository.getGitFolderPath().resolve("machete");
      branchLayout = Try.of(() -> new BranchLayoutFileParser(pathToBranchLayoutFile).parse())
          .getOrElseThrow(e -> {
            Optional<Integer> errorLine = ((BranchLayoutException) e).getErrorLine();
            return new MacheteFileParseException(errorLine.isPresent()
                ? String.format("Error occurred while parsing machete file %s in line %d",
                    pathToBranchLayoutFile.toString(), errorLine.get())
                : String.format("Error occurred while parsing machete file %s",
                    pathToBranchLayoutFile.toString()),
                e);
          });
    }

    verifyBranchLayoutEntriesAndPrepareGitMacheteBranchData(gitCoreRepository, /* parentEntryCoreLocalBranch */ null,
        branchLayout.getRootBranches());

    List<BaseGitMacheteRootBranch> macheteRootBranches = List.narrow(
        branchLayout.getRootBranches()
            .map(entry -> createMacheteRootBranch(entry, deriveDownstreamBranches(entry.getSubbranches()))));

    var rootBranchByName = macheteRootBranches.toMap(branch -> Tuple.of(branch.getName(), branch)).toJavaMap();
    branchByName.putAll(rootBranchByName);

    List<IGitMacheteSubmoduleEntry> macheteSubmodules = Try.of(() -> gitCoreRepository.getSubmodules())
        .getOrElseThrow(e -> new GitMacheteJGitException("Error while getting submodules", e))
        .map(this::convertToGitMacheteSubmoduleEntry)
        .collect(List.collector());

    return new GitMacheteRepository(repositoryName, macheteRootBranches, macheteSubmodules, branchLayout, currentBranch,
        branchByName);
  }

  private List<GitMacheteNonRootBranch> deriveDownstreamBranches(List<IBranchLayoutEntry> directDownstreamEntries) {
    List<GitMacheteNonRootBranch> gitMacheteBranches = List.ofAll(directDownstreamEntries)
        .map(entry -> createMacheteNonRootBranch(entry, deriveDownstreamBranches(entry.getSubbranches())))
        .collect(List.collector());

    var subbranchByName = gitMacheteBranches.toMap(branch -> Tuple.of(branch.getName(), branch)).toJavaMap();
    branchByName.putAll(subbranchByName);

    return gitMacheteBranches;
  }

  private IGitMacheteSubmoduleEntry convertToGitMacheteSubmoduleEntry(IGitCoreSubmoduleEntry m) {
    return new GitMacheteSubmoduleEntry(m.getPath(), m.getName());
  }

  private GitMacheteRootBranch createMacheteRootBranch(IBranchLayoutEntry branchEntry,
      List<GitMacheteNonRootBranch> downstreamBranches) {
    String customAnnotation = branchEntry.getCustomAnnotation().orElse(null);

    GitMacheteBranchData gitMacheteBranchData = branchNameToGitMacheteBranchData.get(branchEntry.getName());
    assert gitMacheteBranchData != null;

    var branch = new GitMacheteRootBranch(branchEntry.getName(),
        downstreamBranches,
        gitMacheteBranchData.pointedCommit,
        gitMacheteBranchData.syncToOriginStatus,
        gitMacheteBranchData.coreBranch,
        customAnnotation);

    if (gitMacheteBranchData.coreBranch.equals(currentCoreBranch)) {
      currentBranch = branch;
    }

    return branch;
  }

  private GitMacheteNonRootBranch createMacheteNonRootBranch(IBranchLayoutEntry branchEntry,
      List<GitMacheteNonRootBranch> downstreamBranches) {
    String customAnnotation = branchEntry.getCustomAnnotation().orElse(null);

    GitMacheteBranchData gitMacheteBranchData = branchNameToGitMacheteBranchData.get(branchEntry.getName());
    assert gitMacheteBranchData != null;

    var branch = new GitMacheteNonRootBranch(branchEntry.getName(),
        downstreamBranches,
        gitMacheteBranchData.pointedCommit,
        gitMacheteBranchData.commits,
        gitMacheteBranchData.syncToOriginStatus,
        gitMacheteBranchData.syncToParentStatus,
        gitMacheteBranchData.coreBranch,
        customAnnotation);

    if (gitMacheteBranchData.coreBranch.equals(currentCoreBranch)) {
      currentBranch = branch;
    }

    return branch;
  }

  /**
   * Recursively verifies whether all branches entries of the provided list exists within {@code coreRepository}.
   * Additionally some git machete branch data is collected and stored. Those operations are performed in advance to
   * simplify further process of a machete repository construction.
   */
  private void verifyBranchLayoutEntriesAndPrepareGitMacheteBranchData(
      IGitCoreRepository gitCoreRepository,
      @Nullable IGitCoreLocalBranch parentEntryCoreLocalBranch,
      List<IBranchLayoutEntry> entries)
      throws GitMacheteException {
    for (var entry : entries) {
      Optional<IGitCoreLocalBranch> coreBranchOptional = getCoreBranchFromName(gitCoreRepository, entry.getName());
      if (!coreBranchOptional.isPresent()) {
        throw new GitMacheteException(MessageFormat
            .format("Branch \"%s\" defined in machete file does not exist in repository", entry.getName()));
      } else {
        IGitCoreLocalBranch coreLocalBranch = coreBranchOptional.get();

        try {
          Optional<BaseGitCoreCommit> forkPoint = coreLocalBranch.deriveForkPoint();

          // translate IGitCoreCommit list to IGitMacheteCommit list
          List<GitMacheteCommit> commits = !forkPoint.isPresent()
              ? List.empty()
              : coreLocalBranch.deriveCommitsUntil(forkPoint.get())
                  .map(GitMacheteCommit::new)
                  .collect(List.collector());

          var pointedCommit = new GitMacheteCommit(coreLocalBranch.getPointedCommit());
          var syncToOriginStatus = deriveSyncToOriginStatus(coreLocalBranch);
          var syncToParentStatus = deriveSyncToParentStatus(gitCoreRepository, coreLocalBranch,
              parentEntryCoreLocalBranch);

          var gitMacheteBranchData = GitMacheteBranchData.of(pointedCommit, commits, syncToOriginStatus,
              syncToParentStatus, coreLocalBranch);

          branchNameToGitMacheteBranchData.put(entry.getName(), gitMacheteBranchData);

        } catch (GitCoreException e) {
          throw new GitMacheteException(e);
        }

        verifyBranchLayoutEntriesAndPrepareGitMacheteBranchData(gitCoreRepository, coreLocalBranch,
            entry.getSubbranches());
      }
    }
  }

  private SyncToOriginStatus deriveSyncToOriginStatus(IGitCoreLocalBranch coreLocalBranch) throws GitMacheteException {
    try {
      Optional<IGitCoreBranchTrackingStatus> ts = coreLocalBranch.deriveRemoteTrackingStatus();
      if (!ts.isPresent()) {
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

  private SyncToParentStatus deriveSyncToParentStatus(IGitCoreRepository gitCoreRepository,
      IGitCoreLocalBranch coreLocalBranch,
      @Nullable IGitCoreLocalBranch parentCoreLocalBranch) throws GitMacheteException {
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
          Optional<BaseGitCoreCommit> forkPoint = coreLocalBranch.deriveForkPoint();
          if (!forkPoint.isPresent() || !forkPoint.get().equals(parentPointedCommit)) {
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

  /**
   * @return Option of {@link IGitCoreLocalBranch} or if branch with given name doesn't exist returns empty Option
   */
  private Optional<IGitCoreLocalBranch> getCoreBranchFromName(IGitCoreRepository gitCoreRepository, String branchName) {
    return Try.of(() -> Optional.of(gitCoreRepository.getLocalBranch(branchName))).getOrElse(() -> Optional.empty());
  }

  @AllArgsConstructor(staticName = "of")
  private static class GitMacheteBranchData {
    final GitMacheteCommit pointedCommit;
    final List<GitMacheteCommit> commits;
    final SyncToOriginStatus syncToOriginStatus;
    final SyncToParentStatus syncToParentStatus;
    final IGitCoreLocalBranch coreBranch;
  }
}
