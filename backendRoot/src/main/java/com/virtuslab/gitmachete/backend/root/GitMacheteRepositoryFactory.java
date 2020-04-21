package com.virtuslab.gitmachete.backend.root;

import static com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus.Relation.Ahead;
import static com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus.Relation.Behind;
import static com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus.Relation.Diverged;
import static com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus.Relation.InSync;
import static com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus.Relation.Untracked;

import java.nio.file.Path;

import io.vavr.Tuple;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BaseBranchLayoutEntry;
import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.impl.BranchLayoutFileParser;
import com.virtuslab.gitcore.api.BaseGitCoreCommit;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreBranchTrackingStatus;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitcore.api.IGitCoreRepositoryFactory;
import com.virtuslab.gitcore.impl.jgit.GitCoreRepositoryFactory;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus;
import com.virtuslab.gitmachete.backend.api.MacheteFileParseException;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.impl.GitMacheteCommit;
import com.virtuslab.gitmachete.backend.impl.GitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepository;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRootBranch;
import com.virtuslab.gitmachete.backend.impl.SyncToRemoteStatus;

public class GitMacheteRepositoryFactory {
  private Map<String, BaseGitMacheteBranch> branchByName = HashMap.empty();

  private final IGitCoreRepositoryFactory gitCoreRepositoryFactory;

  public GitMacheteRepositoryFactory() {
    gitCoreRepositoryFactory = new GitCoreRepositoryFactory();
  }

  public IGitMacheteRepository create(Path mainDirectoryPath, Path gitDirectoryPath) throws GitMacheteException {
    return create(mainDirectoryPath, gitDirectoryPath, /* givenBranchLayout */ null);
  }

  public IGitMacheteRepository create(Path mainDirectoryPath, Path gitDirectoryPath,
      @Nullable IBranchLayout givenBranchLayout) throws GitMacheteException {
    // To make sure there are no leftovers from the previous invocations.
    branchByName = HashMap.empty();

    IGitCoreRepository gitCoreRepository = Try
        .of(() -> gitCoreRepositoryFactory.create(mainDirectoryPath, gitDirectoryPath))
        .getOrElseThrow(
            e -> new GitMacheteException("Can't create an ${IGitCoreRepository.class.getSimpleName()} instance " +
                "under ${mainDirectoryPath} (with git directory under ${gitDirectoryPath})", e));

    var branchLayout = givenBranchLayout != null
        ? givenBranchLayout
        : createBranchLayout(gitCoreRepository.getGitDirectoryPath().resolve("machete"));

    var rootBranchTries = branchLayout.getRootBranches()
        .map(entry -> Try.of(() -> createGitMacheteRootBranch(gitCoreRepository, entry)));
    var rootBranches = Try.sequence(rootBranchTries).getOrElseThrow(GitMacheteException::castOrWrap);

    var rootBranchByName = rootBranches.toMap(branch -> Tuple.of(branch.getName(), branch));
    branchByName = branchByName.merge(rootBranchByName);

    BaseGitMacheteBranch currentBranch = Try.of(() -> gitCoreRepository.getCurrentBranch())
        .getOrElseThrow(e -> new GitMacheteException("Can't get current branch", e))
        .flatMap(cb -> branchByName.get(cb.getName()))
        .getOrNull();

    return new GitMacheteRepository(List.ofAll(rootBranches), branchLayout, currentBranch, branchByName);
  }

  private IBranchLayout createBranchLayout(Path branchLayoutFilePath) throws MacheteFileParseException {
    return Try.of(() -> new BranchLayoutFileParser(branchLayoutFilePath).parse())
        .getOrElseThrow(e -> {
          Option<@Positive Integer> errorLine = ((BranchLayoutException) e).getErrorLine();
          return new MacheteFileParseException("Error occurred while parsing machete file ${branchLayoutFilePath}" +
              (errorLine.isDefined() ? " in line ${errorLine.get()}" : ""), e);
        });
  }

  private GitMacheteRootBranch createGitMacheteRootBranch(IGitCoreRepository gitCoreRepository,
      BaseBranchLayoutEntry entry) throws GitMacheteException {
    IGitCoreLocalBranch coreLocalBranch = Try.of(() -> gitCoreRepository.getLocalBranch(entry.getName()))
        .getOrElseThrow(e -> new GitMacheteException(e));

    BaseGitCoreCommit corePointedCommit = Try.of(() -> coreLocalBranch.getPointedCommit())
        .getOrElseThrow(e -> new GitMacheteException(e));

    var pointedCommit = new GitMacheteCommit(corePointedCommit);
    var syncToRemoteStatus = deriveSyncToRemoteStatus(coreLocalBranch);
    var customAnnotation = entry.getCustomAnnotation().getOrNull();
    var subbranches = deriveDownstreamBranches(gitCoreRepository, coreLocalBranch, entry);

    return new GitMacheteRootBranch(entry.getName(), subbranches, pointedCommit, syncToRemoteStatus, customAnnotation);
  }

