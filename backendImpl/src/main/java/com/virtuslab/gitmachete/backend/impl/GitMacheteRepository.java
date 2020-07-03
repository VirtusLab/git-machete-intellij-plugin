package com.virtuslab.gitmachete.backend.impl;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.AheadOfRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.BehindRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedFromAndNewerThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedFromAndOlderThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.InSyncToRemote;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import java.time.Instant;
import java.util.function.Predicate;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Queue;
import io.vavr.collection.Seq;
import io.vavr.collection.Set;
import io.vavr.collection.SortedMap;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.With;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.interning.qual.UsesObjectEquals;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.IBranchLayoutEntry;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.GitCoreRelativeCommitCount;
import com.virtuslab.gitcore.api.GitCoreRepositoryState;
import com.virtuslab.gitcore.api.IGitCoreBranchSnapshot;
import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreCommitHash;
import com.virtuslab.gitcore.api.IGitCoreLocalBranchSnapshot;
import com.virtuslab.gitcore.api.IGitCoreReflogEntry;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranchSnapshot;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRemoteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRootBranch;
import com.virtuslab.gitmachete.backend.api.OngoingRepositoryOperation;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.backend.impl.hooks.PreRebaseHookExecutor;
import com.virtuslab.gitmachete.backend.impl.hooks.StatusBranchHookExecutor;

@CustomLog
@RequiredArgsConstructor
public class GitMacheteRepository implements IGitMacheteRepository {

  private final IGitCoreRepository gitCoreRepository;
  private final StatusBranchHookExecutor statusHookExecutor;
  private final PreRebaseHookExecutor preRebaseHookExecutor;

  private static final int NUMBER_OF_MOST_RECENTLY_CHECKED_OUT_BRANCHES_FOR_DISCOVER = 10;

