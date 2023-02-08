package com.virtuslab.gitmachete.backend.impl.aux;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.AheadOfRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.BehindRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.DivergedFromAndNewerThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.DivergedFromAndOlderThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.InSyncToRemote;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import java.nio.file.Path;
import java.time.Instant;

import io.vavr.Tuple;
import io.vavr.collection.LinkedHashMap;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Try;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.BranchLayoutEntry;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.GitCoreRelativeCommitCount;
import com.virtuslab.gitcore.api.GitCoreRepositoryState;
import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreLocalBranchSnapshot;
import com.virtuslab.gitcore.api.IGitCoreReflogEntry;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranchSnapshot;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IBranchReference;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.ILocalBranchReference;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.IRemoteTrackingBranchReference;
import com.virtuslab.gitmachete.backend.api.OngoingRepositoryOperationType;
import com.virtuslab.gitmachete.backend.api.RelationToRemote;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.impl.CommitOfManagedBranch;
import com.virtuslab.gitmachete.backend.impl.CreatedAndDuplicatedAndSkippedBranches;
import com.virtuslab.gitmachete.backend.impl.ForkPointCommitOfManagedBranch;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.impl.NonRootManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.impl.RemoteTrackingBranchReference;
import com.virtuslab.gitmachete.backend.impl.RootManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.impl.hooks.PreRebaseHookExecutor;
import com.virtuslab.gitmachete.backend.impl.hooks.StatusBranchHookExecutor;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
public class CreateGitMacheteRepositoryAux extends Aux {

  private final StatusBranchHookExecutor statusHookExecutor;
  private final PreRebaseHookExecutor preRebaseHookExecutor;
  private final List<String> remoteNames;
  private final java.util.Set<String> createdBranches = new java.util.HashSet<>();
  private final Path mainGitDirectoryPath;

  public CreateGitMacheteRepositoryAux(
      IGitCoreRepository gitCoreRepository,
      StatusBranchHookExecutor statusHookExecutor,
      PreRebaseHookExecutor preRebaseHookExecutor) throws GitCoreException {
    super(gitCoreRepository);

    this.statusHookExecutor = statusHookExecutor;
    this.preRebaseHookExecutor = preRebaseHookExecutor;
    this.remoteNames = gitCoreRepository.deriveAllRemoteNames();
    this.mainGitDirectoryPath = gitCoreRepository.getMainGitDirectoryPath();
  }

  @UIThreadUnsafe
  public IGitMacheteRepositorySnapshot createSnapshot(BranchLayout branchLayout) throws GitMacheteException {
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

    return new GitMacheteRepositorySnapshot(mainGitDirectoryPath, List.narrow(rootBranches), branchLayout,
        currentBranchIfManaged, managedBranchByName, duplicatedBranchNames, skippedBranchNames, preRebaseHookExecutor,
        new IGitMacheteRepositorySnapshot.OngoingRepositoryOperation(ongoingOperationType, operationsBaseBranchName));
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

  // Using LinkedHashMap to retain the original order of branches.
  private LinkedHashMap<String, IManagedBranchSnapshot> createManagedBranchByNameMap(
      List<RootManagedBranchSnapshot> rootBranches) {
    LinkedHashMap<String, IManagedBranchSnapshot> branchByName = LinkedHashMap.empty();
    List<IManagedBranchSnapshot> stack = List.ofAll(rootBranches);
    // A non-recursive DFS over all branches
    while (stack.nonEmpty()) {
      val branch = stack.head();
      branchByName = branchByName.put(branch.getName(), branch);
      stack = stack.tail().prependAll(branch.getChildren());
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
  public @Nullable ForkPointCommitOfManagedBranch deriveParentAwareForkPoint(
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
    } else { // parent is NOT an ancestor of the child
      // Let's avoid including any commits reachable from the parent into unique range of commits for the given branch.
      if (parentAgnosticForkPoint != null) {
        val isForkPointAncestorOfParent = gitCoreRepository.isAncestorOrEqual(parentAgnosticForkPoint.getCoreCommit(),
            parentPointedCommit);
        if (isForkPointAncestorOfParent) {
          val commonAncestor = gitCoreRepository.deriveAnyMergeBase(parentPointedCommit, pointedCommit);
          return commonAncestor != null
              ? ForkPointCommitOfManagedBranch.fallbackToParent(commonAncestor)
              : parentAgnosticForkPoint;
        }
      } else {
        val commonAncestor = gitCoreRepository.deriveAnyMergeBase(parentPointedCommit, pointedCommit);
        return commonAncestor != null ? ForkPointCommitOfManagedBranch.fallbackToParent(commonAncestor) : null;
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
  public RelationToRemote deriveRelationToRemote(IGitCoreLocalBranchSnapshot coreLocalBranch) throws GitCoreException {
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
  public SyncToParentStatus deriveSyncToParentStatus(
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
