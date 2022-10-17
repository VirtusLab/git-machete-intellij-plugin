package com.virtuslab.gitmachete.backend.impl;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.AheadOfRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.BehindRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.DivergedFromAndNewerThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.DivergedFromAndOlderThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.InSyncToRemote;
import static com.virtuslab.gitmachete.backend.impl.GitMacheteRepositorySnapshot.OngoingRepositoryOperation;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import java.time.Instant;
import java.util.function.Predicate;

import com.jcabi.aspects.Loggable;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Queue;
import io.vavr.collection.Seq;
import io.vavr.collection.Set;
import io.vavr.collection.TreeSet;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.val;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.interning.qual.UsesObjectEquals;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.BranchLayoutEntry;
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
import com.virtuslab.gitmachete.backend.api.IBranchReference;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.ILocalBranchReference;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.IRemoteTrackingBranchReference;
import com.virtuslab.gitmachete.backend.api.OngoingRepositoryOperationType;
import com.virtuslab.gitmachete.backend.api.RelationToRemote;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.impl.hooks.PreRebaseHookExecutor;
import com.virtuslab.gitmachete.backend.impl.hooks.StatusBranchHookExecutor;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
@RequiredArgsConstructor
public class GitMacheteRepository implements IGitMacheteRepository {

  private final IGitCoreRepository gitCoreRepository;
  private final StatusBranchHookExecutor statusHookExecutor;
  private final PreRebaseHookExecutor preRebaseHookExecutor;

  private static final int NUMBER_OF_MOST_RECENTLY_CHECKED_OUT_BRANCHES_FOR_DISCOVER = 10;

