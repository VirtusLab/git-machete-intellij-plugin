package com.virtuslab.gitmachete.backend.impl;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.AheadOfRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.BehindRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedFromAndNewerThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedFromAndOlderThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.InSyncToRemote;

import java.nio.file.Path;
import java.time.Instant;
import java.util.function.Function;
import java.util.function.Predicate;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Queue;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.CustomLog;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.IBranchLayoutEntry;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.GitCoreRelativeCommitCount;
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
import com.virtuslab.gitmachete.backend.api.IGitMacheteRootBranch;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.backend.impl.hooks.PreRebaseHookExecutor;
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
    var preRebaseHookExecutor = PreRebaseHookExecutor.of(mainDirectoryPath, gitDirectoryPath);

    var result = new Aux(gitCoreRepository, statusHookExecutor, preRebaseHookExecutor).createGitMacheteRepository(branchLayout);
    LOG.withTimeElapsed().info("Finished");
    return result;
  }

  private static class Aux {
    private final IGitCoreRepository gitCoreRepository;
    private final StatusBranchHookExecutor statusHookExecutor;
    private final PreRebaseHookExecutor preRebaseHookExecutor;
    private final List<IGitCoreLocalBranch> localBranches;
    private final Map<String, IGitCoreLocalBranch> localBranchByName;
    private final List<String> remoteNames;

    private final java.util.Map<IGitCoreBranch, List<IGitCoreReflogEntry>> filteredReflogByBranch = new java.util.HashMap<>();
    private @MonotonicNonNull Map<IGitCoreCommitHash, Seq<String>> branchesContainingGivenCommitInReflog;

    Aux(
        IGitCoreRepository gitCoreRepository,
        StatusBranchHookExecutor statusHookExecutor,
        PreRebaseHookExecutor preRebaseHookExecutor) throws GitMacheteException {

      this.gitCoreRepository = gitCoreRepository;
      this.statusHookExecutor = statusHookExecutor;
      this.preRebaseHookExecutor = preRebaseHookExecutor;

      try {
        this.localBranches = gitCoreRepository.deriveAllLocalBranches();
        this.localBranchByName = localBranches.toMap(localBranch -> Tuple.of(localBranch.getName(), localBranch));
        this.remoteNames = gitCoreRepository.deriveAllRemoteNames();
      } catch (GitCoreException e) {
        throw new GitMacheteException(e);
      }
    }

    IGitMacheteRepository createGitMacheteRepository(IBranchLayout branchLayout) throws GitMacheteException {
      var rootBranchTries = branchLayout.getRootEntries().map(entry -> Try.of(() -> createGitMacheteRootBranch(entry)));
      var rootBranches = Try.sequence(rootBranchTries).getOrElseThrow(GitMacheteException::getOrWrap).toList();

      var branchByName = createBranchByNameMap(rootBranches);

      Option<IGitCoreLocalBranch> coreCurrentBranch = Try.of(() -> gitCoreRepository.deriveCurrentBranch())
          .getOrElseThrow(e -> new GitMacheteException("Can't get current branch", e));
      LOG.debug(() -> "Current branch: " + (coreCurrentBranch.isDefined()
          ? coreCurrentBranch.get().getName()
          : "<none> (detached HEAD)"));

      IGitMacheteBranch currentBranchIfManaged = coreCurrentBranch
          .flatMap(cb -> branchByName.get(cb.getName()))
          .getOrNull();
      LOG.debug(() -> "Current Git Machete branch (if managed): " + (currentBranchIfManaged != null
          ? currentBranchIfManaged.getName()
          : "<none> (unmanaged branch or detached HEAD)"));

      return new GitMacheteRepository(rootBranches, branchLayout, currentBranchIfManaged, branchByName, preRebaseHookExecutor);
    }

    private Map<IGitCoreCommitHash, Seq<String>> deriveBranchesContainingGivenCommitInReflog() {
      if (branchesContainingGivenCommitInReflog != null) {
        return branchesContainingGivenCommitInReflog;
      }

      LOG.debug("Getting reflogs of local branches");

      Map<String, List<IGitCoreReflogEntry>> filteredReflogByLocalBranchName = localBranches
          .toMap(
              /* keyMapper */ localBranch -> localBranch.getName(),
              /* valueMapper */ localBranch -> Try.of(() -> deriveFilteredReflog(localBranch)).getOrElse(List.empty()));

      LOG.debug("Getting reflogs of remote branches");

      List<IGitCoreRemoteBranch> remoteTrackingBranches = localBranches
          .flatMap(localBranch -> localBranch.getRemoteTrackingBranch().toList());

      Map<String, List<IGitCoreReflogEntry>> filteredReflogByRemoteTrackingBranchName = remoteTrackingBranches
          .toMap(
              /* keyMapper */ remoteBranch -> remoteBranch.getName(),
              /* valueMapper */ remoteBranch -> Try.of(() -> deriveFilteredReflog(remoteBranch)).getOrElse(List.empty()));

      LOG.debug("Converting reflogs to mapping of branches containing in reflog by commit");

      Map<String, List<IGitCoreReflogEntry>> filteredReflogsByBranchName = filteredReflogByLocalBranchName
          .merge(filteredReflogByRemoteTrackingBranchName);

      LOG.trace(() -> "Filtered reflogs by branch name:");
      LOG.trace(() -> filteredReflogsByBranchName
          .map(kv -> kv._1 + " -> " + kv._2.map(e -> e.getNewCommitHash()).mkString(", "))
          .sorted().mkString(System.lineSeparator()));

      Seq<Tuple2<IGitCoreCommitHash, String>> commitHashAndBranchNamePairs = filteredReflogsByBranchName
          .flatMap(branchNameAndReflog -> branchNameAndReflog._2
              .map(re -> Tuple.of(re.getNewCommitHash(), branchNameAndReflog._1)));

      var result = commitHashAndBranchNamePairs
          .groupBy(commitHashAndBranchName -> commitHashAndBranchName._1)
          .mapValues(pairsOfCommitHashAndBranchName -> pairsOfCommitHashAndBranchName
              .map(commitHashAndBranchName -> commitHashAndBranchName._2));

      LOG.debug(() -> "Derived the map of branches containing given commit in reflog:");
      LOG.debug(() -> result.toList().map(kv -> kv._1 + " -> " + kv._2.mkString(", "))
          .sorted().mkString(System.lineSeparator()));
      branchesContainingGivenCommitInReflog = result;
      return result;
    }

    private Map<String, IGitMacheteBranch> createBranchByNameMap(List<IGitMacheteRootBranch> rootBranches) {
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

    private IGitMacheteRootBranch createGitMacheteRootBranch(
        IBranchLayoutEntry entry) throws GitCoreException, GitMacheteException {

      var branchName = entry.getName();
      IGitCoreLocalBranch coreLocalBranch = localBranchByName.get(branchName)
          .getOrElseThrow(() -> new GitMacheteException("Branch '${branchName}' not found in the repository"));

      IGitCoreCommit corePointedCommit = coreLocalBranch.derivePointedCommit();

      var pointedCommit = new GitMacheteCommit(corePointedCommit);
      var syncToRemoteStatus = deriveSyncToRemoteStatus(coreLocalBranch);
      var customAnnotation = entry.getCustomAnnotation().getOrNull();
      var downstreamBranches = deriveDownstreamBranches(coreLocalBranch, entry.getSubentries());
      var remoteTrackingBranch = getRemoteTrackingBranchForCoreLocalBranch(coreLocalBranch);
      var statusHookOutput = statusHookExecutor.deriveHookOutputFor(branchName, pointedCommit).getOrNull();

      return new GitMacheteRootBranch(branchName, downstreamBranches, pointedCommit, remoteTrackingBranch,
          syncToRemoteStatus, customAnnotation, statusHookOutput);
    }

    private List<GitMacheteNonRootBranch> createGitMacheteNonRootBranch(
        IGitCoreLocalBranch parentCoreLocalBranch,
        IBranchLayoutEntry entry) throws GitCoreException {

      var branchName = entry.getName();

      IGitCoreLocalBranch coreLocalBranch = localBranchByName.get(branchName).getOrNull();
      if (coreLocalBranch == null) {
        return deriveDownstreamBranches(parentCoreLocalBranch, entry.getSubentries());
      }

      IGitCoreCommit corePointedCommit = coreLocalBranch.derivePointedCommit();

      GitMacheteForkPointCommit forkPoint = deriveParentAwareForkPoint(coreLocalBranch, parentCoreLocalBranch);

      var syncToParentStatus = deriveSyncToParentStatus(coreLocalBranch, parentCoreLocalBranch, forkPoint);

      List<IGitCoreCommit> commits;
      if (forkPoint == null) {
        // That's a rare case in practice, mostly happens due to reflog expiry.
        commits = List.empty();
      } else if (syncToParentStatus == SyncToParentStatus.InSyncButForkPointOff) {
        // In case of yellow edge, we include the entire range from the commit pointed by the branch until its parent,
        // and not until just its fork point. This makes it possible to highlight the fork point candidate on the commit listing.
        commits = gitCoreRepository.deriveCommitRange(corePointedCommit, parentCoreLocalBranch.derivePointedCommit());
      } else {
        // We're handling the cases of green, gray and red edges here.
        commits = gitCoreRepository.deriveCommitRange(corePointedCommit, forkPoint.getCoreCommit());
      }

      var pointedCommit = new GitMacheteCommit(corePointedCommit);
      var syncToRemoteStatus = deriveSyncToRemoteStatus(coreLocalBranch);
      var customAnnotation = entry.getCustomAnnotation().getOrNull();
      var downstreamBranches = deriveDownstreamBranches(coreLocalBranch, entry.getSubentries());
      var remoteTrackingBranch = getRemoteTrackingBranchForCoreLocalBranch(coreLocalBranch);
      var statusHookOutput = statusHookExecutor.deriveHookOutputFor(branchName, pointedCommit).getOrNull();

      var result = new GitMacheteNonRootBranch(branchName, downstreamBranches, pointedCommit, remoteTrackingBranch,
          syncToRemoteStatus, customAnnotation, statusHookOutput,
          forkPoint, commits.map(GitMacheteCommit::new), syncToParentStatus);
      return List.of(result);
    }

    private @Nullable IGitMacheteRemoteBranch getRemoteTrackingBranchForCoreLocalBranch(IGitCoreLocalBranch coreLocalBranch)
        throws GitCoreException {
      IGitCoreRemoteBranch coreRemoteBranch = coreLocalBranch.getRemoteTrackingBranch().getOrNull();
      if (coreRemoteBranch == null) {
        return null;
      }
      return new GitMacheteRemoteBranch(new GitMacheteCommit(coreRemoteBranch.derivePointedCommit()));
    }

    private @Nullable GitMacheteForkPointCommit deriveParentAwareForkPoint(
        IGitCoreLocalBranch coreLocalBranch,
        IGitCoreLocalBranch parentCoreLocalBranch) throws GitCoreException {
      LOG.startTimer().debug(() -> "Entering: coreLocalBranch = '${coreLocalBranch.getName()}', " +
          "parentCoreLocalBranch = '${parentCoreLocalBranch.getName()}'");

      IGitCoreCommit overriddenForkPointCommit = deriveParentAgnosticOverriddenForkPoint(coreLocalBranch);
      GitMacheteForkPointCommit parentAgnosticForkPoint = overriddenForkPointCommit != null
          ? GitMacheteForkPointCommit.overridden(overriddenForkPointCommit)
          : deriveParentAgnosticInferredForkPoint(coreLocalBranch);

      var parentAgnosticForkPointString = parentAgnosticForkPoint != null ? parentAgnosticForkPoint.toString() : "empty";
      var parentPointedCommit = parentCoreLocalBranch.derivePointedCommit();
      var pointedCommit = coreLocalBranch.derivePointedCommit();

      LOG.debug(() -> "parentAgnosticForkPoint = ${parentAgnosticForkPointString}, " +
          "parentPointedCommit = ${parentPointedCommit.getHash().getHashString()}, " +
          "pointedCommit = ${pointedCommit.getHash().getHashString()}");

      var isParentAncestorOfChild = gitCoreRepository.isAncestor(parentPointedCommit, pointedCommit);

      LOG.debug(() -> "Parent branch commit (${parentPointedCommit.getHash().getHashString()}) " +
          "is${isParentAncestorOfChild ? \"\" : \" NOT\"} ancestor of child commit " +
          "(${pointedCommit.getHash().getHashString()})");

      if (isParentAncestorOfChild) {
        if (parentAgnosticForkPoint != null) {
          var isParentAncestorOfForkPoint = gitCoreRepository.isAncestor(parentPointedCommit,
              parentAgnosticForkPoint.getCoreCommit());

          if (!isParentAncestorOfForkPoint) {
            // If parent(A) is ancestor of A, and parent(A) is NOT ancestor of fork-point(A),
            // then assume fork-point(A)=parent(A)
            LOG.debug(() -> "Parent branch commit (${parentPointedCommit.getHash().getHashString()}) is ancestor of " +
                "pointed commit (${pointedCommit.getHash().getHashString()}) but parent branch commit " +
                "is NOT ancestor of parent-agnostic fork point (${parentAgnosticForkPointString}), " +
                "so we assume that parent-aware fork point = parent branch commit");
            return GitMacheteForkPointCommit.parentFallback(parentPointedCommit);
          }

        } else {
          // If parent(A) is ancestor of A, and fork-point(A) is missing,
          // then assume fork-point(A)=parent(A)
          LOG.debug(() -> "Parent branch commit (${parentPointedCommit.getHash().getHashString()}) is ancestor of " +
              "pointed commit (${pointedCommit.getHash().getHashString()}) and parent-agnostic fork point is missing, " +
              "so we assume that parent-aware fork point = parent branch commit");
          return GitMacheteForkPointCommit.parentFallback(parentPointedCommit);
        }
      }

      // String interpolation caused some weird Nullness Checker issues (exception from `com.sun.tools.javac`) in this line.
      LOG.withTimeElapsed().debug(() -> "Parent-aware fork point for branch " + coreLocalBranch.getName() +
          " is " + parentAgnosticForkPointString);

      return parentAgnosticForkPoint;
    }

    private @Nullable IGitCoreCommit deriveParentAgnosticOverriddenForkPoint(IGitCoreLocalBranch coreLocalBranch)
        throws GitCoreException {
      String section = "machete";
      String subsectionPrefix = "overrideForkPoint";
      String branchName = coreLocalBranch.getName();

      // Section spans the characters before the first dot
      // Name spans the characters after the first dot
      // Subsection is everything else
      String toRevision = gitCoreRepository
          .deriveConfigValue(section, subsectionPrefix + "." + branchName, "to").getOrNull();
      String whileDescendantOfRevision = gitCoreRepository
          .deriveConfigValue(section, subsectionPrefix + "." + branchName, "whileDescendantOf").getOrNull();
      if (toRevision == null || whileDescendantOfRevision == null) {
        return null;
      }
      LOG.debug(() -> "Fork point override config for '${branchName}': " +
          "to='${toRevision}', whileDescendantOf='${whileDescendantOfRevision}'");

      // Let's check the internal consistency of the config - we can't rule out that it's been tampered with.
      IGitCoreCommit to = gitCoreRepository.parseRevision(toRevision).getOrNull();
      IGitCoreCommit whileDescendantOf = gitCoreRepository.parseRevision(whileDescendantOfRevision).getOrNull();
      if (to == null || whileDescendantOf == null) {
        LOG.warn("Could not parse either <to> (${to}) or " +
            "<whileDescendantOf> (${whileDescendantOf}) into a valid commit, ignoring faulty fork point override");
        return null;
      }
      // Yes, that's not a bug. We're checking whether `whileDescendantOf` is a descendant of `to`, not the other way round.
      // The `descendant of` part of `whileDescendantOf` refers to the *current branch* being a descendant
      // of whatever `whileDescendantOf` points to.
      if (!gitCoreRepository.isAncestor(to, whileDescendantOf)) {
        LOG.warn("Commit <to> (${to}) is NOT an ancestor of " +
            "<whileDescendantOf> (${whileDescendantOf}), ignoring faulty fork point override");
        return null;
      }

      // Now we know that the override config is consistent, but it still doesn't mean
      // that it actually applies to the given branch AT THIS POINT (it could e.g. have applied earlier but now no longer applies).
      var branchCommit = coreLocalBranch.derivePointedCommit();
      if (!gitCoreRepository.isAncestor(whileDescendantOf, branchCommit)) {
        LOG.debug(() -> "Branch ${branchName} (${branchCommit}) is NOT a descendant of " +
            "<whileDescendantOf> (${whileDescendantOf}), ignoring outdated fork point override");
        return null;
      }

      // Now we know that:
      //   to <-- whileDescendantOf <-- branchCommit
      // so the fork point override is internally consistent and applies to the commit currently pointed by the branch.
      // Note that we still need to validate whether the fork point is a descendant of the branch's parent,
      // but this will happen in parent-aware logic (and we're parent-agnostic here yet).
      LOG.debug(() -> "Applying fork point override for '${branchName}' (${branchCommit}): " +
          "to=${to}, whileDescendantOf=${whileDescendantOf}");
      return to;
    }

    private @Nullable GitMacheteForkPointCommit deriveParentAgnosticInferredForkPoint(IGitCoreLocalBranch branch)
        throws GitCoreException {
      LOG.debug(() -> "Entering: branch = '${branch.getFullName()}'");

      String remoteTrackingBranchName = branch.getRemoteTrackingBranch().map(rtb -> rtb.getName()).getOrNull();

      Function<IGitCoreCommitHash, Seq<String>> getRelevantContainingBranches = commitHash -> deriveBranchesContainingGivenCommitInReflog()
          .getOrElse(commitHash, List.empty())
          .reject(branchName -> branchName.equals(branch.getName()) || branchName.equals(remoteTrackingBranchName));

      IGitCoreCommit forkPoint = gitCoreRepository.findFirstAncestor(branch.derivePointedCommit(),
          commitHash -> getRelevantContainingBranches.apply(commitHash).nonEmpty()).getOrNull();

      if (forkPoint != null) {
        // We now know that this list is non-empty.
        List<String> containingBranches = getRelevantContainingBranches.apply(forkPoint.getHash()).toList();
        LOG.debug(() -> "Commit ${forkPoint} found in filtered reflog(s) of ${containingBranches.mkString(\", \")}; " +
            "returning as fork point for branch '${branch.getFullName()}'");
        return GitMacheteForkPointCommit.inferred(forkPoint, containingBranches);
      } else {
        LOG.debug(() -> "Fork for branch '${branch.getFullName()}' not found ");
        return null;
      }
    }

    private List<GitMacheteNonRootBranch> deriveDownstreamBranches(
        IGitCoreLocalBranch parentCoreLocalBranch,
        List<IBranchLayoutEntry> entries) throws GitCoreException {

      var downstreamBranchTries = entries.map(entry -> Try.of(
          () -> createGitMacheteNonRootBranch(parentCoreLocalBranch, entry)));
      var downstreamBranches = Try.sequence(downstreamBranchTries)
          .getOrElseThrow(GitCoreException::getOrWrap)
          .flatMap(list -> list);
      return downstreamBranches.toList();
    }

    private SyncToRemoteStatus deriveSyncToRemoteStatus(IGitCoreLocalBranch coreLocalBranch) throws GitCoreException {
      String localBranchName = coreLocalBranch.getName();
      LOG.debug(() -> "Entering: coreLocalBranch = '${localBranchName}'");

      if (remoteNames.isEmpty()) {
        LOG.debug("There are no remotes");
        return SyncToRemoteStatus.noRemotes();
      }

      IGitCoreRemoteBranch coreRemoteBranch = coreLocalBranch.getRemoteTrackingBranch().getOrNull();
      if (coreRemoteBranch == null) {
        LOG.debug(() -> "Branch '${localBranchName}' is untracked");
        return SyncToRemoteStatus.untracked();
      }

      GitCoreRelativeCommitCount relativeCommitCount = gitCoreRepository
          .deriveRelativeCommitCount(coreLocalBranch.derivePointedCommit(), coreRemoteBranch.derivePointedCommit())
          .getOrNull();
      if (relativeCommitCount == null) {
        LOG.debug(() -> "Relative commit count for '${localBranchName}' could not be determined");
        return SyncToRemoteStatus.untracked();
      }

      String remoteName = coreRemoteBranch.getRemoteName();
      SyncToRemoteStatus syncToRemoteStatus;

      if (relativeCommitCount.getAhead() > 0 && relativeCommitCount.getBehind() > 0) {
        Instant localBranchCommitDate = coreLocalBranch.derivePointedCommit().getCommitTime();
        Instant remoteBranchCommitDate = coreRemoteBranch.derivePointedCommit().getCommitTime();
        // In case when commit dates are equal we assume that our relation is `DivergedFromAndNewerThanRemote`
        if (remoteBranchCommitDate.compareTo(localBranchCommitDate) > 0) {
          syncToRemoteStatus = SyncToRemoteStatus.of(DivergedFromAndOlderThanRemote, remoteName);
        } else {
          if (remoteBranchCommitDate.compareTo(localBranchCommitDate) == 0) {
            LOG.debug("Commit dates of both local and remote branches are the same, so we assume " +
                "${DivergedFromAndNewerThanRemote} sync to remote status");
          }
          syncToRemoteStatus = SyncToRemoteStatus.of(DivergedFromAndNewerThanRemote, remoteName);
        }
      } else if (relativeCommitCount.getAhead() > 0) {
        syncToRemoteStatus = SyncToRemoteStatus.of(AheadOfRemote, remoteName);
      } else if (relativeCommitCount.getBehind() > 0) {
        syncToRemoteStatus = SyncToRemoteStatus.of(BehindRemote, remoteName);
      } else {
        syncToRemoteStatus = SyncToRemoteStatus.of(InSyncToRemote, remoteName);
      }

      LOG.debug(() -> "Sync to remote status for branch '${localBranchName}': ${syncToRemoteStatus.toString()}");

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
          LOG.trace(() -> "All entries with the same hash as first entry (${firstEntry.getNewCommitHash().toString()}) " +
              "will be excluded because first entry comment starts with '${createdFromPrefix}'");
        } else {
          entryToExcludeNewId = null;
        }
      } else {
        entryToExcludeNewId = null;
      }

      String rebaseComment = "rebase finished: " + branch.getFullName() + " onto "
          + Try.of(() -> branch.derivePointedCommit().getHash().getHashString()).getOrElse("");

      // It's necessary to exclude entry with the same hash as the first entry in reflog (if it still exists)
      // for cases like branch rename just after branch creation.
      Predicate<IGitCoreReflogEntry> isEntryExcluded = e -> {
        // For debug logging only
        String newIdHash = e.getNewCommitHash().getHashString();

        if (e.getNewCommitHash().equals(entryToExcludeNewId)) {
          LOG.trace(() -> "Exclude ${e} because it has the same hash as first entry");
        } else if (e.getOldCommitHash().isDefined() && e.getNewCommitHash().equals(e.getOldCommitHash().get())) {
          LOG.trace(() -> "Exclude ${e} because its old and new IDs are the same");
        } else if (e.getComment().startsWith("branch: Created from")) {
          LOG.trace(() -> "Exclude ${e} because its comment starts with 'branch: Created from'");
        } else if (e.getComment().equals("branch: Reset to " + branch.getName())) {
          LOG.trace(() -> "Exclude ${e} because its comment is 'branch: Reset to ${branch.getName()}'");
        } else if (e.getComment().equals("branch: Reset to HEAD")) {
          LOG.trace(() -> "Exclude ${e} because its comment is 'branch: Reset to HEAD'");
        } else if (e.getComment().startsWith("reset: moving to ")) {
          LOG.trace(() -> "Exclude ${e} because its comment starts with 'reset: moving to '");
        } else if (e.getComment().equals(rebaseComment)) {
          LOG.trace(() -> "Exclude ${e} because its comment is '${rebaseComment}'");
        } else {
          return false;
        }

        return true;
      };

      var result = reflogEntries.reject(isEntryExcluded);
      LOG.debug(() -> "Filtered reflog of ${branch.getFullName()}:");
      LOG.debug(() -> result.mkString(System.lineSeparator()));
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
        @Nullable GitMacheteForkPointCommit forkPoint)
        throws GitCoreException {
      var branchName = coreLocalBranch.getName();
      LOG.debug(() -> "Entering: coreLocalBranch = '${branchName}', " +
          "parentCoreLocalBranch = '${parentCoreLocalBranch.getName()}', " +
          "forkPoint = ${forkPoint})");

      IGitCoreCommit parentPointedCommit = parentCoreLocalBranch.derivePointedCommit();
      IGitCoreCommit pointedCommit = coreLocalBranch.derivePointedCommit();

      LOG.debug(() -> "parentPointedCommit = ${parentPointedCommit.getHash().getHashString()}; " +
          "pointedCommit = ${pointedCommit.getHash().getHashString()}");

      if (pointedCommit.equals(parentPointedCommit)) {
        if (hasJustBeenCreated(coreLocalBranch)) {
          LOG.debug(() -> "Branch '${branchName}' has been detected as just created, " +
              "so we assume it's in sync");
          return SyncToParentStatus.InSync;
        } else {
          LOG.debug(
              () -> "For this branch (${branchName}) its parent's commit is equal to this branch pointed commit "
                  + "and this branch hasn't been detected as just created, so we assume it's merged");
          return SyncToParentStatus.MergedToParent;
        }
      } else {
        var isParentAncestorOfChild = gitCoreRepository.isAncestor(
            /* presumedAncestor */ parentPointedCommit, /* presumedDescendant */ pointedCommit);

        if (isParentAncestorOfChild) {
          if (forkPoint == null || forkPoint.isOverridden() || forkPoint.getCoreCommit().equals(parentPointedCommit)) {
            LOG.debug(
                () -> "For this branch (${branchName}) its parent's commit is ancestor of this branch pointed commit "
                    + "and fork point is absent or overridden or equal to parent commit, so we assume that this branch is in sync");
            return SyncToParentStatus.InSync;
          } else {
            LOG.debug(
                () -> "For this branch (${branchName}) its parent's commit is ancestor of this branch pointed commit "
                    + "but fork point is not overridden and not equal to parent commit, so we assume that this branch is in sync but with fork point off");
            return SyncToParentStatus.InSyncButForkPointOff;
          }
        } else {
          var isChildAncestorOfParent = gitCoreRepository.isAncestor(
              /* presumedAncestor */ pointedCommit, /* presumedDescendant */ parentPointedCommit);

          if (isChildAncestorOfParent) {
            LOG.debug(
                () -> "For this branch (${branchName}) its parent's commit is not ancestor of this branch pointed commit "
                    + "but this branch pointed commit is ancestor of parent branch commit, so we assume that this branch is merged");
            return SyncToParentStatus.MergedToParent;
          } else {
            LOG.debug(
                () -> "For this branch (${branchName}) its parent's commit is not ancestor of this branch pointed commit "
                    + "neither this branch pointed commit is ancestor of parent branch commit, so we assume that this branch is out of sync");
            return SyncToParentStatus.OutOfSync;
          }
        }
      }
    }
  }
}
