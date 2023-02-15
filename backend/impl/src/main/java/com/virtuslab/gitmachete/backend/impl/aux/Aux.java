package com.virtuslab.gitmachete.backend.impl.aux;

import java.util.function.Predicate;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreBranchSnapshot;
import com.virtuslab.gitcore.api.IGitCoreCommitHash;
import com.virtuslab.gitcore.api.IGitCoreLocalBranchSnapshot;
import com.virtuslab.gitcore.api.IGitCoreReflogEntry;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranchSnapshot;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitmachete.backend.api.IBranchReference;
import com.virtuslab.gitmachete.backend.api.ILocalBranchReference;
import com.virtuslab.gitmachete.backend.impl.LocalBranchReference;
import com.virtuslab.gitmachete.backend.impl.RemoteTrackingBranchReference;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
public class Aux {
  protected final IGitCoreRepository gitCoreRepository;
  protected final List<IGitCoreLocalBranchSnapshot> localBranches;
  protected final Map<String, IGitCoreLocalBranchSnapshot> localBranchByName;

  private final java.util.Map<IGitCoreBranchSnapshot, List<IGitCoreReflogEntry>> filteredReflogByBranch = new java.util.HashMap<>();
  private @MonotonicNonNull Map<IGitCoreCommitHash, Seq<IBranchReference>> branchesContainingGivenCommitInReflog;

  @UIThreadUnsafe
  public Aux(IGitCoreRepository gitCoreRepository) throws GitCoreException {
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
  public ILocalBranchReference inferParentForLocalBranch(
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
