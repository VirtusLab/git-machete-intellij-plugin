package com.virtuslab.gitmachete.backend.impl;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.AheadOfRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.BehindRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedFromAndNewerThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedFromAndOlderThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.InSyncToRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.Untracked;

import java.nio.file.Path;
import java.time.Instant;

import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Queue;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.CustomLog;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.IBranchLayoutEntry;
import com.virtuslab.gitcore.api.GitCoreBranchTrackingStatus;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranch;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitcore.api.IGitCoreRepositoryFactory;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRemoteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryFactory;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.backend.impl.hooks.StatusBranchHookExecutor;

@CustomLog
public class GitMacheteRepositoryFactory implements IGitMacheteRepositoryFactory {

  private final IGitCoreRepositoryFactory gitCoreRepositoryFactory;

  public GitMacheteRepositoryFactory() {
    gitCoreRepositoryFactory = RuntimeBinding.instantiateSoleImplementingClass(IGitCoreRepositoryFactory.class);
  }

  @Override
  public IGitMacheteRepository create(Path mainDirectoryPath, Path gitDirectoryPath, IBranchLayout branchLayout)
      throws GitMacheteException {
    LOG.startTimer().debug(() -> "Entering: mainDirectoryPath = ${mainDirectoryPath}, gitDirectoryPath = ${gitDirectoryPath}");

    IGitCoreRepository gitCoreRepository = Try
        .of(() -> gitCoreRepositoryFactory.create(mainDirectoryPath, gitDirectoryPath))
        .getOrElseThrow(
            e -> new GitMacheteException("Can't create an ${IGitCoreRepository.class.getSimpleName()} instance " +
                "under ${mainDirectoryPath} (with git directory under ${gitDirectoryPath})", e));

    var statusHookExecutor = StatusBranchHookExecutor.of(mainDirectoryPath, gitDirectoryPath);

    var rootBranchTries = branchLayout.getRootEntries()
        .map(entry -> Try.of(() -> createGitMacheteRootBranch(gitCoreRepository, statusHookExecutor, entry)));
    var rootBranches = Try.sequence(rootBranchTries).getOrElseThrow(GitMacheteException::castOrWrap);

    var branchByName = createBranchByNameMap(rootBranches);

    IGitMacheteBranch currentBranch = Try.of(() -> gitCoreRepository.getCurrentBranch())
        .getOrElseThrow(e -> new GitMacheteException("Can't get current branch", e))
        .flatMap(cb -> branchByName.get(cb.getShortName()))
        .getOrNull();

    LOG.debug(() -> "Current branch: ${currentBranch != null ? currentBranch.getName() : \"<none> (detached HEAD)\"}");
    LOG.withTimeElapsed().debug(() -> "Finished");

    return new GitMacheteRepository(List.ofAll(rootBranches), branchLayout, currentBranch, branchByName);
  }

  private Map<String, IGitMacheteBranch> createBranchByNameMap(Seq<GitMacheteRootBranch> rootBranches) {
    Map<String, IGitMacheteBranch> branchByName = HashMap.empty();
    Queue<IGitMacheteBranch> queue = Queue.ofAll(rootBranches);
    // BFS over all branches
    while (queue.nonEmpty()) {
      var headAndTail = queue.dequeue();
      var branch = headAndTail._1;
      branchByName = branchByName.put(branch.getName(), branch);
      queue = headAndTail._2.appendAll(branch.getDownstreamBranches());
    }
    return branchByName;
  }

  private GitMacheteRootBranch createGitMacheteRootBranch(
      IGitCoreRepository gitCoreRepository,
      StatusBranchHookExecutor statusHookExecutor,
      IBranchLayoutEntry entry) throws GitMacheteException {
    var branchName = entry.getName();
    IGitCoreLocalBranch coreLocalBranch = Try.of(() -> gitCoreRepository.getLocalBranch(branchName))
        .getOrElseThrow(e -> new GitMacheteException(e));

    IGitCoreCommit corePointedCommit = Try.of(() -> coreLocalBranch.getPointedCommit())
        .getOrElseThrow(e -> new GitMacheteException(e));

    var pointedCommit = new GitMacheteCommit(corePointedCommit);
    var syncToRemoteStatus = deriveSyncToRemoteStatus(coreLocalBranch);
    var customAnnotation = entry.getCustomAnnotation().getOrNull();
    var downstreamBranches = deriveDownstreamBranches(gitCoreRepository, statusHookExecutor, coreLocalBranch, entry);
    var remoteBranch = getRemoteBranchFromCoreLocalBranch(coreLocalBranch);
    var statusHookOutput = statusHookExecutor.deriveHookOutputFor(branchName, pointedCommit).getOrNull();

    return new GitMacheteRootBranch(branchName, downstreamBranches, pointedCommit,
        remoteBranch, syncToRemoteStatus, customAnnotation, statusHookOutput);
  }

