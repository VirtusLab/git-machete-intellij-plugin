package com.virtuslab.gitcore.impl.jgit;

import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_BRANCH_SECTION;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_KEY_MERGE;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_KEY_REMOTE;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Function;

import io.vavr.CheckedFunction1;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.CustomLog;
import lombok.Getter;
import org.checkerframework.common.aliasing.qual.Unique;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.ReflogReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevWalkUtils;
import org.eclipse.jgit.revwalk.filter.RevFilter;

import com.virtuslab.gitcore.api.GitCoreBranchTrackingStatus;
import com.virtuslab.gitcore.api.GitCoreCannotAccessGitDirectoryException;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.GitCoreNoSuchRevisionException;
import com.virtuslab.gitcore.api.IGitCoreBranch;
import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranch;
import com.virtuslab.gitcore.api.IGitCoreRepository;

@CustomLog
public class GitCoreRepository implements IGitCoreRepository {
  private final Repository jgitRepo;
  @Getter
  private final Path mainDirectoryPath;
  private final Path gitDirectoryPath;

  private static final String ORIGIN = "origin";

  public GitCoreRepository(Path mainDirectoryPath, Path gitDirectoryPath) throws GitCoreException {
    LOG.debug(() -> "Creating ${getClass().getSimpleName()}(mainDirectoryPath = ${mainDirectoryPath}, " +
        "gitDirectoryPath = ${gitDirectoryPath})");
    this.mainDirectoryPath = mainDirectoryPath;
    this.gitDirectoryPath = gitDirectoryPath;

    this.jgitRepo = Try.of(() -> new FileRepository(gitDirectoryPath.toString())).getOrElseThrow(
        e -> new GitCoreCannotAccessGitDirectoryException("Cannot access .git directory under ${gitDirectoryPath}", e));
  }

  @Override
  public Option<IGitCoreLocalBranch> deriveCurrentBranch() throws GitCoreException {
    Ref ref;
    try {
      ref = jgitRepo.getRefDatabase().findRef(Constants.HEAD);
    } catch (IOException e) {
      throw new GitCoreException("Cannot get current branch", e);
    }
    if (ref == null) {
      throw new GitCoreException("Error occurred while getting current branch ref");
    }
    if (ref.isSymbolic()) {
      String currentBranchName = Repository.shortenRefName(ref.getTarget().getName());
      return deriveLocalBranch(currentBranchName);
    }
    return Option.none();
  }

  @Override
  public Option<IGitCoreLocalBranch> deriveLocalBranch(String localBranchShortName) {
    if (!isBranchPresent(Constants.R_HEADS + localBranchShortName)) {
      return Option.none();
    }

    IGitCoreRemoteBranch remoteBranch = Try.of(() -> deriveRemoteBranch(localBranchShortName).getOrNull())
        .getOrNull();
    return Option.some(new GitCoreLocalBranch(/* repo */ this, localBranchShortName, remoteBranch));
  }

  private Option<GitCoreRemoteBranch> deriveRemoteBranch(String remoteName, String remoteBranchShortName) {
    if (!isBranchPresent(Constants.R_REMOTES + remoteName + "/" + remoteBranchShortName)) {
      return Option.none();
    }
    return Option.some(new GitCoreRemoteBranch(/* repo */ this, remoteName, remoteBranchShortName));
  }

  @Override
  public List<GitCoreLocalBranch> deriveLocalBranches() throws GitCoreException {
    LOG.debug(() -> "Entering: repository = ${mainDirectoryPath} (${gitDirectoryPath})");
    LOG.debug("List of local branches:");
    return Try.of(() -> jgitRepo.getRefDatabase().getRefsByPrefix(Constants.R_HEADS))
        .getOrElseThrow(e -> new GitCoreException("Error while getting list of local branches", e))
        .stream()
        .filter(branch -> !branch.getName().equals(Constants.HEAD))
        .map(branch -> {
          LOG.debug(() -> "* " + branch.getName());
          return branch;
        })
        .map(ref -> {
          String localBranchShortName = ref.getName().replace(Constants.R_HEADS, /* replacement */ "");
          IGitCoreRemoteBranch remoteBranch = Try.of(() -> deriveRemoteBranch(localBranchShortName).getOrNull())
              .getOrNull();
          return new GitCoreLocalBranch(/* repo */ this, localBranchShortName, remoteBranch);
        })
        .collect(List.collector());
  }

  @Override
  public List<GitCoreRemoteBranch> deriveRemoteBranches(String remoteName) throws GitCoreException {
    LOG.debug(() -> "Entering: remoteName = ${remoteName}, repository = ${mainDirectoryPath} (${gitDirectoryPath})");
    LOG.debug("List of remote branches of '${remoteName}':");
    String remoteBranchFullNamePrefix = Constants.R_REMOTES + remoteName + "/";
    return Try.of(() -> jgitRepo.getRefDatabase().getRefsByPrefix(remoteBranchFullNamePrefix))
        .getOrElseThrow(e -> new GitCoreException("Error while getting list of remote branches", e))
        .stream()
        .filter(branch -> !branch.getName().equals(Constants.HEAD))
        .map(branch -> {
          LOG.debug(() -> "* " + branch.getName());
          return branch;
        })
        .map(ref -> {
          String remoteBranchShortName = ref.getName().replace(remoteBranchFullNamePrefix, /* replacement */ "");
          return new GitCoreRemoteBranch(/* repo */ this, remoteName, remoteBranchShortName);
        })
        .collect(List.collector());
  }