  private GitMacheteNonRootBranch createGitMacheteNonRootBranch(IGitCoreRepository gitCoreRepository,
      IGitCoreLocalBranch parentEntryCoreLocalBranch,
      BaseBranchLayoutEntry entry)
      throws GitMacheteException {

    IGitCoreLocalBranch coreLocalBranch = Try.of(() -> gitCoreRepository.getLocalBranch(entry.getName()))
        .getOrElseThrow(e -> new GitMacheteException(e));

    Option<BaseGitCoreCommit> deducedForkPoint = deduceForkPoint(gitCoreRepository, coreLocalBranch,
        parentEntryCoreLocalBranch);

    BaseGitCoreCommit corePointedCommit = Try.of(() -> coreLocalBranch.getPointedCommit())
        .getOrElseThrow(e -> new GitMacheteException(e));

    // translate IGitCoreCommit list to IGitMacheteCommit list
    List<IGitMacheteCommit> commits = deducedForkPoint.isDefined()
        ? Try.of(() -> coreLocalBranch.deriveCommitsUntil(deducedForkPoint.get()))
            .getOrElseThrow(e -> new GitMacheteException(e))
            .map(GitMacheteCommit::new)
            .collect(List.collector())
        : List.empty();

    var pointedCommit = new GitMacheteCommit(corePointedCommit);
    var forkPoint = deducedForkPoint.isDefined() ? new GitMacheteCommit(deducedForkPoint.get()) : null;
    var syncToRemoteStatus = deriveSyncToRemoteStatus(coreLocalBranch);
    var syncToParentStatus = deriveSyncToParentStatus(gitCoreRepository, coreLocalBranch, parentEntryCoreLocalBranch,
        deducedForkPoint.getOrNull());
    var customAnnotation = entry.getCustomAnnotation().getOrNull();
    var subbranches = deriveDownstreamBranches(gitCoreRepository, coreLocalBranch, entry);

    return new GitMacheteNonRootBranch(entry.getName(), subbranches, forkPoint, pointedCommit,
        commits, syncToRemoteStatus, syncToParentStatus, customAnnotation);
  }

  private Option<BaseGitCoreCommit> deduceForkPoint(
      IGitCoreRepository gitCoreRepository,
      IGitCoreLocalBranch coreLocalBranch,
      IGitCoreLocalBranch parentCoreLocalBranch) throws GitMacheteException {

    return Try.of(() -> {

      var forkPointOption = coreLocalBranch.deriveForkPoint();
      var parentPointedCommit = parentCoreLocalBranch.getPointedCommit();
      var pointedCommit = coreLocalBranch.getPointedCommit();

      var isParentAncestorOfChild = gitCoreRepository.isAncestor(parentPointedCommit, pointedCommit);

      if (isParentAncestorOfChild) {
        if (forkPointOption.isDefined()) {
          var isParentAncestorOfForkPoint = gitCoreRepository.isAncestor(parentPointedCommit, forkPointOption.get());

          if (!isParentAncestorOfForkPoint) {
            // If parent(A) is ancestor of A, and parent(A) is NOT ancestor of fork-point(A),
            // then assume fork-point(A)=parent(A)
            return Option.of(parentPointedCommit);
          }

        } else {
          // If parent(A) is ancestor of A, and fork-point(A) is missing,
          // then assume fork-point(A)=parent(A)
          return Option.of(parentPointedCommit);
        }
      }

      return forkPointOption;

    }).getOrElseThrow(e -> new GitMacheteException(e));
  }

  private List<GitMacheteNonRootBranch> deriveDownstreamBranches(
      IGitCoreRepository gitCoreRepository,
      IGitCoreLocalBranch parentCoreLocalBranch,
      BaseBranchLayoutEntry directUpstreamEntry) throws GitMacheteException {

    var downstreamBranchTries = directUpstreamEntry.getSubbranches().map(entry -> Try.of(
        () -> createGitMacheteNonRootBranch(gitCoreRepository, parentCoreLocalBranch, entry)));
    var downstreamBranches = Try.sequence(downstreamBranchTries).getOrElseThrow(GitMacheteException::castOrWrap);

    var downstreamBranchByName = downstreamBranches.toMap(branch -> Tuple.of(branch.getName(), branch));
    branchByName = branchByName.merge(downstreamBranchByName);

    return List.ofAll(downstreamBranches);
  }

  private ISyncToRemoteStatus deriveSyncToRemoteStatus(IGitCoreLocalBranch coreLocalBranch) throws GitMacheteException {
    try {
      Option<IGitCoreBranchTrackingStatus> ts = coreLocalBranch.deriveRemoteTrackingStatus();
      if (ts.isEmpty()) {
        return SyncToRemoteStatus.of(Untracked, "");
      }

      IGitCoreBranchTrackingStatus trackingStatus = ts.get();

      if (trackingStatus.getAhead() > 0 && trackingStatus.getBehind() > 0) {
        return SyncToRemoteStatus.of(Diverged, trackingStatus.getRemoteName());
      } else if (trackingStatus.getAhead() > 0) {
        return SyncToRemoteStatus.of(Ahead, trackingStatus.getRemoteName());
      } else if (trackingStatus.getBehind() > 0) {
        return SyncToRemoteStatus.of(Behind, trackingStatus.getRemoteName());
      } else {
        return SyncToRemoteStatus.of(InSync, trackingStatus.getRemoteName());
      }

    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

  private SyncToParentStatus deriveSyncToParentStatus(
      IGitCoreRepository gitCoreRepository,
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
        var isParentAncestorOfChild = gitCoreRepository.isAncestor(
            /* presumedAncestor */ parentPointedCommit, /* presumedDescendant */ pointedCommit);

        if (isParentAncestorOfChild) {
          if (forkPoint != null && !forkPoint.equals(parentPointedCommit)) {
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
}
