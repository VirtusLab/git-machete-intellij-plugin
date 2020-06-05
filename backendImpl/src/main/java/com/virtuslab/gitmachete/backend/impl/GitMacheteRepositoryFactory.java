package com.virtuslab.gitmachete.backend.impl;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.AheadOfRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.BehindRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedFromAndNewerThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedFromAndOlderThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.InSyncToRemote;

import java.nio.file.Path;
import java.time.Instant;
import java.util.function.Predicate;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Queue;
import io.vavr.collection.Seq;
import io.vavr.control.Try;
import lombok.CustomLog;
import lombok.Value;
import lombok.With;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.IBranchLayoutEntry;
import com.virtuslab.gitcore.api.GitCoreBranchTrackingStatus;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreBranch;
import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreCommitHash;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCoreReflogEntry;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranch;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitcore.api.IGitCoreRepositoryFactory;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
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

    var result = new Aux(gitCoreRepository, statusHookExecutor).createGitMacheteRepository(branchLayout);
    LOG.withTimeElapsed().debug("Finished");
    return result;
  }

  private static class Aux {
    private final IGitCoreRepository gitCoreRepository;
    private final StatusBranchHookExecutor statusHookExecutor;
    private final Map<String, IGitCoreLocalBranch> localBranchByName;
    private final Map<String, IGitCoreRemoteBranch> remoteBranchByName;
    private final java.util.Map<IGitCoreBranch, List<IGitCoreReflogEntry>> filteredReflogByBranch = new java.util.HashMap<>();

    Aux(IGitCoreRepository gitCoreRepository, StatusBranchHookExecutor statusHookExecutor) throws GitMacheteException {
      this.gitCoreRepository = gitCoreRepository;
      this.statusHookExecutor = statusHookExecutor;
      try {
        this.localBranchByName = gitCoreRepository.deriveAllLocalBranches()
            .toMap(localBranch -> Tuple.of(localBranch.getShortName(), localBranch));
        this.remoteBranchByName = gitCoreRepository.deriveAllRemoteBranches()
            .toMap(remoteBranch -> Tuple.of(remoteBranch.getShortName(), remoteBranch));
      } catch (GitCoreException e) {
        throw new GitMacheteException(e);
      }
    }

    IGitMacheteRepository createGitMacheteRepository(IBranchLayout branchLayout) throws GitMacheteException {
      var rootBranchTries = branchLayout.getRootEntries().map(entry -> Try.of(() -> createGitMacheteRootBranch(entry)));
      var rootBranches = Try.sequence(rootBranchTries).getOrElseThrow(GitMacheteException::getOrWrap);

      var branchByName = createBranchByNameMap(rootBranches);

      IGitMacheteBranch currentBranch = Try.of(() -> gitCoreRepository.deriveCurrentBranch())
          .getOrElseThrow(e -> new GitMacheteException("Can't get current branch", e))
          .flatMap(cb -> branchByName.get(cb.getShortName()))
          .getOrNull();
      LOG.debug(() -> "Current branch: ${currentBranch != null ? currentBranch.getName() : \"<none> (detached HEAD)\"}");

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
        IBranchLayoutEntry entry) throws GitCoreException, GitMacheteException {

      var branchName = entry.getName();
      IGitCoreLocalBranch coreLocalBranch = localBranchByName.get(branchName)
          .getOrElseThrow(() -> new GitMacheteException("Branch '${branchName}' not found in the repository"));

      IGitCoreCommit corePointedCommit = coreLocalBranch.derivePointedCommit();

      var pointedCommit = new GitMacheteCommit(corePointedCommit);
      var syncToRemoteStatus = deriveSyncToRemoteStatus(coreLocalBranch);
      var customAnnotation = entry.getCustomAnnotation().getOrNull();
      var downstreamBranches = deriveDownstreamBranches(coreLocalBranch, entry);
      var remoteBranch = getRemoteBranchFromCoreLocalBranch(coreLocalBranch);
      var statusHookOutput = statusHookExecutor.deriveHookOutputFor(branchName, pointedCommit).getOrNull();

      return new GitMacheteRootBranch(branchName, downstreamBranches, pointedCommit, remoteBranch, syncToRemoteStatus,
          customAnnotation, statusHookOutput);
    }

    private GitMacheteNonRootBranch createGitMacheteNonRootBranch(
        IGitCoreLocalBranch parentCoreLocalBranch,
        IBranchLayoutEntry entry) throws GitCoreException, GitMacheteException {

      var branchName = entry.getName();

      IGitCoreLocalBranch coreLocalBranch = localBranchByName.get(branchName)
          .getOrElseThrow(() -> new GitMacheteException("Branch '${branchName}' not found in the repository"));

      IGitCoreCommit corePointedCommit = coreLocalBranch.derivePointedCommit();

      CoreForkPoint coreForkPoint = deriveParentAwareForkPoint(coreLocalBranch, parentCoreLocalBranch);
      IGitCoreCommit coreForkPointCommit = coreForkPoint != null ? coreForkPoint.coreCommit : null;

      var syncToParentStatus = deriveSyncToParentStatus(coreLocalBranch, parentCoreLocalBranch, coreForkPointCommit);

      // translate IGitCoreCommit list to IGitMacheteCommit list
      List<IGitCoreCommit> commits;
      if (coreForkPointCommit == null) {
        // That's a rare case in practice, mostly happens due to reflog expiry.
        commits = List.empty();
      } else if (syncToParentStatus == SyncToParentStatus.InSyncButForkPointOff) {
        // In case of yellow edge, we include the entire range from the commit pointed by the branch until its parent,
        // and not until just its fork point. This makes it possible to highlight the fork point candidate on the commit listing.
        commits = gitCoreRepository.deriveCommitRange(corePointedCommit, parentCoreLocalBranch.derivePointedCommit());
      } else {
        // We're handling the cases of green, gray and red edges here.
        commits = gitCoreRepository.deriveCommitRange(corePointedCommit, coreForkPointCommit);
      }

      var pointedCommit = new GitMacheteCommit(corePointedCommit);
      var forkPoint = coreForkPoint != null
          ? new GitMacheteForkPointCommit(coreForkPoint.coreCommit, coreForkPoint.containingBranches)
          : null;
      var syncToRemoteStatus = deriveSyncToRemoteStatus(coreLocalBranch);
      var customAnnotation = entry.getCustomAnnotation().getOrNull();
      var downstreamBranches = deriveDownstreamBranches(coreLocalBranch, entry);
      var remoteBranch = getRemoteBranchFromCoreLocalBranch(coreLocalBranch);
      var statusHookOutput = statusHookExecutor.deriveHookOutputFor(branchName, pointedCommit).getOrNull();

      return new GitMacheteNonRootBranch(branchName, downstreamBranches, pointedCommit, remoteBranch, syncToRemoteStatus,
          customAnnotation, statusHookOutput, forkPoint, commits.map(GitMacheteCommit::new), syncToParentStatus);
    }

    private @Nullable IGitMacheteRemoteBranch getRemoteBranchFromCoreLocalBranch(IGitCoreLocalBranch coreLocalBranch)
        throws GitMacheteException {
      IGitCoreRemoteBranch coreRemoteBranch = coreLocalBranch.getRemoteTrackingBranch().getOrNull();
      if (coreRemoteBranch == null) {
        return null;
      }
      IGitCoreCommit coreRemoteBranchPointedCommit = Try.of(() -> coreRemoteBranch.derivePointedCommit())
          .getOrElseThrow(e -> new GitMacheteException("Cannot get core remote branch pointed commit", e));
      return new GitMacheteRemoteBranch(new GitMacheteCommit(coreRemoteBranchPointedCommit));
    }

    @Value(staticConstructor = "of")
    @With
    private static class CoreForkPoint {
      IGitCoreCommit coreCommit;
      List<String> containingBranches;
    }

    private @Nullable CoreForkPoint deriveParentAwareForkPoint(
        IGitCoreLocalBranch coreLocalBranch,
        IGitCoreLocalBranch parentCoreLocalBranch) throws GitCoreException {
      LOG.debug(() -> "Entering: gitCoreRepository = ${gitCoreRepository}, " +
          "coreLocalBranch = '${coreLocalBranch.getShortName()}', parentCoreLocalBranch = '${parentCoreLocalBranch.getShortName()}'");
      var coreForkPoint = deriveParentAgnosticForkPoint(coreLocalBranch);
      var forkPointString = coreForkPoint != null ? coreForkPoint.toString() : "empty";
      var parentPointedCommit = parentCoreLocalBranch.derivePointedCommit();
      var pointedCommit = coreLocalBranch.derivePointedCommit();

      LOG.debug(() -> "coreForkPoint = ${forkPointString}, " +
          "parentPointedCommit = ${parentPointedCommit.getHash().getHashString()}, " +
          "pointedCommit = ${pointedCommit.getHash().getHashString()}");

      var isParentAncestorOfChild = gitCoreRepository.isAncestor(parentPointedCommit, pointedCommit);

      LOG.debug(() -> "Parent branch commit (${parentPointedCommit.getHash().getHashString()}) " +
          "is ${isParentAncestorOfChild ? \"\" : \" NOT\"} ancestor of child commit " +
          "(${pointedCommit.getHash().getHashString()})");

      if (isParentAncestorOfChild) {
        if (coreForkPoint != null) {
          var isParentAncestorOfForkPoint = gitCoreRepository.isAncestor(parentPointedCommit, coreForkPoint.coreCommit);

          if (!isParentAncestorOfForkPoint) {
            // If parent(A) is ancestor of A, and parent(A) is NOT ancestor of fork-point(A),
            // then assume fork-point(A)=parent(A)
            LOG.debug(() -> "Parent branch commit (${parentPointedCommit.getHash().getHashString()}) is ancestor of " +
                "pointed commit (${pointedCommit.getHash().getHashString()}) but parent branch commit " +
                "is NOT ancestor of pointed commit fork point (${forkPointString}), " +
                "so we assume that pointed commit fork point = parent branch commit");
            return coreForkPoint.withCoreCommit(parentPointedCommit);
          }

        } else {
          // If parent(A) is ancestor of A, and fork-point(A) is missing,
          // then assume fork-point(A)=parent(A)
          LOG.debug(
              () -> "Parent branch commit (${parentPointedCommit.getHash().getHashString()}) is ancestor of pointed commit " +
                  "(${pointedCommit.getHash().getHashString()}) but fork point of pointed commit is missing, " +
                  "so we assume that pointed commit fork point = parent branch commit");
          return CoreForkPoint.of(parentPointedCommit, List.empty());
        }
      }

      // String interpolation caused some weird Nullness Checker issues (exception from `com.sun.tools.javac`) in this line.
      LOG.debug(() -> "Deduced fork point for branch " + coreLocalBranch.getShortName() + " is " + forkPointString);

      return coreForkPoint;
    }

    private @Nullable CoreForkPoint deriveParentAgnosticForkPoint(IGitCoreLocalBranch branch) throws GitCoreException {
      LOG.debug(() -> "Entering: branch = '${branch.getFullName()}'");

      LOG.debug("Getting reflogs of local branches");

      Map<String, List<IGitCoreReflogEntry>> filteredReflogByLocalBranchName = localBranchByName
          .rejectValues(branch::equals)
          .mapValues(otherLocalBranch -> Try.of(() -> deriveFilteredReflog(otherLocalBranch)).get());

      LOG.debug("Getting reflogs of remote branches");

      IGitCoreRemoteBranch remoteTrackingBranch = branch.getRemoteTrackingBranch().getOrNull();

      Map<String, List<IGitCoreReflogEntry>> filteredReflogByRemoteBranchName = remoteBranchByName
          .rejectValues(someRemoteBranch -> someRemoteBranch.equals(remoteTrackingBranch))
          .mapValues(unrelatedRemoteBranch -> Try.of(() -> deriveFilteredReflog(unrelatedRemoteBranch)).get());

      Map<String, List<IGitCoreReflogEntry>> filteredReflogsByBranchName = filteredReflogByLocalBranchName
          .merge(filteredReflogByRemoteBranchName);

      Seq<Tuple2<IGitCoreCommitHash, String>> objectIdAndBranchNamePairs = filteredReflogsByBranchName
          .flatMap(bnAres -> bnAres._2.map(re -> Tuple.of(re.getNewCommitHash(), bnAres._1)));
      Map<IGitCoreCommitHash, Seq<String>> branchesContainingInReflogByCommit = objectIdAndBranchNamePairs
          .groupBy(oidAbn -> oidAbn._1)
          .mapValues(oidAbns -> oidAbns.map(oidAbn -> oidAbn._2));

      LOG.debug("Start walking through logs");

      IGitCoreCommit forkPoint = gitCoreRepository.findFirstAncestor(branch.derivePointedCommit(), commitHash -> {
        Seq<String> containingBranches = branchesContainingInReflogByCommit.getOrElse(commitHash, List.empty());
        return containingBranches.nonEmpty();
      }).getOrNull();

      if (forkPoint != null) {
        // We now know that this list is non-empty.
        List<String> containingBranches = branchesContainingInReflogByCommit.getOrElse(forkPoint.getHash(), List.empty())
            .toList();
        LOG.debug(() -> "Commit ${forkPoint} found in filtered reflog(s) of ${containingBranches.mkString(\", \")}; " +
            "returning as fork point for branch '${branch.getFullName()}'");
        return CoreForkPoint.of(forkPoint, containingBranches);
      } else {
        LOG.debug(() -> "Fork for branch '${branch.getFullName()}' not found ");
        return null;
      }
    }

    private List<GitMacheteNonRootBranch> deriveDownstreamBranches(
        IGitCoreLocalBranch parentCoreLocalBranch,
        IBranchLayoutEntry directUpstreamEntry) throws GitCoreException {

      var downstreamBranchTries = directUpstreamEntry.getSubentries().map(entry -> Try.of(
          () -> createGitMacheteNonRootBranch(parentCoreLocalBranch, entry)));
      var downstreamBranches = Try.sequence(downstreamBranchTries).getOrElseThrow(GitCoreException::getOrWrap);
      return List.ofAll(downstreamBranches);
    }

    private SyncToRemoteStatus deriveSyncToRemoteStatus(IGitCoreLocalBranch coreLocalBranch) throws GitCoreException {
      String localBranchShortName = coreLocalBranch.getShortName();
      LOG.debug(() -> "Entering: coreLocalBranch = '${localBranchShortName}'");

      IGitCoreRemoteBranch coreRemoteBranch = coreLocalBranch.getRemoteTrackingBranch().getOrNull();
      if (coreRemoteBranch == null) {
        LOG.debug(() -> "Branch '${localBranchShortName}' is untracked");
        return SyncToRemoteStatus.untracked();
      }

      GitCoreBranchTrackingStatus trackingStatus = gitCoreRepository.deriveRemoteTrackingStatus(coreLocalBranch).getOrNull();
      if (trackingStatus == null) {
        LOG.debug(() -> "Tracking status for '${localBranchShortName}' could not be determined");
        return SyncToRemoteStatus.untracked();
      }

      String remoteName = coreRemoteBranch.getRemoteName();
      SyncToRemoteStatus syncToRemoteStatus;

      if (trackingStatus.getAhead() > 0 && trackingStatus.getBehind() > 0) {
        Instant localBranchCommitDate = coreLocalBranch.derivePointedCommit().getCommitTime();
        Instant remoteBranchCommitDate = coreRemoteBranch.derivePointedCommit().getCommitTime();
        // In case when commit dates are equal we assume that our relation is `DivergedAndNewerThanRemote`
        if (remoteBranchCommitDate.compareTo(localBranchCommitDate) > 0) {
          syncToRemoteStatus = SyncToRemoteStatus.of(DivergedFromAndOlderThanRemote, remoteName);
        } else {
          if (remoteBranchCommitDate.compareTo(localBranchCommitDate) == 0) {
            LOG.debug("Commit dates of both local and remote branches are the same, so we assume " +
                "'DivergedAndNewerThanRemote' sync to remote status");
          }
          syncToRemoteStatus = SyncToRemoteStatus.of(DivergedFromAndNewerThanRemote, remoteName);
        }
      } else if (trackingStatus.getAhead() > 0) {
        syncToRemoteStatus = SyncToRemoteStatus.of(AheadOfRemote, remoteName);
      } else if (trackingStatus.getBehind() > 0) {
        syncToRemoteStatus = SyncToRemoteStatus.of(BehindRemote, remoteName);
      } else {
        syncToRemoteStatus = SyncToRemoteStatus.of(InSyncToRemote, remoteName);
      }

      LOG.debug(() -> "Sync to remote status for branch '${localBranchShortName}': ${syncToRemoteStatus.toString()}");

      return syncToRemoteStatus;
    }

    /**
     * @return reflog entries, excluding branch creation and branch reset events irrelevant for fork point/upstream inference,
     * ordered from the latest to the oldest
     */
    private List<IGitCoreReflogEntry> deriveFilteredReflog(IGitCoreBranch branch) throws GitCoreException {
      if (filteredReflogByBranch.containsKey(branch)) {
        return filteredReflogByBranch.get(branch);
      }

      LOG.trace(() -> "Entering: branch = '${branch.getFullName()}'; original list of entries:");

      List<IGitCoreReflogEntry> reflogEntries = branch.deriveReflog();
      reflogEntries.forEach(entry -> LOG.trace(() -> "* ${entry}"));

      IGitCoreCommitHash entryToExcludeNewId;
      if (reflogEntries.size() > 0) {
        var firstEntry = reflogEntries.get(reflogEntries.size() - 1);
        String createdFromPrefix = "branch: Created from";
        if (firstEntry.getComment().startsWith(createdFromPrefix)) {
          entryToExcludeNewId = firstEntry.getNewCommitHash();
          LOG.debug(
              () -> "All entries with the same hash as first entry (${firstEntry.getNewCommitHash().toString()}) will be excluded "
                  + "because first entry comment starts with '${createdFromPrefix}'");
        } else {
          entryToExcludeNewId = null;
        }
      } else {
        entryToExcludeNewId = null;
      }

      String rebaseComment = "rebase finished: " + branch.getFullName() + " onto "
          + Try.of(() -> branch.derivePointedCommit().getHash().getHashString()).getOrElse("");

      // It's necessary to exclude entry with the same hash as the first entry in reflog (if it still exists)
      // for cases like branch rename just after branch creation
      Predicate<IGitCoreReflogEntry> isEntryExcluded = e -> {
        // For debug logging only
        String newIdHash = e.getNewCommitHash().getHashString();

        if (e.getNewCommitHash().equals(entryToExcludeNewId)) {
          LOG.debug(() -> "Exclude ${newIdHash} because it has the same hash as first entry");
        } else if (e.getOldCommitHash().isDefined() && e.getNewCommitHash().equals(e.getOldCommitHash().get())) {
          LOG.debug(() -> "Exclude ${newIdHash} because its old and new IDs are the same");
        } else if (e.getComment().startsWith("branch: Created from")) {
          LOG.debug(() -> "Exclude ${newIdHash} because its comment starts with 'branch: Created from'");
        } else if (e.getComment().equals("branch: Reset to " + branch.getShortName())) {
          LOG.debug(() -> "Exclude ${newIdHash} because its comment is 'branch: Reset to ${branch.getShortName()}'");
        } else if (e.getComment().equals("branch: Reset to HEAD")) {
          LOG.debug(() -> "Exclude ${newIdHash} because its comment is 'branch: Reset to HEAD'");
        } else if (e.getComment().startsWith("reset: moving to ")) {
          LOG.debug(() -> "Exclude ${newIdHash} because its comment starts with 'reset: moving to '");
        } else if (e.getComment().equals(rebaseComment)) {
          LOG.debug(() -> "Exclude ${newIdHash} because its comment is '${rebaseComment}'");
        } else {
          return false;
        }

        return true;
      };

      var result = reflogEntries.reject(isEntryExcluded);
      filteredReflogByBranch.put(branch, result);
      return result;
    }

    private boolean hasJustBeenCreated(IGitCoreLocalBranch branch) throws GitCoreException {
      List<IGitCoreReflogEntry> reflog = deriveFilteredReflog(branch);
      return reflog.isEmpty() || reflog.head().getOldCommitHash().isEmpty();
    }

    private SyncToParentStatus deriveSyncToParentStatus(
        IGitCoreLocalBranch coreLocalBranch,
        IGitCoreLocalBranch parentCoreLocalBranch,
        @Nullable IGitCoreCommit forkPoint)
        throws GitCoreException {
      LOG.debug(() -> "Entering: gitCoreRepository = ${gitCoreRepository}, " +
          "coreLocalBranch = '${coreLocalBranch.getShortName()}', parentCoreLocalBranch = '${parentCoreLocalBranch.getShortName()}', "
          + "forkPoint = ${forkPoint != null ? forkPoint.getHash().getHashString() : \"null\"})");

      IGitCoreCommit parentPointedCommit = parentCoreLocalBranch.derivePointedCommit();
      IGitCoreCommit pointedCommit = coreLocalBranch.derivePointedCommit();

      LOG.debug(() -> "parentPointedCommit = ${parentPointedCommit.getHash().getHashString()}; " +
          "pointedCommit = ${pointedCommit.getHash().getHashString()}");

      if (pointedCommit.equals(parentPointedCommit)) {
        if (hasJustBeenCreated(coreLocalBranch)) {
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
    }
  }
}