  @Override
  public IGitMacheteRepositorySnapshot createSnapshotForLayout(IBranchLayout branchLayout) throws GitMacheteException {
    LOG.startTimer().debug("Entering");
    try {
      var aux = new CreateGitMacheteRepositoryAux(gitCoreRepository, statusHookExecutor, preRebaseHookExecutor);
      var result = aux.createGitMacheteRepository(branchLayout);
      LOG.withTimeElapsed().info("Finished");
      return result;
    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

  @Override
  public Option<String> inferUpstreamForLocalBranch(
      Set<String> eligibleBranchNames,
      String localBranchName) throws GitMacheteException {
    LOG.startTimer().debug(() -> "Entering: localBranchName = ${localBranchName}");
    try {
      var aux = new Aux(gitCoreRepository);
      var result = aux.inferUpstreamForLocalBranch(eligibleBranchNames, localBranchName);
      LOG.withTimeElapsed().info("Finished");
      return result;
    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

  @Override
  public IGitMacheteRepositorySnapshot discoverLayoutAndCreateSnapshot() throws GitMacheteException {
    LOG.startTimer().debug("Entering");

    try {
      var aux = new DiscoverGitMacheteRepositoryAux(gitCoreRepository, statusHookExecutor, preRebaseHookExecutor);
      var result = aux.discoverGitMacheteRepository(NUMBER_OF_MOST_RECENTLY_CHECKED_OUT_BRANCHES_FOR_DISCOVER);
      LOG.withTimeElapsed().info("Finished");
      return result;
    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

  private static class Aux {
    protected final IGitCoreRepository gitCoreRepository;
    protected final List<IGitCoreLocalBranchSnapshot> localBranches;
    protected final Map<String, IGitCoreLocalBranchSnapshot> localBranchByName;

    private final java.util.Map<IGitCoreBranchSnapshot, List<IGitCoreReflogEntry>> filteredReflogByBranch = new java.util.HashMap<>();
    private @MonotonicNonNull Map<IGitCoreCommitHash, Seq<String>> branchesContainingGivenCommitInReflog;

    Aux(IGitCoreRepository gitCoreRepository) throws GitCoreException {
      this.gitCoreRepository = gitCoreRepository;
      this.localBranches = gitCoreRepository.deriveAllLocalBranches();
      this.localBranchByName = localBranches.toMap(localBranch -> Tuple.of(localBranch.getName(), localBranch));
    }

    protected Map<IGitCoreCommitHash, Seq<String>> deriveBranchesContainingGivenCommitInReflog() {
      if (branchesContainingGivenCommitInReflog != null) {
        return branchesContainingGivenCommitInReflog;
      }

      LOG.debug("Getting reflogs of local branches");

      Map<String, List<IGitCoreReflogEntry>> filteredReflogByLocalBranchName = localBranches
          .toMap(
              /* keyMapper */ localBranch -> localBranch.getName(),
              /* valueMapper */ this::deriveFilteredReflog);

      LOG.debug("Getting reflogs of remote branches");

      List<IGitCoreRemoteBranchSnapshot> remoteTrackingBranches = localBranches
          .flatMap(localBranch -> localBranch.getRemoteTrackingBranch().toList());

      Map<String, List<IGitCoreReflogEntry>> filteredReflogByRemoteTrackingBranchName = remoteTrackingBranches
          .toMap(
              /* keyMapper */ remoteBranch -> remoteBranch.getName(),
              /* valueMapper */ this::deriveFilteredReflog);

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

      LOG.debug("Derived the map of branches containing given commit in reflog:");
      LOG.debug(() -> result.toList().map(kv -> kv._1 + " -> " + kv._2.mkString(", "))
          .sorted().mkString(System.lineSeparator()));
      branchesContainingGivenCommitInReflog = result;
      return result;
    }

    /**
     * @return reflog entries, excluding branch creation and branch reset events irrelevant for fork point/upstream inference,
     * ordered from the latest to the oldest
     */
    protected List<IGitCoreReflogEntry> deriveFilteredReflog(IGitCoreBranchSnapshot branch) {
      if (filteredReflogByBranch.containsKey(branch)) {
        return filteredReflogByBranch.get(branch);
      }

      LOG.trace(() -> "Entering: branch = '${branch.getFullName()}'; original list of entries:");

      List<IGitCoreReflogEntry> reflogEntries = branch.getReflogFromMostRecent();
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
          + Try.of(() -> branch.getPointedCommit().getHash().getHashString()).getOrElse("");

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

    Option<String> inferUpstreamForLocalBranch(
        Set<String> eligibleBranchNames,
        String localBranchName) throws GitCoreException {

      var branch = localBranchByName.get(localBranchName).getOrNull();
      if (branch == null) {
        return Option.none();
      }

      String remoteTrackingBranchName = branch.getRemoteTrackingBranch().map(rtb -> rtb.getName()).getOrNull();

      var commitAndContainingBranches = gitCoreRepository
          .ancestorsOf(branch.getPointedCommit())
          .map(commit -> {
            var containingManagedBranches = deriveBranchesContainingGivenCommitInReflog()
                .getOrElse(commit.getHash(), List.empty())
                .filter(candidateBranchName -> !candidateBranchName.equals(branch.getName())
                    && !candidateBranchName.equals(remoteTrackingBranchName)
                    && eligibleBranchNames.contains(candidateBranchName));
            return Tuple.of(commit, containingManagedBranches);
          })
          .find(ccbs -> ccbs._2.nonEmpty())
          .getOrNull();

      if (commitAndContainingBranches != null) {
        var commit = commitAndContainingBranches._1;
        var containingBranches = commitAndContainingBranches._2.toList();
        assert containingBranches.nonEmpty() : "containingBranches is empty";

        String firstContainingBranchName = containingBranches.head();
        LOG.debug(() -> "Commit ${commit} found in filtered reflog(s) " +
            "of managed branch(es) ${containingBranches.mkString(\", \")}; " +
            "returning ${firstContainingBranchName} as the inferred upstream for branch '${localBranchName}'");
        return Option.some(firstContainingBranchName);
      } else {
        LOG.debug(() -> "Could not infer upstream for branch '${branch.getFullName()}'");
        return Option.none();
      }
    }
  }

  private static class CreateGitMacheteRepositoryAux extends Aux {

    private final StatusBranchHookExecutor statusHookExecutor;
    private final PreRebaseHookExecutor preRebaseHookExecutor;
    private final List<String> remoteNames;

    CreateGitMacheteRepositoryAux(
        IGitCoreRepository gitCoreRepository,
        StatusBranchHookExecutor statusHookExecutor,
        PreRebaseHookExecutor preRebaseHookExecutor) throws GitCoreException {
      super(gitCoreRepository);

      this.statusHookExecutor = statusHookExecutor;
      this.preRebaseHookExecutor = preRebaseHookExecutor;
      this.remoteNames = gitCoreRepository.deriveAllRemoteNames();
    }

    IGitMacheteRepositorySnapshot createGitMacheteRepository(IBranchLayout branchLayout) throws GitMacheteException {
      var rootBranchTries = branchLayout.getRootEntries().map(entry -> Try.of(() -> createGitMacheteRootBranch(entry)));
      var rootBranchCreationResults = Try.sequence(rootBranchTries).getOrElseThrow(GitMacheteException::getOrWrap).toList();
      var rootBranches = rootBranchCreationResults.map(creationResult -> creationResult.getCreatedRootBranch());
      var skippedBranchNames = rootBranchCreationResults.flatMap(creationResult -> creationResult.getNotCreatedBranchNames());

      var managedBranchByName = createManagedBranchByNameMap(rootBranches);

      Option<IGitCoreLocalBranchSnapshot> coreCurrentBranch = Try.of(() -> gitCoreRepository.deriveHead().getTargetBranch())
          .getOrElseThrow(e -> new GitMacheteException("Can't get current branch", e));
      LOG.debug(() -> "Current branch: " + (coreCurrentBranch.isDefined()
          ? coreCurrentBranch.get().getName()
          : "<none> (detached HEAD)"));

      IGitMacheteBranch currentBranchIfManaged = coreCurrentBranch
          .flatMap(cb -> managedBranchByName.get(cb.getName()))
          .getOrNull();
      LOG.debug(() -> "Current Git Machete branch (if managed): " + (currentBranchIfManaged != null
          ? currentBranchIfManaged.getName()
          : "<none> (unmanaged branch or detached HEAD)"));

      var ongoingOperation = Match(gitCoreRepository.deriveRepositoryState()).of(
          Case($(GitCoreRepositoryState.CHERRY_PICK), OngoingRepositoryOperation.CHERRY_PICKING),
          Case($(GitCoreRepositoryState.MERGING), OngoingRepositoryOperation.MERGING),
          Case($(GitCoreRepositoryState.REBASING), OngoingRepositoryOperation.REBASING),
          Case($(GitCoreRepositoryState.REVERTING), OngoingRepositoryOperation.REVERTING),
          Case($(GitCoreRepositoryState.APPLYING), OngoingRepositoryOperation.APPLYING),
          Case($(GitCoreRepositoryState.BISECTING), OngoingRepositoryOperation.BISECTING),
          Case($(), OngoingRepositoryOperation.NO_OPERATION));

      return new GitMacheteRepositorySnapshot(rootBranches, branchLayout, currentBranchIfManaged, managedBranchByName,
          skippedBranchNames, preRebaseHookExecutor, ongoingOperation);
    }

    private Map<String, IGitMacheteBranch> createManagedBranchByNameMap(List<IGitMacheteRootBranch> rootBranches) {
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

    private RootCreatedBranchAndSkippedBranches createGitMacheteRootBranch(
        IBranchLayoutEntry entry) throws GitCoreException, GitMacheteException {

      var branchName = entry.getName();
      IGitCoreLocalBranchSnapshot coreLocalBranch = localBranchByName.get(branchName)
          .getOrElseThrow(() -> new GitMacheteException("Branch '${branchName}' not found in the repository"));

      IGitCoreCommit corePointedCommit = coreLocalBranch.getPointedCommit();

      var pointedCommit = new GitMacheteCommit(corePointedCommit);
      var syncToRemoteStatus = deriveSyncToRemoteStatus(coreLocalBranch);
      var customAnnotation = entry.getCustomAnnotation().getOrNull();
      var downstreamBranches = deriveDownstreamBranches(coreLocalBranch, entry.getChildren());
      var remoteTrackingBranch = getRemoteTrackingBranchForCoreLocalBranch(coreLocalBranch);
      var statusHookOutput = statusHookExecutor.deriveHookOutputFor(branchName, pointedCommit).getOrNull();

      GitMacheteRootBranch createdRootBranch = new GitMacheteRootBranch(branchName, downstreamBranches.getCreatedBranches(),
          pointedCommit, remoteTrackingBranch, syncToRemoteStatus, customAnnotation, statusHookOutput);
      return RootCreatedBranchAndSkippedBranches.of(createdRootBranch, downstreamBranches.getSkippedBranchNames());
    }

    private NonRootCreatedAndSkippedBranches createGitMacheteNonRootBranch(
        IGitCoreLocalBranchSnapshot parentCoreLocalBranch,
        IBranchLayoutEntry entry) throws GitCoreException {

      var branchName = entry.getName();

      IGitCoreLocalBranchSnapshot coreLocalBranch = localBranchByName.get(branchName).getOrNull();
      if (coreLocalBranch == null) {
        NonRootCreatedAndSkippedBranches downstreamResult = deriveDownstreamBranches(parentCoreLocalBranch,
            entry.getChildren());
        return downstreamResult.withExtraSkippedBranch(branchName);
      }

      IGitCoreCommit corePointedCommit = coreLocalBranch.getPointedCommit();

      GitMacheteForkPointCommit forkPoint = deriveParentAwareForkPoint(coreLocalBranch, parentCoreLocalBranch);

      var syncToParentStatus = deriveSyncToParentStatus(coreLocalBranch, parentCoreLocalBranch, forkPoint);

      List<IGitCoreCommit> commits;
      if (forkPoint == null) {
        // That's a rare case in practice, mostly happens due to reflog expiry.
        commits = List.empty();
      } else if (syncToParentStatus == SyncToParentStatus.InSyncButForkPointOff) {
        // In case of yellow edge, we include the entire range from the commit pointed by the branch until its parent,
        // and not until just its fork point. This makes it possible to highlight the fork point candidate on the commit listing.
        commits = gitCoreRepository.deriveCommitRange(corePointedCommit, parentCoreLocalBranch.getPointedCommit());
      } else {
        // We're handling the cases of green, gray and red edges here.
        commits = gitCoreRepository.deriveCommitRange(corePointedCommit, forkPoint.getCoreCommit());
      }

      var pointedCommit = new GitMacheteCommit(corePointedCommit);
      var syncToRemoteStatus = deriveSyncToRemoteStatus(coreLocalBranch);
      var customAnnotation = entry.getCustomAnnotation().getOrNull();
      var downstreamBranches = deriveDownstreamBranches(coreLocalBranch, entry.getChildren());
      var remoteTrackingBranch = getRemoteTrackingBranchForCoreLocalBranch(coreLocalBranch);
      var statusHookOutput = statusHookExecutor.deriveHookOutputFor(branchName, pointedCommit).getOrNull();

      var result = new GitMacheteNonRootBranch(branchName, downstreamBranches.getCreatedBranches(), pointedCommit,
          remoteTrackingBranch, syncToRemoteStatus, customAnnotation, statusHookOutput, forkPoint,
          commits.map(GitMacheteCommit::new), syncToParentStatus);
      return NonRootCreatedAndSkippedBranches.of(result, downstreamBranches.getSkippedBranchNames());
    }

    private @Nullable IGitMacheteRemoteBranch getRemoteTrackingBranchForCoreLocalBranch(
        IGitCoreLocalBranchSnapshot coreLocalBranch) {

      IGitCoreRemoteBranchSnapshot coreRemoteBranch = coreLocalBranch.getRemoteTrackingBranch().getOrNull();
      if (coreRemoteBranch == null) {
        return null;
      }
      return new GitMacheteRemoteBranch(new GitMacheteCommit(coreRemoteBranch.getPointedCommit()));
    }

    private @Nullable GitMacheteForkPointCommit deriveParentAwareForkPoint(
        IGitCoreLocalBranchSnapshot coreLocalBranch,
        IGitCoreLocalBranchSnapshot parentCoreLocalBranch) throws GitCoreException {
      LOG.startTimer().debug(() -> "Entering: coreLocalBranch = '${coreLocalBranch.getName()}', " +
          "parentCoreLocalBranch = '${parentCoreLocalBranch.getName()}'");

      IGitCoreCommit overriddenForkPointCommit = deriveParentAgnosticOverriddenForkPoint(coreLocalBranch);
      GitMacheteForkPointCommit parentAgnosticForkPoint = overriddenForkPointCommit != null
          ? GitMacheteForkPointCommit.overridden(overriddenForkPointCommit)
          : deriveParentAgnosticInferredForkPoint(coreLocalBranch);

      var parentAgnosticForkPointString = parentAgnosticForkPoint != null ? parentAgnosticForkPoint.toString() : "empty";
      var parentPointedCommit = parentCoreLocalBranch.getPointedCommit();
      var pointedCommit = coreLocalBranch.getPointedCommit();

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

    private @Nullable IGitCoreCommit deriveParentAgnosticOverriddenForkPoint(IGitCoreLocalBranchSnapshot coreLocalBranch)
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
      var branchCommit = coreLocalBranch.getPointedCommit();
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

    private @Nullable GitMacheteForkPointCommit deriveParentAgnosticInferredForkPoint(IGitCoreLocalBranchSnapshot branch)
        throws GitCoreException {
      LOG.debug(() -> "Entering: branch = '${branch.getFullName()}'");

      String remoteTrackingBranchName = branch.getRemoteTrackingBranch().map(rtb -> rtb.getName()).getOrNull();

      var forkPointAndContainingBranches = gitCoreRepository
          .ancestorsOf(branch.getPointedCommit())
          .map(commit -> {
            var containingBranches = deriveBranchesContainingGivenCommitInReflog()
                .getOrElse(commit.getHash(), List.empty())
                .reject(branchName -> branchName.equals(branch.getName()) || branchName.equals(remoteTrackingBranchName));
            return Tuple.of(commit, containingBranches);
          })
          .find(commitAndContainingBranches -> commitAndContainingBranches._2.nonEmpty())
          .getOrNull();

      if (forkPointAndContainingBranches != null) {
        var forkPoint = forkPointAndContainingBranches._1;
        var containingBranches = forkPointAndContainingBranches._2.toList();
        LOG.debug(() -> "Commit ${forkPoint} found in filtered reflog(s) of ${containingBranches.mkString(\", \")}; " +
            "returning as fork point for branch '${branch.getFullName()}'");
        return GitMacheteForkPointCommit.inferred(forkPoint, containingBranches);
      } else {
        LOG.debug(() -> "Fork for branch '${branch.getFullName()}' not found ");
        return null;
      }
    }

    private NonRootCreatedAndSkippedBranches deriveDownstreamBranches(
        IGitCoreLocalBranchSnapshot parentCoreLocalBranch,
        List<IBranchLayoutEntry> entries) throws GitCoreException {

      var downstreamBranchTries = entries.map(entry -> Try.of(
          () -> createGitMacheteNonRootBranch(parentCoreLocalBranch, entry)));
      return Try.sequence(downstreamBranchTries)
          .getOrElseThrow(GitCoreException::getOrWrap)
          .fold(NonRootCreatedAndSkippedBranches.empty(), NonRootCreatedAndSkippedBranches::merge);
    }

    private SyncToRemoteStatus deriveSyncToRemoteStatus(IGitCoreLocalBranchSnapshot coreLocalBranch) throws GitCoreException {
      String localBranchName = coreLocalBranch.getName();
      LOG.debug(() -> "Entering: coreLocalBranch = '${localBranchName}'");

      if (remoteNames.isEmpty()) {
        LOG.debug("There are no remotes");
        return SyncToRemoteStatus.noRemotes();
      }

      IGitCoreRemoteBranchSnapshot coreRemoteBranch = coreLocalBranch.getRemoteTrackingBranch().getOrNull();
      if (coreRemoteBranch == null) {
        LOG.debug(() -> "Branch '${localBranchName}' is untracked");
        return SyncToRemoteStatus.untracked();
      }

      GitCoreRelativeCommitCount relativeCommitCount = gitCoreRepository
          .deriveRelativeCommitCount(coreLocalBranch.getPointedCommit(), coreRemoteBranch.getPointedCommit())
          .getOrNull();
      if (relativeCommitCount == null) {
        LOG.debug(() -> "Relative commit count for '${localBranchName}' could not be determined");
        return SyncToRemoteStatus.untracked();
      }

      String remoteName = coreRemoteBranch.getRemoteName();
      SyncToRemoteStatus syncToRemoteStatus;

      if (relativeCommitCount.getAhead() > 0 && relativeCommitCount.getBehind() > 0) {
        Instant localBranchCommitDate = coreLocalBranch.getPointedCommit().getCommitTime();
        Instant remoteBranchCommitDate = coreRemoteBranch.getPointedCommit().getCommitTime();
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

    private boolean hasJustBeenCreated(IGitCoreLocalBranchSnapshot branch) {
      List<IGitCoreReflogEntry> reflog = deriveFilteredReflog(branch);
      return reflog.isEmpty() || reflog.head().getOldCommitHash().isEmpty();
    }

    protected SyncToParentStatus deriveSyncToParentStatus(
        IGitCoreLocalBranchSnapshot coreLocalBranch,
        IGitCoreLocalBranchSnapshot parentCoreLocalBranch,
        @Nullable GitMacheteForkPointCommit forkPoint) throws GitCoreException {

      var branchName = coreLocalBranch.getName();
      LOG.debug(() -> "Entering: coreLocalBranch = '${branchName}', " +
          "parentCoreLocalBranch = '${parentCoreLocalBranch.getName()}', " +
          "forkPoint = ${forkPoint})");

      IGitCoreCommit parentPointedCommit = parentCoreLocalBranch.getPointedCommit();
      IGitCoreCommit pointedCommit = coreLocalBranch.getPointedCommit();

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

  private static class DiscoverGitMacheteRepositoryAux extends CreateGitMacheteRepositoryAux {

    DiscoverGitMacheteRepositoryAux(
        IGitCoreRepository gitCoreRepository,
        StatusBranchHookExecutor statusHookExecutor,
        PreRebaseHookExecutor preRebaseHookExecutor) throws GitCoreException {
      super(gitCoreRepository, statusHookExecutor, preRebaseHookExecutor);
    }

    @AllArgsConstructor // needed for @With
    @SuppressWarnings("interning:not.interned") // to allow for `==` comparison in Lombok-generated `withChildren` method
    @ToString(callSuper = false)
    @UsesObjectEquals
    private static class MyBranchLayoutEntry implements IBranchLayoutEntry {
      @Getter
      private final String name;

      @Getter
      @With
      private List<IBranchLayoutEntry> children;

      @Getter
      private @Nullable MyBranchLayoutEntry parent;

      private @NotOnlyInitialized MyBranchLayoutEntry root;

      MyBranchLayoutEntry(String name) {
        this.name = name;
        this.children = List.empty();
        this.parent = null;
        this.root = this;
      }

      void attachUnder(MyBranchLayoutEntry newParent) {
        parent = newParent;
        root = newParent.root;
      }

      void appendChild(MyBranchLayoutEntry newChild) {
        children = children.append(newChild);
      }

      void removeChild(MyBranchLayoutEntry child) {
        children = children.remove(child);
      }

      MyBranchLayoutEntry getRoot() {
        // Path compression to reduce the lookup time,
        // see https://en.wikipedia.org/wiki/Disjoint-set_data_structure#Find
        if (root != this && root.root != root) {
          root = root.getRoot();
        }
        return root;
      }

      @ToString.Include(name = "children") // avoid recursive `toString` calls on children
      private List<String> getChildNames() {
        return children.map(e -> e.getName());
      }

      @ToString.Include(name = "parent") // avoid recursive `toString` call on parent
      private @Nullable String getParentName() {
        return parent != null ? parent.name : null;
      }

      @ToString.Include(name = "root") // avoid recursive `toString` call on root
      private String getRootName() {
        return root.name;
      }

      @Override
      public Option<String> getCustomAnnotation() {
        return Option.none();
      }
    }

    private Map<String, Instant> deriveLastCheckoutTimestampByBranchName() throws GitCoreException {
      java.util.Map<String, Instant> result = new java.util.HashMap<>();

      for (var reflogEntry : gitCoreRepository.deriveHead().getReflogFromMostRecent()) {
        var checkoutEntry = reflogEntry.parseCheckout().getOrNull();
        if (checkoutEntry != null) {
          var timestamp = reflogEntry.getTimestamp();
          // `putIfAbsent` since we only care about the most recent occurrence of the given branch being checked out,
          // and we iterate over the reflog starting from the latest entries.
          result.putIfAbsent(checkoutEntry.getFromBranchName(), timestamp);
          result.putIfAbsent(checkoutEntry.getToBranchName(), timestamp);
        }
      }
      return HashMap.ofAll(result);
    }

    IGitMacheteRepositorySnapshot discoverGitMacheteRepository(int mostRecentlyCheckedOutBranchesCount)
        throws GitCoreException, GitMacheteException {

      List<String> localBranchNames = localBranches.map(lb -> lb.getName());
      var branchNamesFixedRootPartition = localBranchNames.partition(Predicate.isEqual("master"));
      List<String> fixedRootBranchNames = branchNamesFixedRootPartition._1;
      List<String> nonFixedRootBranchNames = branchNamesFixedRootPartition._2;
      List<String> freshNonFixedRootBranchNames;

      // Let's only leave at most the given number of most recently checked out ("fresh") branches.
      if (nonFixedRootBranchNames.size() <= mostRecentlyCheckedOutBranchesCount) {
        freshNonFixedRootBranchNames = nonFixedRootBranchNames;
      } else {
        Map<String, Instant> lastCheckoutTimestampByBranchName = deriveLastCheckoutTimestampByBranchName();

        var freshAndStaleNonFixedRootBranchNames = nonFixedRootBranchNames
            .sortBy(branchName -> lastCheckoutTimestampByBranchName.getOrElse(branchName, Instant.MIN))
            .reverse()
            .splitAt(mostRecentlyCheckedOutBranchesCount);
        freshNonFixedRootBranchNames = freshAndStaleNonFixedRootBranchNames._1;

        LOG.debug(() -> "Skipping stale branches from the discovered layout: "
            + freshAndStaleNonFixedRootBranchNames._2.mkString(", "));
      }

      // Let's use SortedMaps to ensure a deterministic result.
      SortedMap<String, MyBranchLayoutEntry> entryByFixedRootBranchNames = fixedRootBranchNames
          .toSortedMap(name -> Tuple.of(name, new MyBranchLayoutEntry(name)));
      SortedMap<String, MyBranchLayoutEntry> entryByFreshNonFixedRootBranch = freshNonFixedRootBranchNames
          .toSortedMap(name -> Tuple.of(name, new MyBranchLayoutEntry(name)));
      SortedMap<String, MyBranchLayoutEntry> entryByIncludedBranchName = entryByFixedRootBranchNames
          .merge(entryByFreshNonFixedRootBranch);
      LOG.debug(() -> "Branches included in the discovered layout: " + entryByIncludedBranchName.keySet().mkString(", "));

      // `roots` may be an empty list in the rare case there's no `master` branch in the repository.
      List<MyBranchLayoutEntry> roots = entryByFixedRootBranchNames.values().toList();

      // Skipping the upstream inference for fixed roots (currently just `master`) and for the stale non-fixed-root branches.
      for (var branchEntry : entryByFreshNonFixedRootBranch.values()) {
        // Note that stale non-fixed-root branches are never considered as candidates for an upstream.
        Seq<String> upstreamCandidateNames = entryByIncludedBranchName.values()
            .filter(e -> e.getRoot() != branchEntry)
            .map(e -> e.getName());
        LOG.debug(() -> "Upstream candidate(s) for ${branchEntry.getName()}: " + upstreamCandidateNames.mkString(", "));

        String upstreamName = inferUpstreamForLocalBranch(upstreamCandidateNames.toSet(), branchEntry.getName()).getOrNull();

        if (upstreamName != null) {
          LOG.debug(() -> "Upstream inferred for ${branchEntry.getName()} is ${upstreamName}");

          var parentEntry = entryByIncludedBranchName.get(upstreamName).getOrNull();
          // Generally we expect an entry for upstreamName to always be present.
          if (parentEntry != null) {
            branchEntry.attachUnder(parentEntry);
            parentEntry.appendChild(branchEntry);
          }
        } else {
          LOG.debug(() -> "No upstream inferred for ${branchEntry.getName()}; attaching as new root");

          roots = roots.append(branchEntry);
        }
      }

      var NL = System.lineSeparator();
      LOG.debug(() -> "Final discovered entries: " + NL + entryByIncludedBranchName.values().mkString(NL));

      // Post-process the discovered layout to remove the branches that would both:
      // 1. have no downstream AND
      // 2. be merged to their respective upstreams.
      for (var branchEntry : entryByFreshNonFixedRootBranch.values()) {
        if (branchEntry.getChildren().nonEmpty()) {
          continue;
        }

        var parentEntry = branchEntry.getParent();
        if (parentEntry == null) {
          // This will happen for the roots of the discovered layout.
          continue;
        }
        var branch = localBranchByName.get(branchEntry.getName()).getOrNull();
        var upstreamBranch = localBranchByName.get(parentEntry.getName()).getOrNull();
        if (branch == null || upstreamBranch == null) {
          // This should never happen.
          continue;
        }

        // A little hack wrt. fork point: we only want to distinct between a branch merged or not merged to the upstream,
        // and fork point does not affect this specific distinction.
        // It's in fact only useful for distinguishing between `InSync` and `InSyncButForkPointOff`,
        // but here we don't care if the former is returned instead of the latter.
        SyncToParentStatus syncStatus = deriveSyncToParentStatus(branch, upstreamBranch, /* forkPoint */ null);
        if (syncStatus == SyncToParentStatus.MergedToParent) {
          LOG.debug(() -> "Removing entry for ${branchEntry.getName()} " +
              "since it's merged to its parent ${parentEntry.getName()} and would have no downstreams");
          parentEntry.removeChild(branchEntry);
        }
      }

      return createGitMacheteRepository(new BranchLayout(List.narrow(roots)));
    }

  }

  @Getter
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  private static final class NonRootCreatedAndSkippedBranches {
    private final List<GitMacheteNonRootBranch> createdBranches;
    private final List<String> skippedBranchNames;
    private static final NonRootCreatedAndSkippedBranches EMPTY = new NonRootCreatedAndSkippedBranches(List.empty(),
        List.empty());

    static NonRootCreatedAndSkippedBranches empty() {
      return EMPTY;
    }

    NonRootCreatedAndSkippedBranches withExtraSkippedBranch(String skippedBranch) {
      return new NonRootCreatedAndSkippedBranches(getCreatedBranches(), getSkippedBranchNames().append(skippedBranch));
    }

    static NonRootCreatedAndSkippedBranches of(GitMacheteNonRootBranch createdBranch, List<String> skippedBranchNames) {
      return new NonRootCreatedAndSkippedBranches(List.of(createdBranch), skippedBranchNames);
    }

    static NonRootCreatedAndSkippedBranches merge(NonRootCreatedAndSkippedBranches prevResult1,
        NonRootCreatedAndSkippedBranches prevResult2) {
      return new NonRootCreatedAndSkippedBranches(
          prevResult1.getCreatedBranches().appendAll(prevResult2.getCreatedBranches()),
          prevResult1.getSkippedBranchNames().appendAll(prevResult2.getSkippedBranchNames()));
    }
  }

  @Data
  @RequiredArgsConstructor(staticName = "of")
  private static final class RootCreatedBranchAndSkippedBranches {
    private final IGitMacheteRootBranch createdRootBranch;
    private final List<String> notCreatedBranchNames;
  }
}