  @Override
  public List<String> deriveAllRemotes() {
    return List.ofAll(jgitRepo.getRemoteNames());
  }

  private Try<List<GitCoreRemoteBranch>> deriveRemoteBranchesTry(String remoteName) {
    return Try.of(() -> deriveRemoteBranches(remoteName));
  }

  @Override
  public List<GitCoreRemoteBranch> deriveAllRemoteBranches() throws GitCoreException {
    return Try.traverse(deriveAllRemotes(), this::deriveRemoteBranchesTry)
        .getOrElseThrow(GitCoreException::castOrWrap)
        .flatMap(Function.identity())
        .toList();
  }

  private Option<GitCoreRemoteBranch> deriveRemoteBranch(String localBranchShortName) {
    return deriveConfiguredRemoteBranch(localBranchShortName).orElse(() -> deriveInferredRemoteBranch(localBranchShortName));
  }

  private Option<GitCoreRemoteBranch> deriveConfiguredRemoteBranch(String localBranchShortName) {
    return deriveConfiguredRemoteName(localBranchShortName)
        .flatMap(remoteName -> deriveConfiguredRemoteBranchName(localBranchShortName)
            .flatMap(remoteShortBranchName -> Try.of(() -> deriveRemoteBranch(remoteName, remoteShortBranchName)).get()));
  }

  private Option<String> deriveConfiguredRemoteName(String localBranchShortName) {
    return Option.of(jgitRepo.getConfig().getString(CONFIG_BRANCH_SECTION, localBranchShortName, CONFIG_KEY_REMOTE));
  }

  private Option<String> deriveConfiguredRemoteBranchName(String localBranchShortName) {
    return Option.of(jgitRepo.getConfig().getString(CONFIG_BRANCH_SECTION, localBranchShortName, CONFIG_KEY_MERGE))
        .map(branchFullName -> branchFullName.replace(Constants.R_HEADS, /* replacement */ ""));
  }

  private Option<GitCoreRemoteBranch> deriveInferredRemoteBranch(String localBranchShortName) {
    var remotes = deriveAllRemotes();

    if (remotes.contains(ORIGIN)) {
      var maybeRemoteBranch = deriveRemoteBranch(ORIGIN, localBranchShortName);
      if (maybeRemoteBranch.isDefined()) {
        return maybeRemoteBranch;
      }
    }
    for (String otherRemote : remotes.reject(r -> r.equals(ORIGIN))) {
      var maybeRemoteBranch = deriveRemoteBranch(otherRemote, localBranchShortName);
      if (maybeRemoteBranch.isDefined()) {
        return maybeRemoteBranch;
      }
    }
    return Option.none();
  }

  @SuppressWarnings("IllegalCatch")
  <T> T withRevWalk(CheckedFunction1<RevWalk, T> fun) throws GitCoreException {
    try (RevWalk walk = new RevWalk(jgitRepo)) {
      return fun.apply(walk);
    } catch (Throwable e) {
      throw new GitCoreException(e);
    }
  }

  Option<GitCoreBranchTrackingStatus> deriveRemoteTrackingStatus(GitCoreLocalBranch localBranch) throws GitCoreException {
    IGitCoreRemoteBranch remoteBranch = localBranch.getRemoteTrackingBranch().getOrNull();
    if (remoteBranch == null) {
      return Option.none();
    }

    return withRevWalk(walk -> {
      @Unique RevCommit localCommit = walk.parseCommit(gitCoreCommitToObjectId(localBranch.getPointedCommit()));
      @Unique RevCommit remoteCommit = walk.parseCommit(gitCoreCommitToObjectId(remoteBranch.getPointedCommit()));

      var mergeBaseCommitHash = deriveMergeBaseIfNeeded(localBranch.getPointedCommit(), remoteBranch.getPointedCommit());
      if (mergeBaseCommitHash.isEmpty()) {
        return Option.none();
      }
      @Unique RevCommit mergeBase = walk.parseCommit(revStringToObjectId(mergeBaseCommitHash.get().getHashString()));

      // Yes, `walk` is leaked here.
      // `count()` calls `walk.reset()` at the very beginning but NOT at the end.
      // We should be careful NOT to use `walk` afterwards (or at least call `reset()` first).
      int aheadCount = RevWalkUtils.count(walk, localCommit, mergeBase);
      int behindCount = RevWalkUtils.count(walk, remoteCommit, mergeBase);

      return Option.some(GitCoreBranchTrackingStatus.of(remoteBranch.getRemoteName(), aheadCount, behindCount));
    });
  }