  private GitMacheteNonRootBranch createGitMacheteNonRootBranch(
      IGitCoreRepository gitCoreRepository,
      StatusBranchHookExecutor statusHookExecutor,
      IGitCoreLocalBranch parentEntryCoreLocalBranch,
      IBranchLayoutEntry entry) throws GitMacheteException {

    var branchName = entry.getName();

    IGitCoreLocalBranch coreLocalBranch = Try.of(() -> gitCoreRepository.getLocalBranch(branchName))
        .getOrElseThrow(e -> new GitMacheteException(e));

    Option<IGitCoreCommit> deducedForkPoint = deduceForkPoint(gitCoreRepository, coreLocalBranch,
        parentEntryCoreLocalBranch);

    IGitCoreCommit corePointedCommit = Try.of(() -> coreLocalBranch.getPointedCommit())
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
    var downstreamBranches = deriveDownstreamBranches(gitCoreRepository, statusHookExecutor, coreLocalBranch, entry);
    var remoteBranch = getRemoteBranchFromCoreLocalBranch(coreLocalBranch);
    var statusHookOutput = statusHookExecutor.deriveHookOutputFor(branchName, pointedCommit).getOrNull();

    return new GitMacheteNonRootBranch(branchName, downstreamBranches, pointedCommit,
        remoteBranch, syncToRemoteStatus, customAnnotation, statusHookOutput, forkPoint, commits, syncToParentStatus);
  }

  @Nullable
  private IGitMacheteRemoteBranch getRemoteBranchFromCoreLocalBranch(IGitCoreLocalBranch coreLocalBranch)
      throws GitMacheteException {
    IGitMacheteRemoteBranch remoteBranch = null;
    Option<IGitCoreRemoteBranch> remoteBranchOption = coreLocalBranch.getRemoteTrackingBranch();
    if (remoteBranchOption.isDefined()) {
      IGitCoreRemoteBranch coreRemoteBranch = remoteBranchOption.get();
      IGitCoreCommit coreRemoteBranchPointedCommit = Try.of(() -> coreRemoteBranch.getPointedCommit())
          .getOrElseThrow(e -> new GitMacheteException("Cannot get core remote branch pointed commit", e));
      remoteBranch = new GitMacheteRemoteBranch(new GitMacheteCommit(coreRemoteBranchPointedCommit));
    }
    return remoteBranch;
  }

  private Option<IGitCoreCommit> deduceForkPoint(
      IGitCoreRepository gitCoreRepository,
      IGitCoreLocalBranch coreLocalBranch,
      IGitCoreLocalBranch parentCoreLocalBranch) throws GitMacheteException {
    LOG.debug(() -> "Entering: gitCoreRepository = ${gitCoreRepository.getMainDirectoryPath()}, " +
        "coreLocalBranch = '${coreLocalBranch.getShortName()}', parentCoreLocalBranch = '${parentCoreLocalBranch.getShortName()}'");
    return Try.of(() -> {

      var forkPointOption = coreLocalBranch.deriveForkPoint();
      var parentPointedCommit = parentCoreLocalBranch.getPointedCommit();
      var pointedCommit = coreLocalBranch.getPointedCommit();

      LOG.debug(
          () -> "forkPointOption = ${forkPointOption.isDefined() ? forkPointOption.get().getHash().getHashString() : \"empty\"}, "
              + "parentPointedCommit = ${parentPointedCommit.getHash().getHashString()}, " +
              "pointedCommit = ${pointedCommit.getHash().getHashString()}");

      var isParentAncestorOfChild = gitCoreRepository.isAncestor(parentPointedCommit, pointedCommit);

      LOG.debug(() -> "Parent branch commit (${parentPointedCommit.getHash().getHashString()}) " +
          "is${isParentAncestorOfChild ? \"\" : \" NOT\"} ancestor of child commit " +
          "(${pointedCommit.getHash().getHashString()})");

      if (isParentAncestorOfChild) {
        if (forkPointOption.isDefined()) {
          var isParentAncestorOfForkPoint = gitCoreRepository.isAncestor(parentPointedCommit, forkPointOption.get());

          if (!isParentAncestorOfForkPoint) {
            // If parent(A) is ancestor of A, and parent(A) is NOT ancestor of fork-point(A),
            // then assume fork-point(A)=parent(A)
            LOG.debug(() -> "Parent branch commit (${parentPointedCommit.getHash().getHashString()}) is ancestor of " +
                "pointed commit (${pointedCommit.getHash().getHashString()}) but parent branch commit " +
                "is NOT ancestor of pointed commit fork point (${forkPointOption.get().getHash().getHashString()}), " +
                "so we assume that pointed commit fork point = parent branch commit");
            return Option.of(parentPointedCommit);
          }

        } else {
          // If parent(A) is ancestor of A, and fork-point(A) is missing,
          // then assume fork-point(A)=parent(A)
          LOG.debug(
              () -> "Parent branch commit (${parentPointedCommit.getHash().getHashString()}) is ancestor of pointed commit " +
                  "(${pointedCommit.getHash().getHashString()}) but fork point of pointed commit is missing, " +
                  "so we assume that pointed commit fork point = parent branch commit");
          return Option.of(parentPointedCommit);
        }
      }

      String forkPointString = forkPointOption.isDefined() ? forkPointOption.get().getHash().getHashString() : "empty";
      // String interpolation caused some weird Nullness Checker issues (exception from `com.sun.tools.javac`) in this line.
      LOG.debug(() -> "Deduced fork point for branch " + coreLocalBranch.getShortName() + " is " + forkPointString);

      return forkPointOption;

    }).getOrElseThrow(e -> new GitMacheteException(e));
  }

