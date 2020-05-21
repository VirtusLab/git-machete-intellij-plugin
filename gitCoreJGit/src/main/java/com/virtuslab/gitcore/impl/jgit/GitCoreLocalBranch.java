package com.virtuslab.gitcore.impl.jgit;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.CustomLog;
import org.checkerframework.common.aliasing.qual.Unique;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;

import com.virtuslab.gitcore.api.GitCoreBranchTrackingStatus;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranch;

@CustomLog
public class GitCoreLocalBranch extends BaseGitCoreBranch implements IGitCoreLocalBranch {

  @Nullable
  private final IGitCoreRemoteBranch remoteBranch;

  public GitCoreLocalBranch(GitCoreRepository repo, String shortBranchName, @Nullable IGitCoreRemoteBranch remoteBranch) {
    super(repo, shortBranchName);
    this.remoteBranch = remoteBranch;
  }

  @Override
  public String getFullName() {
    return Constants.R_HEADS + shortName;
  }

  @Override
  public String getBranchTypeString(boolean capitalized) {
    return capitalized ? "Local" : "local";
  }

  @Override
  public Option<GitCoreBranchTrackingStatus> deriveRemoteTrackingStatus() throws GitCoreException {
    return repo.deriveTrackingStatus(this);
  }

  @Override
  public Option<IGitCoreRemoteBranch> getRemoteTrackingBranch() {
    return Option.of(remoteBranch);
  }

  @Override
  @SuppressWarnings("aliasing:enhancedfor.type.incompatible")
  public Option<IGitCoreCommit> deriveForkPoint() throws GitCoreException {
    LOG.debug(() -> "Entering: branch = '${getFullName()}'");
    LOG.debug("Getting reflogs of local branches");

    Map<String, List<ReflogEntry>> filteredReflogByLocalBranchName = repo
        .getLocalBranches()
        .reject(this::equals)
        .toMap(branch -> Tuple.of(branch.getShortName(), Try.of(() -> branch.deriveFilteredReflog()).get()));

    LOG.debug("Getting reflogs of remote branches");

    Option<IGitCoreRemoteBranch> remoteTrackingBranch = getRemoteTrackingBranch();

    Map<String, List<ReflogEntry>> filteredReflogByRemoteBranchName = repo
        .getAllRemoteBranches()
        .reject(branch -> remoteTrackingBranch.isDefined() && remoteTrackingBranch.get().equals(branch))
        .toMap(branch -> Tuple.of(branch.getShortName(), Try.of(() -> branch.deriveFilteredReflog()).get()));

    Map<String, List<ReflogEntry>> filteredReflogsByBranchName = filteredReflogByLocalBranchName
        .merge(filteredReflogByRemoteBranchName);

    Seq<Tuple2<ObjectId, String>> objectIdAndBranchNamePairs = filteredReflogsByBranchName
        .flatMap(bnAres -> bnAres._2.map(re -> Tuple.of(re.getNewId(), bnAres._1)));
    Map<ObjectId, Seq<String>> branchesContainingInReflogByCommit = objectIdAndBranchNamePairs
        .groupBy(oidAbn -> oidAbn._1)
        .mapValues(oidAbns -> oidAbns.map(oidAbn -> oidAbn._2));

    LOG.debug("Start walking through logs");

    return repo.withRevWalk(walk -> {
      walk.sort(RevSort.TOPO);

      ObjectId objectId = repo.gitCoreCommitToObjectId(getPointedCommit());
      walk.markStart(walk.parseCommit(objectId));

      // There's apparently no way for AliasingChecker to work correctly with generics
      // (in particular, with enhanced `for` loops, which are essentially syntax sugar over Iterator<...>);
      // hence we need to suppress `aliasing:enhancedfor.type.incompatible` here.
      for (@Unique RevCommit currentBranchCommit : walk) {
        Option<Seq<String>> containingBranches = branchesContainingInReflogByCommit.get(currentBranchCommit.getId());
        if (containingBranches.isDefined()) {
          LOG.debug(() -> "Commit ${currentBranchCommit.getId().getName()} found " +
              "in filtered reflog(s) of ${containingBranches.get().mkString(\", \")}; " +
              "returning as fork point for branch '${getFullName()}'");
          return Option.of(new GitCoreCommit(currentBranchCommit));
        }
      }

      LOG.debug("Fork point for branch '${getFullName()}' not found");
      return Option.none();
    });
  }

  @Override
  public boolean hasJustBeenCreated() throws GitCoreException {
    List<ReflogEntry> reflog = deriveFilteredReflog();
    return reflog.isEmpty() || reflog.head().getOldId().equals(ObjectId.zeroId());
  }
}