  private boolean isBranchPresent(String branchFullName) {
    return Try.of(() -> jgitRepo.resolve(branchFullName)).getOrNull() != null;
  }

  public GitCoreCommit revStringToGitCoreCommit(String revStr) throws GitCoreException {
    return withRevWalk(walk -> new GitCoreCommit(walk.parseCommit(revStringToObjectId(revStr))));
  }

  private ObjectId revStringToObjectId(String revStr) throws GitCoreException {
    ObjectId objectId;
    try {
      objectId = jgitRepo.resolve(revStr);
    } catch (IOException e) {
      throw new GitCoreException(e);
    }
    if (objectId == null) {
      throw new GitCoreNoSuchRevisionException("Commit '${revStr}' does not exist in this repository");
    }
    return objectId;
  }

  ObjectId gitCoreCommitToObjectId(IGitCoreCommit commit) throws GitCoreException {
    return revStringToObjectId(commit.getHash().getHashString());
  }

  Option<ReflogReader> getReflogReader(IGitCoreBranch branch) throws GitCoreException {
    try {
      return Option.of(jgitRepo.getReflogReader(branch.getFullName()));
    } catch (IOException e) {
      throw new GitCoreException(e);
    }
  }

  private Option<GitCoreCommitHash> deriveMergeBase(IGitCoreCommit c1, IGitCoreCommit c2) throws GitCoreException {
    LOG.debug(() -> "Entering: repository = ${mainDirectoryPath} (${gitDirectoryPath})");

    return withRevWalk(walk -> {
      walk.setRevFilter(RevFilter.MERGE_BASE);
      walk.markStart(walk.parseCommit(gitCoreCommitToObjectId(c1)));
      walk.markStart(walk.parseCommit(gitCoreCommitToObjectId(c2)));

      // Note that we'll get asking for one merge-base here
      // even if there is more than one (in the rare case of criss-cross histories).
      // This is still okay from the perspective of is-ancestor checks that are our sole use of merge-base:
      // * if any of c1, c2 is an ancestor of another,
      //   then there is exactly one merge-base - the ancestor,
      // * if neither of c1, c2 is an ancestor of another,
      //   then none of the (possibly more than one) merge-bases is equal to either of c1/c2 anyway.
      @Unique RevCommit mergeBase = walk.next();
      LOG.debug(() -> "Detected merge base for ${c1.getHash().getHashString()} " +
          "and ${c2.getHash().getHashString()} is ${mergeBase.toString()}");
      GitCoreCommitHash mergeBaseHash = mergeBase != null ? new GitCoreCommitHash(mergeBase.getId().getName()) : null;
      return Option.of(mergeBaseHash);
    });
  }

  // Note that this cache can be static since merge-base for the given two commits
  // will never change thanks to git commit graph immutability.
  private static final java.util.Map<Tuple2<IGitCoreCommit, IGitCoreCommit>, Option<GitCoreCommitHash>> mergeBaseCache = new java.util.HashMap<>();

  private Option<GitCoreCommitHash> deriveMergeBaseIfNeeded(IGitCoreCommit a, IGitCoreCommit b) throws GitCoreException {
    LOG.debug(() -> "Entering: commit1 = ${a.getHash().getHashString()}, commit2 = ${b.getHash().getHashString()}");
    var abKey = Tuple.of(a, b);
    var baKey = Tuple.of(b, a);
    if (mergeBaseCache.containsKey(abKey)) {
      LOG.debug(() -> "Merge base for ${a.getHash().getHashString()} and ${b.getHash().getHashString()} found in cache");
      return mergeBaseCache.get(abKey);
    } else if (mergeBaseCache.containsKey(baKey)) {
      LOG.debug(() -> "Merge base for ${b.getHash().getHashString()} and ${a.getHash().getHashString()} found in cache");
      return mergeBaseCache.get(baKey);
    } else {
      var result = deriveMergeBase(a, b);
      mergeBaseCache.put(abKey, result);
      return result;
    }
  }

  @Override
  public boolean isAncestor(IGitCoreCommit presumedAncestor, IGitCoreCommit presumedDescendant)
      throws GitCoreException {
    LOG.debug(() -> "Entering: presumedAncestor = ${presumedAncestor.getHash().getHashString()}, " +
        "presumedDescendant = ${presumedDescendant.getHash().getHashString()}");

    if (presumedAncestor.equals(presumedDescendant)) {
      LOG.debug("presumedAncestor is equal to presumedDescendant");
      return true;
    }
    var mergeBaseHash = deriveMergeBaseIfNeeded(presumedAncestor, presumedDescendant);
    if (mergeBaseHash.isEmpty()) {
      LOG.debug("Merge base of presumedAncestor and presumedDescendant not found " +
          "=> presumedAncestor is not ancestor of presumedDescendant");
      return false;
    }
    boolean isAncestor = mergeBaseHash.get().equals(presumedAncestor.getHash());
    LOG.debug("Merge base of presumedAncestor and presumedDescendant is equal to presumedAncestor " +
        "=> presumedAncestor is ancestor of presumedDescendant");
    return isAncestor;
  }
}