  @Override
  @UIThreadUnsafe
  @Loggable(value = Loggable.DEBUG, prepend = true, skipArgs = true, skipResult = true)
  public IGitMacheteRepositorySnapshot createSnapshotForLayout(BranchLayout branchLayout) throws GitMacheteException {
    try {
      val aux = new CreateGitMacheteRepositoryAux(gitCoreRepository, statusHookExecutor, preRebaseHookExecutor);
      return aux.createSnapshot(branchLayout);
    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

  @Override
  @UIThreadUnsafe
  @Loggable(value = Loggable.DEBUG, prepend = true)
  public @Nullable ILocalBranchReference inferParentForLocalBranch(
      Set<String> eligibleLocalBranchNames,
      String localBranchName) throws GitMacheteException {
    try {
      val aux = new Aux(gitCoreRepository);
      return aux.inferParentForLocalBranch(eligibleLocalBranchNames, localBranchName);
    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

  @Override
  @UIThreadUnsafe
  @Loggable(value = Loggable.DEBUG, prepend = true, skipResult = true)
  public IGitMacheteRepositorySnapshot discoverLayoutAndCreateSnapshot() throws GitMacheteException {
    try {
      val aux = new DiscoverGitMacheteRepositoryAux(gitCoreRepository, statusHookExecutor, preRebaseHookExecutor);
      return aux.discoverLayoutAndCreateSnapshot(NUMBER_OF_MOST_RECENTLY_CHECKED_OUT_BRANCHES_FOR_DISCOVER);
    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

  @CustomLog
  private static class Aux {
    protected final IGitCoreRepository gitCoreRepository;
    protected final List<IGitCoreLocalBranchSnapshot> localBranches;
    protected final Map<String, IGitCoreLocalBranchSnapshot> localBranchByName;

    private final java.util.Map<IGitCoreBranchSnapshot, List<IGitCoreReflogEntry>> filteredReflogByBranch = new java.util.HashMap<>();
    private @MonotonicNonNull Map<IGitCoreCommitHash, Seq<IBranchReference>> branchesContainingGivenCommitInReflog;

    Aux(IGitCoreRepository gitCoreRepository) throws GitCoreException {
      this.gitCoreRepository = gitCoreRepository;
      this.localBranches = gitCoreRepository.deriveAllLocalBranches();
      this.localBranchByName = localBranches.toMap(localBranch -> Tuple.of(localBranch.getName(), localBranch));
    }

    protected Map<IGitCoreCommitHash, Seq<IBranchReference>> deriveBranchesContainingGivenCommitInReflog() {
      if (branchesContainingGivenCommitInReflog != null) {
        return branchesContainingGivenCommitInReflog;
      }

      LOG.debug("Getting reflogs of local branches");

      Map<IBranchReference, List<IGitCoreReflogEntry>> filteredReflogByLocalBranch = localBranches
          .toMap(
              /* keyMapper */ LocalBranchReference::toLocalBranchReference,
              /* valueMapper */ this::deriveFilteredReflog);

      LOG.debug("Getting reflogs of remote branches");

      List<Tuple2<IGitCoreLocalBranchSnapshot, IGitCoreRemoteBranchSnapshot>> remoteTrackingBranches = localBranches
          .flatMap(localBranch -> Option.of(localBranch.getRemoteTrackingBranch())
              .map(remoteTrackingBranch -> Tuple.of(localBranch, remoteTrackingBranch)));

      Map<IBranchReference, List<IGitCoreReflogEntry>> filteredReflogByRemoteTrackingBranch = remoteTrackingBranches
          .toMap(
              /* keyMapper */ localAndRemote -> RemoteTrackingBranchReference.of(localAndRemote._2, localAndRemote._1),
              /* valueMapper */ localAndRemote -> deriveFilteredReflog(localAndRemote._2));

      LOG.debug("Converting reflogs to mapping of branches containing in reflog by commit");

      Map<IBranchReference, List<IGitCoreReflogEntry>> filteredReflogsByBranch = filteredReflogByLocalBranch
          .merge(filteredReflogByRemoteTrackingBranch);

      LOG.trace(() -> "Filtered reflogs by branch name:");
      LOG.trace(() -> filteredReflogsByBranch
          .map(kv -> kv._1.getName() + " -> " + kv._2.map(e -> e.getNewCommitHash()).mkString(", "))
          .sorted().mkString(System.lineSeparator()));

      Seq<Tuple2<IGitCoreCommitHash, IBranchReference>> reflogCommitHashAndBranchPairs = filteredReflogsByBranch
          .flatMap(branchAndReflog -> branchAndReflog._2
              .map(re -> Tuple.of(re.getNewCommitHash(), branchAndReflog._1)));

      val result = reflogCommitHashAndBranchPairs
          .groupBy(reflogCommitHashAndBranch -> reflogCommitHashAndBranch._1)
          .mapValues(pairsOfReflogCommitHashAndBranch -> pairsOfReflogCommitHashAndBranch
              .map(reflogCommitHashAndBranch -> reflogCommitHashAndBranch._2));

      LOG.debug("Derived the map of branches containing given commit in reflog:");
      LOG.debug(() -> result.toList().map(kv -> kv._1 + " -> " + kv._2.mkString(", "))
          .sorted().mkString(System.lineSeparator()));
      branchesContainingGivenCommitInReflog = result;
      return result;
    }

    /**
     * @return reflog entries, excluding branch creation and branch reset events irrelevant for fork point/parent inference,
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
        val firstEntry = reflogEntries.get(reflogEntries.size() - 1);
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

      String noOpRebaseCommentSuffix = branch.getFullName() + " onto " + branch.getPointedCommit().getHash().getHashString();

      // It's necessary to exclude entry with the same hash as the first entry in reflog (if it still exists)
      // for cases like branch rename just after branch creation.
      Predicate<IGitCoreReflogEntry> isEntryExcluded = e -> {
        String comment = e.getComment();
        if (e.getNewCommitHash().equals(entryToExcludeNewId)) {
          LOG.trace(() -> "Exclude ${e} because it has the same hash as first entry");
        } else if (e.getOldCommitHash() != null && e.getNewCommitHash().equals(e.getOldCommitHash())) {
          LOG.trace(() -> "Exclude ${e} because its old and new IDs are the same");
        } else if (comment.startsWith("branch: Created from")) {
          LOG.trace(() -> "Exclude ${e} because its comment starts with 'branch: Created from'");
        } else if (comment.equals("branch: Reset to " + branch.getName())) {
          LOG.trace(() -> "Exclude ${e} because its comment is '${comment}'");
        } else if (comment.equals("branch: Reset to HEAD")) {
          LOG.trace(() -> "Exclude ${e} because its comment is '${comment}'");
        } else if (comment.startsWith("reset: moving to ")) {
          LOG.trace(() -> "Exclude ${e} because its comment starts with 'reset: moving to '");
        } else if (comment.startsWith("fetch . ")) {
          LOG.trace(() -> "Exclude ${e} because its comment starts with 'fetch . '");
        } else if (comment.equals("rebase finished: " + noOpRebaseCommentSuffix)
            || comment.equals("rebase -i (finish): " + noOpRebaseCommentSuffix)) {
          LOG.trace(() -> "Exclude ${e} because its comment is '${comment}' which indicates a no-op rebase");
        } else if (comment.equals("update by push")) {
          LOG.trace(() -> "Exclude ${e} because its comment is '${comment}'");
        } else {
          return false;
        }

        return true;
      };

      val result = reflogEntries.reject(isEntryExcluded);
      LOG.debug(() -> "Filtered reflog of ${branch.getFullName()}:");
      LOG.debug(() -> result.mkString(System.lineSeparator()));
      filteredReflogByBranch.put(branch, result);
      return result;
    }

    @UIThreadUnsafe
    @Nullable
    ILocalBranchReference inferParentForLocalBranch(
        Set<String> eligibleLocalBranchNames,
        String localBranchName) throws GitCoreException {

      val localBranch = localBranchByName.get(localBranchName).getOrNull();
      if (localBranch == null) {
        return null;
      }

      LOG.debug(() -> "Branch(es) eligible for becoming the parent of ${localBranchName}: " +
          "${eligibleLocalBranchNames.mkString(\", \")}");

      val commitAndContainingBranches = gitCoreRepository
          .ancestorsOf(localBranch.getPointedCommit())
          .map(commit -> {
            Seq<ILocalBranchReference> eligibleContainingBranches = deriveBranchesContainingGivenCommitInReflog()
                .getOrElse(commit.getHash(), List.empty())
                .map(candidateBranch -> candidateBranch.isLocal()
                    ? candidateBranch.asLocal()
                    : candidateBranch.asRemote().getTrackedLocalBranch())
                .filter(correspondingLocalBranch -> !correspondingLocalBranch.getName().equals(localBranch.getName())
                    && eligibleLocalBranchNames.contains(correspondingLocalBranch.getName()));
            return Tuple.of(commit, eligibleContainingBranches);
          })
          .find(ccbs -> ccbs._2.nonEmpty())
          .getOrNull();

      if (commitAndContainingBranches != null) {
        val commit = commitAndContainingBranches._1;
        val containingBranches = commitAndContainingBranches._2.toList();
        assert containingBranches.nonEmpty() : "containingBranches is empty";

        val firstContainingBranch = containingBranches.head();
        val containingBranchNames = containingBranches.map(IBranchReference::getName);
        LOG.debug(() -> "Commit ${commit} found in filtered reflog(s) " +
            "of managed branch(es) ${containingBranchNames.mkString(\", \")}; " +
            "returning ${firstContainingBranch.getName()} as the inferred parent for branch '${localBranchName}'");
        return firstContainingBranch;
      } else {
        LOG.debug(() -> "Could not infer parent for branch '${localBranchName}'");
        return null;
      }
    }
  }

  @CustomLog
  private static class CreateGitMacheteRepositoryAux extends Aux {

    private final StatusBranchHookExecutor statusHookExecutor;
    private final PreRebaseHookExecutor preRebaseHookExecutor;
    private final List<String> remoteNames;
    private final java.util.Set<String> createdBranches = new java.util.HashSet<>();

    CreateGitMacheteRepositoryAux(
        IGitCoreRepository gitCoreRepository,
        StatusBranchHookExecutor statusHookExecutor,
        PreRebaseHookExecutor preRebaseHookExecutor) throws GitCoreException {
      super(gitCoreRepository);

      this.statusHookExecutor = statusHookExecutor;
      this.preRebaseHookExecutor = preRebaseHookExecutor;
      this.remoteNames = gitCoreRepository.deriveAllRemoteNames();
    }

    @UIThreadUnsafe
    IGitMacheteRepositorySnapshot createSnapshot(BranchLayout branchLayout) throws GitMacheteException {
      val rootBranchTries = branchLayout.getRootEntries().map(entry -> Try.of(() -> createGitMacheteRootBranch(entry)));
      val rootBranchCreationResults = Try.sequence(rootBranchTries).getOrElseThrow(GitMacheteException::getOrWrap).toList();
      val rootBranches = rootBranchCreationResults.flatMap(creationResult -> creationResult.getCreatedBranches());
      val skippedBranchNames = rootBranchCreationResults.flatMap(creationResult -> creationResult.getSkippedBranchNames())
          .toSet();
      val duplicatedBranchNames = rootBranchCreationResults
          .flatMap(creationResult -> creationResult.getDuplicatedBranchNames()).toSet();

      val managedBranchByName = createManagedBranchByNameMap(rootBranches);

      IGitCoreLocalBranchSnapshot coreCurrentBranch = deriveCoreCurrentBranch();

      LOG.debug(() -> "Current branch: " + (coreCurrentBranch != null
          ? coreCurrentBranch.getName()
          : "<none> (detached HEAD)"));

      IManagedBranchSnapshot currentBranchIfManaged = coreCurrentBranch != null
          ? managedBranchByName.get(coreCurrentBranch.getName()).getOrNull()
          : null;
      LOG.debug(() -> "Current Git Machete branch (if managed): " + (currentBranchIfManaged != null
          ? currentBranchIfManaged.getName()
          : "<none> (unmanaged branch or detached HEAD)"));

      val ongoingOperationType = Match(gitCoreRepository.deriveRepositoryState()).of(
          Case($(GitCoreRepositoryState.CHERRY_PICK), OngoingRepositoryOperationType.CHERRY_PICKING),
          Case($(GitCoreRepositoryState.MERGING), OngoingRepositoryOperationType.MERGING),
          Case($(GitCoreRepositoryState.REBASING), OngoingRepositoryOperationType.REBASING),
          Case($(GitCoreRepositoryState.REVERTING), OngoingRepositoryOperationType.REVERTING),
          Case($(GitCoreRepositoryState.APPLYING), OngoingRepositoryOperationType.APPLYING),
          Case($(GitCoreRepositoryState.BISECTING), OngoingRepositoryOperationType.BISECTING),
          Case($(), OngoingRepositoryOperationType.NO_OPERATION));

      val operationsBaseBranchName = deriveOngoingOperationsBaseBranchName(ongoingOperationType);

      return new GitMacheteRepositorySnapshot(List.narrow(rootBranches), branchLayout, currentBranchIfManaged,
          managedBranchByName, duplicatedBranchNames, skippedBranchNames, preRebaseHookExecutor,
          new OngoingRepositoryOperation(ongoingOperationType, operationsBaseBranchName));
    }

    @UIThreadUnsafe
    private @Nullable String deriveOngoingOperationsBaseBranchName(OngoingRepositoryOperationType ongoingOperation)
        throws GitMacheteException {
      try {
        if (ongoingOperation == OngoingRepositoryOperationType.REBASING) {
          return gitCoreRepository.deriveRebasedBranch();
        } else if (ongoingOperation == OngoingRepositoryOperationType.BISECTING) {
          return gitCoreRepository.deriveBisectedBranch();
        }
      } catch (GitCoreException e) {
        throw new GitMacheteException("Error occurred while getting the base branch of ongoing repository operation", e);
      }

      return null;
    }

    @UIThreadUnsafe
    private @Nullable IGitCoreLocalBranchSnapshot deriveCoreCurrentBranch() throws GitMacheteException {
      try {
        return gitCoreRepository.deriveHead().getTargetBranch();
      } catch (GitCoreException e) {
        throw new GitMacheteException("Can't get current branch", e);
      }
    }

    private Map<String, IManagedBranchSnapshot> createManagedBranchByNameMap(List<RootManagedBranchSnapshot> rootBranches) {
      Map<String, IManagedBranchSnapshot> branchByName = HashMap.empty();
      Queue<IManagedBranchSnapshot> queue = Queue.ofAll(rootBranches);
      // BFS over all branches
      while (queue.nonEmpty()) {
        val headAndTail = queue.dequeue();
        val branch = headAndTail._1;
        branchByName = branchByName.put(branch.getName(), branch);
        queue = headAndTail._2.appendAll(branch.getChildren());
      }
      return branchByName;
    }

    @UIThreadUnsafe
    private CreatedAndDuplicatedAndSkippedBranches<RootManagedBranchSnapshot> createGitMacheteRootBranch(
        BranchLayoutEntry entry) throws GitCoreException {

      val branchName = entry.getName();
      IGitCoreLocalBranchSnapshot coreLocalBranch = localBranchByName.get(branchName).getOrNull();
      if (coreLocalBranch == null) {
        val childBranchTries = entry.getChildren()
            .map(childEntry -> Try.of(() -> createGitMacheteRootBranch(childEntry)));
        val newRoots = Try.sequence(childBranchTries)
            .getOrElseThrow(GitCoreException::getOrWrap)
            .fold(CreatedAndDuplicatedAndSkippedBranches.empty(), CreatedAndDuplicatedAndSkippedBranches::merge);
        return newRoots.withExtraSkippedBranch(branchName);
      }

      if (!createdBranches.add(branchName)) {
        val childBranchTries = entry.getChildren()
            .map(childEntry -> Try.of(() -> createGitMacheteRootBranch(childEntry)));
        val newRoots = Try.sequence(childBranchTries)
            .getOrElseThrow(GitCoreException::getOrWrap)
            .fold(CreatedAndDuplicatedAndSkippedBranches.empty(), CreatedAndDuplicatedAndSkippedBranches::merge);
        return newRoots.withExtraDuplicatedBranch(branchName);
      }

      val branchFullName = coreLocalBranch.getFullName();

      IGitCoreCommit corePointedCommit = coreLocalBranch.getPointedCommit();

      val pointedCommit = new CommitOfManagedBranch(corePointedCommit);
      val relationToRemote = deriveRelationToRemote(coreLocalBranch);
      val customAnnotation = entry.getCustomAnnotation();
      val childBranches = deriveChildBranches(coreLocalBranch, entry.getChildren());
      val remoteTrackingBranch = getRemoteTrackingBranchForCoreLocalBranch(coreLocalBranch);
      val statusHookOutput = statusHookExecutor.deriveHookOutputFor(branchName, pointedCommit);

      val createdRootBranch = new RootManagedBranchSnapshot(branchName, branchFullName,
          childBranches.getCreatedBranches(), pointedCommit, remoteTrackingBranch, relationToRemote, customAnnotation,
          statusHookOutput);
      return CreatedAndDuplicatedAndSkippedBranches.of(List.of(createdRootBranch),
          childBranches.getDuplicatedBranchNames(), childBranches.getSkippedBranchNames());
    }

    @UIThreadUnsafe
    private CreatedAndDuplicatedAndSkippedBranches<NonRootManagedBranchSnapshot> createGitMacheteNonRootBranch(
        IGitCoreLocalBranchSnapshot parentCoreLocalBranch,
        BranchLayoutEntry entry) throws GitCoreException {

      val branchName = entry.getName();
      IGitCoreLocalBranchSnapshot coreLocalBranch = localBranchByName.get(branchName).getOrNull();
      if (coreLocalBranch == null) {
        CreatedAndDuplicatedAndSkippedBranches<NonRootManagedBranchSnapshot> childResult = deriveChildBranches(
            parentCoreLocalBranch,
            entry.getChildren());
        return childResult.withExtraSkippedBranch(branchName);
      }

      if (!createdBranches.add(branchName)) {
        CreatedAndDuplicatedAndSkippedBranches<NonRootManagedBranchSnapshot> childResult = deriveChildBranches(
            parentCoreLocalBranch,
            entry.getChildren());
        return childResult.withExtraDuplicatedBranch(branchName);
      }

      val branchFullName = coreLocalBranch.getFullName();

      IGitCoreCommit corePointedCommit = coreLocalBranch.getPointedCommit();

      ForkPointCommitOfManagedBranch forkPoint = deriveParentAwareForkPoint(coreLocalBranch, parentCoreLocalBranch);

      val syncToParentStatus = deriveSyncToParentStatus(coreLocalBranch, parentCoreLocalBranch, forkPoint);

      List<IGitCoreCommit> uniqueCommits;
      if (forkPoint == null) {
        // That's a rare case in practice, mostly happens due to reflog expiry.
        uniqueCommits = List.empty();
      } else if (syncToParentStatus == SyncToParentStatus.MergedToParent) {
        uniqueCommits = List.empty();
      } else if (syncToParentStatus == SyncToParentStatus.InSyncButForkPointOff) {
        // In case of yellow edge, we include the entire range from the commit pointed by the branch until its parent,
        // and not until just its fork point. This makes it possible to highlight the fork point candidate on the commit listing.
        uniqueCommits = gitCoreRepository.deriveCommitRange(corePointedCommit, parentCoreLocalBranch.getPointedCommit());
      } else {
        // We're handling the cases of green and red edges here.
        uniqueCommits = gitCoreRepository.deriveCommitRange(corePointedCommit, forkPoint.getCoreCommit());
      }

      val pointedCommit = new CommitOfManagedBranch(corePointedCommit);
      val relationToRemote = deriveRelationToRemote(coreLocalBranch);
      val customAnnotation = entry.getCustomAnnotation();
      val childBranches = deriveChildBranches(coreLocalBranch, entry.getChildren());
      val remoteTrackingBranch = getRemoteTrackingBranchForCoreLocalBranch(coreLocalBranch);
      val statusHookOutput = statusHookExecutor.deriveHookOutputFor(branchName, pointedCommit);
      val commitsUntilParent = gitCoreRepository.deriveCommitRange(corePointedCommit, parentCoreLocalBranch.getPointedCommit());

      val result = new NonRootManagedBranchSnapshot(branchName, branchFullName, childBranches.getCreatedBranches(),
          pointedCommit, remoteTrackingBranch, relationToRemote, customAnnotation, statusHookOutput, forkPoint,
          uniqueCommits.map(CommitOfManagedBranch::new), commitsUntilParent.map(CommitOfManagedBranch::new),
          syncToParentStatus);
      return CreatedAndDuplicatedAndSkippedBranches.of(List.of(result),
          childBranches.getDuplicatedBranchNames(), childBranches.getSkippedBranchNames());
    }

    private @Nullable IRemoteTrackingBranchReference getRemoteTrackingBranchForCoreLocalBranch(
        IGitCoreLocalBranchSnapshot coreLocalBranch) {
      IGitCoreRemoteBranchSnapshot coreRemoteTrackingBranch = coreLocalBranch.getRemoteTrackingBranch();
      if (coreRemoteTrackingBranch == null) {
        return null;
      }
      return RemoteTrackingBranchReference.of(coreRemoteTrackingBranch, coreLocalBranch);
    }

    @UIThreadUnsafe
    private @Nullable ForkPointCommitOfManagedBranch deriveParentAwareForkPoint(
        IGitCoreLocalBranchSnapshot coreLocalBranch,
        IGitCoreLocalBranchSnapshot parentCoreLocalBranch) throws GitCoreException {
      LOG.debug(() -> "Entering: coreLocalBranch = '${coreLocalBranch.getName()}', " +
          "parentCoreLocalBranch = '${parentCoreLocalBranch.getName()}'");

      IGitCoreCommit overriddenForkPointCommit = deriveParentAgnosticOverriddenForkPoint(coreLocalBranch);
      ForkPointCommitOfManagedBranch parentAgnosticForkPoint = overriddenForkPointCommit != null
          ? ForkPointCommitOfManagedBranch.overridden(overriddenForkPointCommit)
          : deriveParentAgnosticInferredForkPoint(coreLocalBranch);

      val parentAgnosticForkPointString = parentAgnosticForkPoint != null ? parentAgnosticForkPoint.toString() : "empty";
      val parentPointedCommit = parentCoreLocalBranch.getPointedCommit();
      val pointedCommit = coreLocalBranch.getPointedCommit();

      LOG.debug(() -> "parentAgnosticForkPoint = ${parentAgnosticForkPointString}, " +
          "parentPointedCommit = ${parentPointedCommit.getHash().getHashString()}, " +
          "pointedCommit = ${pointedCommit.getHash().getHashString()}");

      val isParentAncestorOfChild = gitCoreRepository.isAncestorOrEqual(parentPointedCommit, pointedCommit);

      LOG.debug(() -> "Parent branch commit (${parentPointedCommit.getHash().getHashString()}) " +
          "is${isParentAncestorOfChild ? \"\" : \" NOT\"} ancestor of commit " +
          "${pointedCommit.getHash().getHashString()}");

      if (isParentAncestorOfChild) {
        if (parentAgnosticForkPoint != null) {
          val isParentAncestorOfForkPoint = gitCoreRepository.isAncestorOrEqual(parentPointedCommit,
              parentAgnosticForkPoint.getCoreCommit());

          if (!isParentAncestorOfForkPoint) {
            // If parent(A) is ancestor of A, and parent(A) is NOT ancestor of fork-point(A),
            // then assume fork-point(A)=parent(A)
            LOG.debug(() -> "Parent branch commit (${parentPointedCommit.getHash().getHashString()}) is ancestor of " +
                "commit (${pointedCommit.getHash().getHashString()}) but parent branch commit " +
                "is NOT ancestor of parent-agnostic fork point (${parentAgnosticForkPointString}), " +
                "so we assume that parent-aware fork point = parent branch commit");
            return ForkPointCommitOfManagedBranch.fallbackToParent(parentPointedCommit);
          }

        } else {
          // If parent(A) is ancestor of A, and fork-point(A) is missing,
          // then assume fork-point(A)=parent(A)
          LOG.debug(() -> "Parent branch commit (${parentPointedCommit.getHash().getHashString()}) is ancestor of " +
              "commit (${pointedCommit.getHash().getHashString()}) and parent-agnostic fork point is missing, " +
              "so we assume that parent-aware fork point = parent branch commit");
          return ForkPointCommitOfManagedBranch.fallbackToParent(parentPointedCommit);
        }
      }

      LOG.debug(() -> "Parent-aware fork point for branch ${coreLocalBranch.getName()} is ${parentAgnosticForkPointString}");

      return parentAgnosticForkPoint;
    }

    @UIThreadUnsafe
    private @Nullable IGitCoreCommit deriveParentAgnosticOverriddenForkPoint(IGitCoreLocalBranchSnapshot coreLocalBranch)
        throws GitCoreException {
      String section = "machete";
      String subsectionPrefix = "overrideForkPoint";
      String branchName = coreLocalBranch.getName();

      // Section spans the characters before the first dot
      // Name spans the characters after the last dot
      // Subsection is everything else
      String toRevision = gitCoreRepository
          .deriveConfigValue(section, subsectionPrefix + "." + branchName, "to");
      String whileDescendantOfRevision = gitCoreRepository
          .deriveConfigValue(section, subsectionPrefix + "." + branchName, "whileDescendantOf");
      if (toRevision == null || whileDescendantOfRevision == null) {
        return null;
      }
      LOG.debug(() -> "Fork point override config for '${branchName}': " +
          "to='${toRevision}', whileDescendantOf='${whileDescendantOfRevision}'");

      // Let's check the internal consistency of the config - we can't rule out that it's been tampered with.
      IGitCoreCommit to = gitCoreRepository.parseRevision(toRevision);
      IGitCoreCommit whileDescendantOf = gitCoreRepository.parseRevision(whileDescendantOfRevision);
      if (to == null || whileDescendantOf == null) {
        LOG.warn("Could not parse either <to> (${to}) or " +
            "<whileDescendantOf> (${whileDescendantOf}) into a valid commit, ignoring faulty fork point override");
        return null;
      }
      // Yes, that's not a bug. We're checking whether `whileDescendantOf` is a descendant of `to`, not the other way round.
      // The `descendant of` part of `whileDescendantOf` refers to the *current branch* being a descendant
      // of whatever `whileDescendantOf` points to.
      if (!gitCoreRepository.isAncestorOrEqual(to, whileDescendantOf)) {
        LOG.warn("Commit <to> (${to}) is NOT an ancestor of " +
            "<whileDescendantOf> (${whileDescendantOf}), ignoring faulty fork point override");
        return null;
      }

      // Now we know that the override config is consistent, but it still doesn't mean
      // that it actually applies to the given branch AT THIS POINT (it could e.g. have applied earlier but now no longer applies).
      val branchCommit = coreLocalBranch.getPointedCommit();
      if (!gitCoreRepository.isAncestorOrEqual(whileDescendantOf, branchCommit)) {
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

    @UIThreadUnsafe
    private @Nullable ForkPointCommitOfManagedBranch deriveParentAgnosticInferredForkPoint(IGitCoreLocalBranchSnapshot branch)
        throws GitCoreException {
      LOG.debug(() -> "Entering: branch = '${branch.getFullName()}'");

      val forkPointAndContainingBranches = gitCoreRepository
          .ancestorsOf(branch.getPointedCommit())
          .map(commit -> {
            Seq<IBranchReference> containingBranches = deriveBranchesContainingGivenCommitInReflog()
                .getOrElse(commit.getHash(), List.empty())
                .reject(candidateBranch -> {
                  ILocalBranchReference correspondingLocalBranch = candidateBranch.isLocal()
                      ? candidateBranch.asLocal()
                      : candidateBranch.asRemote().getTrackedLocalBranch();
                  return correspondingLocalBranch.getName().equals(branch.getName());
                });
            return Tuple.of(commit, containingBranches);
          })
          .find(commitAndContainingBranches -> commitAndContainingBranches._2.nonEmpty())
          .getOrNull();

      if (forkPointAndContainingBranches != null) {
        val forkPoint = forkPointAndContainingBranches._1;
        val containingBranches = forkPointAndContainingBranches._2.toList();
        LOG.debug(() -> "Commit ${forkPoint} found in filtered reflog(s) of ${containingBranches.mkString(\", \")}; " +
            "returning as fork point for branch '${branch.getFullName()}'");
        return ForkPointCommitOfManagedBranch.inferred(forkPoint, containingBranches);
      } else {
        LOG.debug(() -> "Fork for branch '${branch.getFullName()}' not found ");
        return null;
      }
    }

    @UIThreadUnsafe
    private CreatedAndDuplicatedAndSkippedBranches<NonRootManagedBranchSnapshot> deriveChildBranches(
        IGitCoreLocalBranchSnapshot parentCoreLocalBranch,
        List<BranchLayoutEntry> entries) throws GitCoreException {

      val childBranchTries = entries.map(entry -> Try.of(
          () -> createGitMacheteNonRootBranch(parentCoreLocalBranch, entry)));
      return Try.sequence(childBranchTries)
          .getOrElseThrow(GitCoreException::getOrWrap)
          .fold(CreatedAndDuplicatedAndSkippedBranches.empty(), CreatedAndDuplicatedAndSkippedBranches::merge);
    }

    @UIThreadUnsafe
    private RelationToRemote deriveRelationToRemote(IGitCoreLocalBranchSnapshot coreLocalBranch) throws GitCoreException {
      String localBranchName = coreLocalBranch.getName();
      LOG.debug(() -> "Entering: coreLocalBranch = '${localBranchName}'");

      if (remoteNames.isEmpty()) {
        LOG.debug("There are no remotes");
        return RelationToRemote.noRemotes();
      }

      IGitCoreRemoteBranchSnapshot coreRemoteBranch = coreLocalBranch.getRemoteTrackingBranch();
      if (coreRemoteBranch == null) {
        LOG.debug(() -> "Branch '${localBranchName}' is untracked");
        return RelationToRemote.untracked();
      }

      GitCoreRelativeCommitCount relativeCommitCount = gitCoreRepository
          .deriveRelativeCommitCount(coreLocalBranch.getPointedCommit(), coreRemoteBranch.getPointedCommit());
      if (relativeCommitCount == null) {
        LOG.debug(() -> "Relative commit count for '${localBranchName}' could not be determined");
        return RelationToRemote.untracked();
      }

      String remoteName = coreRemoteBranch.getRemoteName();
      RelationToRemote relationToRemote;

      if (relativeCommitCount.getAhead() > 0 && relativeCommitCount.getBehind() > 0) {
        Instant localBranchCommitDate = coreLocalBranch.getPointedCommit().getCommitTime();
        Instant remoteBranchCommitDate = coreRemoteBranch.getPointedCommit().getCommitTime();
        // In case when commit dates are equal we assume that our relation is `DivergedFromAndNewerThanRemote`
        if (remoteBranchCommitDate.compareTo(localBranchCommitDate) > 0) {
          relationToRemote = RelationToRemote.of(DivergedFromAndOlderThanRemote, remoteName);
        } else {
          if (remoteBranchCommitDate.compareTo(localBranchCommitDate) == 0) {
            LOG.debug("Commit dates of both local and remote branches are the same, so we assume " +
                "${DivergedFromAndNewerThanRemote} sync to remote status");
          }
          relationToRemote = RelationToRemote.of(DivergedFromAndNewerThanRemote, remoteName);
        }
      } else if (relativeCommitCount.getAhead() > 0) {
        relationToRemote = RelationToRemote.of(AheadOfRemote, remoteName);
      } else if (relativeCommitCount.getBehind() > 0) {
        relationToRemote = RelationToRemote.of(BehindRemote, remoteName);
      } else {
        relationToRemote = RelationToRemote.of(InSyncToRemote, remoteName);
      }

      LOG.debug(() -> "Relation to remote for branch '${localBranchName}': ${relationToRemote.toString()}");

      return relationToRemote;
    }

    private boolean hasJustBeenCreated(IGitCoreLocalBranchSnapshot branch) {
      List<IGitCoreReflogEntry> reflog = deriveFilteredReflog(branch);
      return reflog.isEmpty() || reflog.head().getOldCommitHash() == null;
    }

    @UIThreadUnsafe
    private boolean isEquivalentTreeReachable(IGitCoreCommit equivalentTo, IGitCoreCommit reachableFrom)
        throws GitCoreException {
      val applicableCommits = gitCoreRepository.deriveCommitRange(/* fromInclusive */ reachableFrom,
          /* untilExclusive */ equivalentTo);
      return applicableCommits.exists(commit -> commit.getTreeHash().equals(equivalentTo.getTreeHash()));
    }

    @UIThreadUnsafe
    protected SyncToParentStatus deriveSyncToParentStatus(
        IGitCoreLocalBranchSnapshot coreLocalBranch,
        IGitCoreLocalBranchSnapshot parentCoreLocalBranch,
        @Nullable ForkPointCommitOfManagedBranch forkPoint) throws GitCoreException {
      val branchName = coreLocalBranch.getName();
      val parentBranchName = parentCoreLocalBranch.getName();
      LOG.debug(() -> "Entering: coreLocalBranch = '${branchName}', " +
          "parentCoreLocalBranch = '${parentBranchName}', " +
          "forkPoint = ${forkPoint})");

      IGitCoreCommit parentPointedCommit = parentCoreLocalBranch.getPointedCommit();
      IGitCoreCommit pointedCommit = coreLocalBranch.getPointedCommit();

      LOG.debug(() -> "parentPointedCommit = ${parentPointedCommit.getHash().getHashString()}; " +
          "pointedCommit = ${pointedCommit.getHash().getHashString()}");

      if (pointedCommit.equals(parentPointedCommit)) {
        if (hasJustBeenCreated(coreLocalBranch)) {
          LOG.debug(() -> "Branch '${branchName}' has been detected as just created, so we assume it's in sync");
          return SyncToParentStatus.InSync;
        } else {
          LOG.debug(
              () -> "For this branch (${branchName}) its parent's commit is equal to this branch pointed commit "
                  + "and this branch hasn't been detected as just created, so we assume it's merged");
          return SyncToParentStatus.MergedToParent;
        }
      } else {
        val isParentAncestorOfChild = gitCoreRepository.isAncestorOrEqual(
            /* presumedAncestor */ parentPointedCommit, /* presumedDescendant */ pointedCommit);

        if (isParentAncestorOfChild) {
          if (forkPoint == null || forkPoint.isOverridden() || forkPoint.getCoreCommit().equals(parentPointedCommit)) {
            LOG.debug(
                () -> "For this branch (${branchName}) its parent's commit is ancestor of this branch pointed commit "
                    + "and fork point is absent or overridden or equal to parent branch commit, " +
                    "so we assume that this branch is in sync");
            return SyncToParentStatus.InSync;
          } else {
            LOG.debug(
                () -> "For this branch (${branchName}) its parent's commit is ancestor of this branch pointed commit "
                    + "but fork point is not overridden and not equal to parent branch commit, " +
                    "so we assume that this branch is in sync but with fork point off");
            return SyncToParentStatus.InSyncButForkPointOff;
          }
        } else {
          val isChildAncestorOfParent = gitCoreRepository.isAncestorOrEqual(
              /* presumedAncestor */ pointedCommit, /* presumedDescendant */ parentPointedCommit);

          if (isChildAncestorOfParent) {
            if (hasJustBeenCreated(coreLocalBranch)) {
              LOG.debug(() -> "Branch '${branchName}' has been detected as just created, so we assume it's out of sync");
              return SyncToParentStatus.OutOfSync;
            } else {
              LOG.debug(
                  () -> "For this branch (${branchName}) its parent's commit is not ancestor of this branch pointed commit "
                      + "but this branch pointed commit is ancestor of parent branch commit, "
                      + "and this branch hasn't been detected as just created, so we assume it's merged");
              return SyncToParentStatus.MergedToParent;
            }
          } else {
            if (isEquivalentTreeReachable(/* equivalentTo */ pointedCommit, /* reachableFrom */ parentPointedCommit)) {
              LOG.debug(
                  () -> "Branch (${branchName}) is probably squash-merged into ${parentBranchName}");
              return SyncToParentStatus.MergedToParent;
            } else {
              LOG.debug(
                  () -> "For this branch (${branchName}) its parent's commit is not ancestor of this branch pointed commit "
                      + "neither this branch pointed commit is ancestor of parent branch commit, "
                      + "so we assume that this branch is out of sync");
              return SyncToParentStatus.OutOfSync;
            }
          }
        }
      }
    }
  }

  @CustomLog
  private static class DiscoverGitMacheteRepositoryAux extends CreateGitMacheteRepositoryAux {

    private static final String MASTER = "master";
    private static final String MAIN = "main"; // see https://github.com/github/renaming
    private static final String DEVELOP = "develop";

    DiscoverGitMacheteRepositoryAux(
        IGitCoreRepository gitCoreRepository,
        StatusBranchHookExecutor statusHookExecutor,
        PreRebaseHookExecutor preRebaseHookExecutor) throws GitCoreException {
      super(gitCoreRepository, statusHookExecutor, preRebaseHookExecutor);
    }

    /**
     * A node of a mutable tree, with extra feature of near-constant time of checking up tree root thanks to path compression.
     * See <a href="https://en.wikipedia.org/wiki/Disjoint-set_data_structure#Find">Disjoint-set data structure on wikipedia</a>.
     */
    @ToString
    @UsesObjectEquals
    private static class CompressablePathTreeNode {
      @Getter
      private final String name;

      @Getter
      private List<CompressablePathTreeNode> children;

      @Getter
      private @Nullable CompressablePathTreeNode parent;

      private @NotOnlyInitialized CompressablePathTreeNode root;

      CompressablePathTreeNode(String name) {
        this.name = name;
        this.children = List.empty();
        this.parent = null;
        this.root = this;
      }

      void attachUnder(CompressablePathTreeNode newParent) {
        parent = newParent;
        root = newParent.root;
      }

      void appendChild(CompressablePathTreeNode newChild) {
        children = children.append(newChild);
      }

      void removeChild(CompressablePathTreeNode child) {
        children = children.remove(child);
      }

      CompressablePathTreeNode getRoot() {
        // The actual path compression happens here.
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

      public BranchLayoutEntry toBranchLayoutEntry() {
        return new BranchLayoutEntry(name, /* customAnnotation */ null, children.map(c -> c.toBranchLayoutEntry()));
      }
    }

    @UIThreadUnsafe
    private Map<String, Instant> deriveLastCheckoutTimestampByBranchName() throws GitCoreException {
      java.util.Map<String, Instant> result = new java.util.HashMap<>();

      for (val reflogEntry : gitCoreRepository.deriveHead().getReflogFromMostRecent()) {
        val checkoutEntry = reflogEntry.parseCheckout();
        if (checkoutEntry != null) {
          val timestamp = reflogEntry.getTimestamp();
          // `putIfAbsent` since we only care about the most recent occurrence of the given branch being checked out,
          // and we iterate over the reflog starting from the latest entries.
          result.putIfAbsent(checkoutEntry.getFromBranchName(), timestamp);
          result.putIfAbsent(checkoutEntry.getToBranchName(), timestamp);
        }
      }
      return HashMap.ofAll(result);
    }

    @UIThreadUnsafe
    IGitMacheteRepositorySnapshot discoverLayoutAndCreateSnapshot(int mostRecentlyCheckedOutBranchesCount)
        throws GitMacheteException, GitCoreException {

      List<String> localBranchNames = localBranches.map(lb -> lb.getName());
      List<String> fixedRootBranchNames = List.empty();
      List<String> nonFixedRootBranchNames = localBranchNames;
      if (localBranchNames.contains(MASTER)) {
        fixedRootBranchNames = fixedRootBranchNames.append(MASTER);
        nonFixedRootBranchNames = nonFixedRootBranchNames.remove(MASTER);
      } else if (localBranchNames.contains(MAIN)) {
        fixedRootBranchNames = fixedRootBranchNames.append(MAIN);
        nonFixedRootBranchNames = nonFixedRootBranchNames.remove(MAIN);
      }
      if (localBranchNames.contains(DEVELOP)) {
        fixedRootBranchNames = fixedRootBranchNames.append(DEVELOP);
        nonFixedRootBranchNames = nonFixedRootBranchNames.remove(DEVELOP);
      }
      List<String> freshNonFixedRootBranchNames;

      // Let's only leave at most the given number of most recently checked out ("fresh") branches.
      if (nonFixedRootBranchNames.size() <= mostRecentlyCheckedOutBranchesCount) {
        freshNonFixedRootBranchNames = nonFixedRootBranchNames;
      } else {
        Map<String, Instant> lastCheckoutTimestampByBranchName = deriveLastCheckoutTimestampByBranchName();

        val freshAndStaleNonFixedRootBranchNames = nonFixedRootBranchNames
            .sortBy(branchName -> lastCheckoutTimestampByBranchName.getOrElse(branchName, Instant.MIN))
            .reverse()
            .splitAt(mostRecentlyCheckedOutBranchesCount);
        freshNonFixedRootBranchNames = freshAndStaleNonFixedRootBranchNames._1.sorted();

        LOG.debug(() -> "Skipping stale branches from the discovered layout: "
            + freshAndStaleNonFixedRootBranchNames._2.mkString(", "));
      }

      // Let's use linked maps to ensure a deterministic result.
      Map<String, CompressablePathTreeNode> nodeByFixedRootBranchNames = fixedRootBranchNames
          .toLinkedMap(name -> Tuple.of(name, new CompressablePathTreeNode(name)));
      Map<String, CompressablePathTreeNode> nodeByFreshNonFixedRootBranch = freshNonFixedRootBranchNames
          .toLinkedMap(name -> Tuple.of(name, new CompressablePathTreeNode(name)));
      Map<String, CompressablePathTreeNode> nodeByIncludedBranchName = nodeByFixedRootBranchNames
          .merge(nodeByFreshNonFixedRootBranch);
      LOG.debug(() -> "Branches included in the discovered layout: " + nodeByIncludedBranchName.keySet().mkString(", "));

      // `roots` may be an empty list in the rare case there's no master/main/develop branch in the repository.
      List<CompressablePathTreeNode> roots = nodeByFixedRootBranchNames.values().toList();

      // Skipping the parent inference for fixed roots and for the stale non-fixed-root branches.
      for (val branchNode : nodeByFreshNonFixedRootBranch.values()) {
        // Note that stale non-fixed-root branches are never considered as candidates for the parent.
        Seq<String> parentCandidateNames = nodeByIncludedBranchName.values()
            .filter(e -> e.getRoot() != branchNode)
            .map(e -> e.getName());
        LOG.debug(() -> "Parent candidate(s) for ${branchNode.getName()}: " + parentCandidateNames.mkString(", "));

        IBranchReference parent = inferParentForLocalBranch(parentCandidateNames.toSet(), branchNode.getName());

        if (parent != null) {
          String parentName = parent.getName();
          LOG.debug(() -> "Parent inferred for ${branchNode.getName()} is ${parentName}");

          val parentNode = nodeByIncludedBranchName.get(parentName).getOrNull();
          // Generally we expect an node for parent to always be present.
          if (parentNode != null) {
            branchNode.attachUnder(parentNode);
            parentNode.appendChild(branchNode);
          }
        } else {
          LOG.debug(() -> "No parent inferred for ${branchNode.getName()}; attaching as new root");

          roots = roots.append(branchNode);
        }
      }

      val NL = System.lineSeparator();
      LOG.debug(() -> "Final discovered entries: " + NL + nodeByIncludedBranchName.values().mkString(NL));

      // Post-process the discovered layout to remove the branches that would both:
      // 1. have no child AND
      // 2. be merged to their respective parents.
      for (val branchNode : nodeByFreshNonFixedRootBranch.values()) {
        if (branchNode.getChildren().nonEmpty()) {
          continue;
        }

        val parentNode = branchNode.getParent();
        if (parentNode == null) {
          // This will happen for the roots of the discovered layout.
          continue;
        }
        val branch = localBranchByName.get(branchNode.getName()).getOrNull();
        val parentBranch = localBranchByName.get(parentNode.getName()).getOrNull();
        if (branch == null || parentBranch == null) {
          // This should never happen.
          continue;
        }

        // A little hack wrt. fork point: we only want to distinguish between a branch merged or not merged to the parent,
        // and fork point does not affect this specific distinction.
        // It's in fact only useful for distinguishing between `InSync` and `InSyncButForkPointOff`,
        // but here we don't care if the former is returned instead of the latter.
        SyncToParentStatus syncStatus = deriveSyncToParentStatus(branch, parentBranch, /* forkPoint */ null);
        if (syncStatus == SyncToParentStatus.MergedToParent) {
          LOG.debug(() -> "Removing node for ${branchNode.getName()} " +
              "since it's merged to its parent ${parentNode.getName()} and would have no children");
          parentNode.removeChild(branchNode);
        }
      }
      return createSnapshot(new BranchLayout(roots.map(r -> r.toBranchLayoutEntry())));
    }

  }

  @Getter
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  private static final class CreatedAndDuplicatedAndSkippedBranches<T extends BaseManagedBranchSnapshot> {
    private final List<T> createdBranches;
    private final Set<String> duplicatedBranchNames;
    private final Set<String> skippedBranchNames;

    CreatedAndDuplicatedAndSkippedBranches<T> withExtraDuplicatedBranch(String duplicatedBranchName) {
      return new CreatedAndDuplicatedAndSkippedBranches<T>(getCreatedBranches(),
          getDuplicatedBranchNames().add(duplicatedBranchName),
          getSkippedBranchNames());
    }

    CreatedAndDuplicatedAndSkippedBranches<T> withExtraSkippedBranch(String skippedBranchName) {
      return new CreatedAndDuplicatedAndSkippedBranches<T>(getCreatedBranches(), getDuplicatedBranchNames(),
          getSkippedBranchNames().add(skippedBranchName));
    }

    static <T extends BaseManagedBranchSnapshot> CreatedAndDuplicatedAndSkippedBranches<T> of(List<T> createdBranches,
        Set<String> duplicatedBranchName, Set<String> skippedBranchNames) {
      return new CreatedAndDuplicatedAndSkippedBranches<T>(createdBranches, duplicatedBranchName, skippedBranchNames);
    }

    static <T extends BaseManagedBranchSnapshot> CreatedAndDuplicatedAndSkippedBranches<T> empty() {
      return new CreatedAndDuplicatedAndSkippedBranches<T>(List.empty(), TreeSet.empty(), TreeSet.empty());
    }

    static <T extends BaseManagedBranchSnapshot> CreatedAndDuplicatedAndSkippedBranches<T> merge(
        CreatedAndDuplicatedAndSkippedBranches<T> prevResult1, CreatedAndDuplicatedAndSkippedBranches<T> prevResult2) {
      return new CreatedAndDuplicatedAndSkippedBranches<T>(
          prevResult1.getCreatedBranches().appendAll(prevResult2.getCreatedBranches()),
          prevResult1.getDuplicatedBranchNames().addAll(prevResult2.getDuplicatedBranchNames()),
          prevResult1.getSkippedBranchNames().addAll(prevResult2.getSkippedBranchNames()));
    }
  }
}