  private List<GitMacheteNonRootBranch> deriveDownstreamBranches(
      IGitCoreRepository gitCoreRepository,
      StatusBranchHookExecutor statusHookExecutor,
      IGitCoreLocalBranch parentCoreLocalBranch,
      IBranchLayoutEntry directUpstreamEntry) throws GitMacheteException {

    var downstreamBranchTries = directUpstreamEntry.getSubentries().map(entry -> Try.of(
        () -> createGitMacheteNonRootBranch(gitCoreRepository, statusHookExecutor, parentCoreLocalBranch, entry)));
    var downstreamBranches = Try.sequence(downstreamBranchTries).getOrElseThrow(GitMacheteException::castOrWrap);
    return List.ofAll(downstreamBranches);
  }

  private SyncToRemoteStatus deriveSyncToRemoteStatus(IGitCoreLocalBranch coreLocalBranch) throws GitMacheteException {
    LOG.debug(() -> "Entering: coreLocalBranch = '${coreLocalBranch.getShortName()}'");

    try {
      Option<GitCoreBranchTrackingStatus> ts = coreLocalBranch.deriveRemoteTrackingStatus();
      if (ts.isEmpty()) {
        LOG.debug(() -> "Branch '${coreLocalBranch.getShortName()}' is untracked");
        return SyncToRemoteStatus.of(Untracked, "");
      }

      GitCoreBranchTrackingStatus trackingStatus = ts.get();
      SyncToRemoteStatus syncToRemoteStatus;

      if (trackingStatus.getAhead() > 0 && trackingStatus.getBehind() > 0) {
        Option<IGitCoreRemoteBranch> remoteTrackingBranchOption = coreLocalBranch.getRemoteTrackingBranch();
        if (remoteTrackingBranchOption.isDefined()) {
          Instant localBranchCommitDate = coreLocalBranch.getPointedCommit().getCommitTime();
          Instant remoteBranchCommitDate = remoteTrackingBranchOption.get().getPointedCommit().getCommitTime();
          // In case when commit dates are equal we assume that our relation is `DivergedAndNewerThanRemote`
          if (remoteBranchCommitDate.compareTo(localBranchCommitDate) > 0) {
            syncToRemoteStatus = SyncToRemoteStatus.of(DivergedFromAndOlderThanRemote, trackingStatus.getRemoteName());
          } else {
            if (remoteBranchCommitDate.compareTo(localBranchCommitDate) == 0) {
              LOG.debug("Commit dates of both local and remote branches are the same, so we assume " +
                  "'DivergedAndNewerThanRemote' sync to remote status");
            }
            syncToRemoteStatus = SyncToRemoteStatus.of(DivergedFromAndNewerThanRemote, trackingStatus.getRemoteName());
          }
        } else {
          // Theoretically this `else` should never happen coz deriveRemoteTrackingStatus() for coreLocalBranch
          // should be empty in this case
          LOG.debug(() -> "Because remote tracking branch for branch '${coreLocalBranch.getShortName()}' is undefined" +
              "this branch is untracked");
          return SyncToRemoteStatus.of(Untracked, "");
        }
      } else if (trackingStatus.getAhead() > 0) {
        syncToRemoteStatus = SyncToRemoteStatus.of(AheadOfRemote, trackingStatus.getRemoteName());
      } else if (trackingStatus.getBehind() > 0) {
        syncToRemoteStatus = SyncToRemoteStatus.of(BehindRemote, trackingStatus.getRemoteName());
      } else {
        syncToRemoteStatus = SyncToRemoteStatus.of(InSyncToRemote, trackingStatus.getRemoteName());
      }

      LOG.debug(() -> "Sync to remote status for branch '${coreLocalBranch.getShortName()}': ${syncToRemoteStatus.toString()}");

      return syncToRemoteStatus;

    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

  private SyncToParentStatus deriveSyncToParentStatus(
      IGitCoreRepository gitCoreRepository,
      IGitCoreLocalBranch coreLocalBranch,
      IGitCoreLocalBranch parentCoreLocalBranch,
      @Nullable IGitCoreCommit forkPoint)
      throws GitMacheteException {
    LOG.debug(() -> "Entering: gitCoreRepository = ${gitCoreRepository.getMainDirectoryPath()}, " +
        "coreLocalBranch = '${coreLocalBranch.getShortName()}', parentCoreLocalBranch = '${parentCoreLocalBranch.getShortName()}', "
        + "forkPoint = ${forkPoint != null ? forkPoint.getHash().getHashString() : \"null\"})");
    try {
      IGitCoreCommit parentPointedCommit = parentCoreLocalBranch.getPointedCommit();
      IGitCoreCommit pointedCommit = coreLocalBranch.getPointedCommit();

      LOG.debug(() -> "parentPointedCommit = ${parentPointedCommit.getHash().getHashString()}; " +
          "pointedCommit = ${pointedCommit.getHash().getHashString()}");

      if (pointedCommit.equals(parentPointedCommit)) {
        if (coreLocalBranch.hasJustBeenCreated()) {
          LOG.debug(() -> "Branch '${coreLocalBranch.getShortName()}' has been detected as just created, " +
              "so we assume it's in sync");
          return SyncToParentStatus.InSync;
        } else {
          LOG.debug(
              () -> "For this branch (${coreLocalBranch.getShortName()}) its parent's commit is equal to this branch pointed commit "
                  + "and this branch hasn't been detected as just created, so we assume it's merged");
          return SyncToParentStatus.MergedToParent;
        }
      } else {
        var isParentAncestorOfChild = gitCoreRepository.isAncestor(
            /* presumedAncestor */ parentPointedCommit, /* presumedDescendant */ pointedCommit);

        if (isParentAncestorOfChild) {
          if (forkPoint != null && !forkPoint.equals(parentPointedCommit)) {
            LOG.debug(
                () -> "For this branch (${coreLocalBranch.getShortName()}) its parent's commit is ancestor of this branch pointed commit "
                    + "but fork point is not equal to parent commit, so we assume that this branch is 'InSyncButForkPointOff'");
            return SyncToParentStatus.InSyncButForkPointOff;
          } else {
            LOG.debug(
                () -> "For this branch (${coreLocalBranch.getShortName()}) its parent's commit is ancestor of this branch pointed commit and fork point "
                    + "and fork point is absent or equal to parent commit, so we assume that this branch is in sync");
            return SyncToParentStatus.InSync;
          }
        } else {
          var isChildAncestorOfParent = gitCoreRepository.isAncestor(
              /* presumedAncestor */ pointedCommit, /* presumedDescendant */ parentPointedCommit);

          if (isChildAncestorOfParent) {
            LOG.debug(
                () -> "For this branch (${coreLocalBranch.getShortName()}) its parent's commit is not ancestor of this branch pointed commit "
                    + "but this branch pointed commit is ancestor of parent branch commit, so we assume that this branch is merged");
            return SyncToParentStatus.MergedToParent;
          } else {
            LOG.debug(
                () -> "For this branch (${coreLocalBranch.getShortName()}) its parent's commit is not ancestor of this branch pointed commit "
                    + "neither this branch pointed commit is ancestor of parent branch commit, so we assume that this branch is out of sync");
            return SyncToParentStatus.OutOfSync;
          }
        }
      }

    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }
}
