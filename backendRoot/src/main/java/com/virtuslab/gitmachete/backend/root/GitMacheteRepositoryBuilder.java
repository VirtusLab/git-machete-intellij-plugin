package com.virtuslab.gitmachete.backend.root;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BaseBranchLayoutEntry;
import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.impl.BranchLayoutFileParser;
import com.virtuslab.gitcore.api.BaseGitCoreCommit;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreBranch;
import com.virtuslab.gitcore.api.IGitCoreBranchTrackingStatus;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteRootBranch;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.MacheteFileParseException;
import com.virtuslab.gitmachete.backend.api.SyncToOriginStatus;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.impl.GitMacheteCommit;
import com.virtuslab.gitmachete.backend.impl.GitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepository;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRootBranch;

@Accessors(chain = true, fluent = true)
@Getter(AccessLevel.PACKAGE)
public class GitMacheteRepositoryBuilder implements IGitMacheteRepositoryBuilder {
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

  @Inject
  public GitMacheteRepositoryBuilder(
      IGitCoreRepositoryFactory gitCoreRepositoryFactory,
      @Assisted Path pathToRepoRoot) {
    this.gitCoreRepositoryFactory = gitCoreRepositoryFactory;
    this.pathToRepoRoot = pathToRepoRoot;
  }

  public GitMacheteRepository build() throws GitMacheteException {
    IGitCoreRepository gitCoreRepository = gitCoreRepositoryFactory.create(pathToRepoRoot);

    currentCoreBranch = Try.of(() -> gitCoreRepository.getCurrentBranch())
        .getOrElseThrow(e -> new GitMacheteException("Can't get current branch", e))
        .orElse(null);

    if (branchLayout == null) {
      Path pathToBranchLayoutFile = gitCoreRepository.getGitDirectoryPath().resolve("machete");
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

    java.util.List<BaseGitMacheteRootBranch> mutableGitMacheteRootBranchesList = new LinkedList<>();
    for (BaseBranchLayoutEntry entry : branchLayout.getRootBranches()) {
      var gitMacheteRootBranch = createGitMacheteRootBranch(gitCoreRepository, entry);
      mutableGitMacheteRootBranchesList.add(gitMacheteRootBranch);
    }

    var rootBranches = List.ofAll(mutableGitMacheteRootBranchesList);
    var rootBranchByName = rootBranches.toMap(branch -> Tuple.of(branch.getName(), branch)).toJavaMap();
    branchByName.putAll(rootBranchByName);

    BaseGitMacheteBranch currentBranch = currentCoreBranch != null
        ? branchByName.get(currentCoreBranch.getName())
        : null;

    return new GitMacheteRepository(repositoryName, rootBranches, branchLayout, currentBranch, branchByName);
  }

  private GitMacheteRootBranch createGitMacheteRootBranch(IGitCoreRepository gitCoreRepository,
      BaseBranchLayoutEntry entry) throws GitMacheteException {
    IGitCoreLocalBranch coreLocalBranch = Try.of(() -> gitCoreRepository.getLocalBranch(entry.getName()))
        .getOrElseThrow(e -> new GitMacheteException(e));

    BaseGitCoreCommit corePointedCommit = Try.of(() -> coreLocalBranch.getPointedCommit())
        .getOrElseThrow(e -> new GitMacheteException(e));

    var pointedCommit = new GitMacheteCommit(corePointedCommit);
    var syncToOriginStatus = deriveSyncToOriginStatus(coreLocalBranch);
    var customAnnotation = entry.getCustomAnnotation().orElse(null);
    AncestorityCache ancestorityCache = new AncestorityCache(gitCoreRepository);
    var subbranches = deriveDownstreamBranches(gitCoreRepository, ancestorityCache, coreLocalBranch, entry);

    return new GitMacheteRootBranch(entry.getName(), subbranches, pointedCommit, syncToOriginStatus, customAnnotation);
  }

  private GitMacheteNonRootBranch createGitMacheteNonRootBranch(IGitCoreRepository gitCoreRepository,
      AncestorityCache ancestorityCache,
      IGitCoreLocalBranch parentEntryCoreLocalBranch,
      BaseBranchLayoutEntry entry)
      throws GitMacheteException {

    IGitCoreLocalBranch coreLocalBranch = Try.of(() -> gitCoreRepository.getLocalBranch(entry.getName()))
        .getOrElseThrow(e -> new GitMacheteException(e));

    Optional<BaseGitCoreCommit> deducedForkPoint = deduceForkPoint(ancestorityCache, coreLocalBranch,
        parentEntryCoreLocalBranch);

    BaseGitCoreCommit corePointedCommit = Try.of(() -> coreLocalBranch.getPointedCommit())
        .getOrElseThrow(e -> new GitMacheteException(e));

    // translate IGitCoreCommit list to IGitMacheteCommit list
    List<IGitMacheteCommit> commits = deducedForkPoint.isPresent()
        ? Try.of(() -> coreLocalBranch.deriveCommitsUntil(deducedForkPoint.get()))
            .getOrElseThrow(e -> new GitMacheteException(e))
            .map(GitMacheteCommit::new)
            .collect(List.collector())
        : List.empty();

    var pointedCommit = new GitMacheteCommit(corePointedCommit);
    var forkPoint = deducedForkPoint.isPresent() ? new GitMacheteCommit(deducedForkPoint.get()) : null;
    var syncToOriginStatus = deriveSyncToOriginStatus(coreLocalBranch);
    var syncToParentStatus = deriveSyncToParentStatus(ancestorityCache, coreLocalBranch, parentEntryCoreLocalBranch,
        deducedForkPoint.orElse(null));
    var customAnnotation = entry.getCustomAnnotation().orElse(null);
    var subbranches = deriveDownstreamBranches(gitCoreRepository, ancestorityCache, coreLocalBranch, entry);

    return new GitMacheteNonRootBranch(entry.getName(), subbranches, forkPoint, pointedCommit,
        commits, syncToOriginStatus, syncToParentStatus, customAnnotation);
  }

  private Optional<BaseGitCoreCommit> deduceForkPoint(AncestorityCache ancestorityCache,
      IGitCoreLocalBranch coreLocalBranch,
      IGitCoreLocalBranch parentCoreLocalBranch) throws GitMacheteException {
    var forkPointOptional = Try.of(() -> coreLocalBranch.deriveForkPoint())
        .getOrElseThrow(e -> new GitMacheteException(e));
    if (!forkPointOptional.isPresent()) {
      return forkPointOptional;
    }

    return Try.of(() -> {
      var parentPointedCommit = parentCoreLocalBranch.getPointedCommit();
      var pointedCommit = coreLocalBranch.getPointedCommit();
      var isParentAncestorOfForkPoint = ancestorityCache.isAncestor(parentPointedCommit, forkPointOptional.get());
      var isParentAncestorOfChild = ancestorityCache.isAncestor(parentPointedCommit, pointedCommit);

      // If parent(A) is NOT ancestor of fork-point(A), and parent(A) is ancestor of A, then assume
      // fork-point(A)=parent(A)
      if (!isParentAncestorOfForkPoint && isParentAncestorOfChild) {
        return Optional.of(parentPointedCommit);
      } else {
        return forkPointOptional;
      }
    }).getOrElseThrow(e -> new GitMacheteException(e));
  }

  private List<GitMacheteNonRootBranch> deriveDownstreamBranches(IGitCoreRepository gitCoreRepository,
      AncestorityCache ancestorityCache,
      IGitCoreLocalBranch parentCoreLocalBranch,
      BaseBranchLayoutEntry directUpstreamEntry) throws GitMacheteException {
    java.util.List<GitMacheteNonRootBranch> mutableGitMacheteBranchesList = new LinkedList<>();

    for (BaseBranchLayoutEntry entry : directUpstreamEntry.getSubbranches()) {
      var gitMacheteNonRootBranch = createGitMacheteNonRootBranch(gitCoreRepository, ancestorityCache,
          parentCoreLocalBranch, entry);
      mutableGitMacheteBranchesList.add(gitMacheteNonRootBranch);
    }

    var downstreamBranches = List.ofAll(mutableGitMacheteBranchesList);
    var subbranchByName = downstreamBranches.toMap(branch -> Tuple.of(branch.getName(), branch)).toJavaMap();
    branchByName.putAll(subbranchByName);

    return downstreamBranches;
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

  private SyncToParentStatus deriveSyncToParentStatus(AncestorityCache ancestorityCache,
      IGitCoreLocalBranch coreLocalBranch,
      IGitCoreLocalBranch parentCoreLocalBranch,
      @Nullable BaseGitCoreCommit forkPoint)
      throws GitMacheteException {
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
        var isParentAncestorOfChild = ancestorityCache.isAncestor(
            /* presumedAncestor */ parentPointedCommit, /* presumedDescendant */ pointedCommit);

        if (isParentAncestorOfChild) {
          if (forkPoint != null && !forkPoint.equals(parentPointedCommit)) {
            return SyncToParentStatus.InSyncButForkPointOff;
          } else {
            return SyncToParentStatus.InSync;
          }
        } else {
          var isChildAncestorOfParent = ancestorityCache.isAncestor(
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

  @RequiredArgsConstructor
  private static class AncestorityCache {
    private final IGitCoreRepository repository;
    Map<Tuple2<BaseGitCoreCommit, BaseGitCoreCommit>, Boolean> cache = new HashMap<>();

    boolean isAncestor(BaseGitCoreCommit presumedAncestor, BaseGitCoreCommit presumedDescendant)
        throws GitMacheteException {
      Tuple2<BaseGitCoreCommit, BaseGitCoreCommit> key = Tuple.of(presumedAncestor, presumedDescendant);
      Boolean isAncestorResult = cache.get(key);
      if (isAncestorResult != null) {
        return isAncestorResult;
      } else {
        isAncestorResult = Try.of(() -> repository.isAncestor(presumedAncestor, presumedDescendant))
            .getOrElseThrow(e -> new GitMacheteException(e));
        cache.put(key, isAncestorResult);
        return isAncestorResult;
      }
    }
  }
}
