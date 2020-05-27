package com.virtuslab.gitcore.impl.jgit;

import static com.virtuslab.gitcore.impl.jgit.BranchFullNameUtils.getLocalBranchFullName;
import static com.virtuslab.gitcore.impl.jgit.BranchFullNameUtils.getRemoteBranchFullName;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_BRANCH_SECTION;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_KEY_MERGE;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_KEY_REMOTE;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Predicate;

import io.vavr.CheckedFunction1;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Iterator;
import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.CustomLog;
import lombok.ToString;
import org.checkerframework.common.aliasing.qual.Unique;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.ReflogReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevWalkUtils;
import org.eclipse.jgit.revwalk.filter.RevFilter;

import com.virtuslab.gitcore.api.GitCoreBranchTrackingStatus;
import com.virtuslab.gitcore.api.GitCoreCannotAccessGitDirectoryException;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.GitCoreNoSuchRevisionException;
import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreCommitHash;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCoreReflogEntry;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranch;
import com.virtuslab.gitcore.api.IGitCoreRepository;

@CustomLog
@ToString(onlyExplicitlyIncluded = true)
public final class GitCoreRepository implements IGitCoreRepository {
  @ToString.Include
  private final Path mainDirectoryPath;
  @ToString.Include
  private final Path gitDirectoryPath;
  private final Repository jgitRepo;

  private static final String ORIGIN = "origin";

  public GitCoreRepository(Path mainDirectoryPath, Path gitDirectoryPath) throws GitCoreException {
    this.mainDirectoryPath = mainDirectoryPath;
    this.gitDirectoryPath = gitDirectoryPath;

    this.jgitRepo = Try.of(() -> new FileRepository(gitDirectoryPath.toString())).getOrElseThrow(
        e -> new GitCoreCannotAccessGitDirectoryException("Cannot access .git directory under ${gitDirectoryPath}", e));

    LOG.debug(() -> "Creating ${this})");
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
      return deriveLocalBranchByShortName(currentBranchName);
    }
    return Option.none();
  }

  private Either<GitCoreException, GitCoreCommit> derivePointedCommitByBranchFullName(String branchFullName) {
    try {
      return Either.right(revStringToGitCoreCommit(branchFullName));
    } catch (GitCoreException e) {
      return Either.left(new GitCoreException(e));
    }
  }

  private Either<GitCoreException, List<IGitCoreReflogEntry>> deriveReflogByBranchFullName(String branchFullName) {
    try {
      ReflogReader reflogReader = jgitRepo.getReflogReader(branchFullName);
      if (reflogReader == null) {
        return Either.left(new GitCoreNoSuchRevisionException("Branch '${branchFullName}' does not exist in this repository"));
      }
      List<IGitCoreReflogEntry> result = reflogReader
          .getReverseEntries()
          .stream()
          .map(GitCoreReflogEntry::of)
          .collect(List.collector());
      return Either.right(result);
    } catch (IOException e) {
      return Either.left(new GitCoreException(e));
    }
  }

  @Override
  public Option<GitCoreBranchTrackingStatus> deriveRemoteTrackingStatus(IGitCoreLocalBranch localBranch)
      throws GitCoreException {
    IGitCoreRemoteBranch remoteBranch = localBranch.getRemoteTrackingBranch().getOrNull();
    if (remoteBranch == null) {
      return Option.none();
    }

    return withRevWalk(walk -> {
      @Unique RevCommit localCommit = walk.parseCommit(gitCoreCommitToObjectId(localBranch.derivePointedCommit()));
      @Unique RevCommit remoteCommit = walk.parseCommit(gitCoreCommitToObjectId(remoteBranch.derivePointedCommit()));

      var mergeBaseCommitHash = deriveMergeBaseIfNeeded(localBranch.derivePointedCommit(), remoteBranch.derivePointedCommit());
      if (mergeBaseCommitHash.isEmpty()) {
        return Option.none();
      }
      @Unique RevCommit mergeBase = walk.parseCommit(revStringToObjectId(mergeBaseCommitHash.get().getHashString()));

      // Yes, `walk` is leaked here.
      // `count()` calls `walk.reset()` at the very beginning but NOT at the end.
      // `walk` must NOT be used afterwards (or at least without a prior `reset()` call).
      int aheadCount = RevWalkUtils.count(walk, localCommit, mergeBase);
      int behindCount = RevWalkUtils.count(walk, remoteCommit, mergeBase);

      return Option.some(GitCoreBranchTrackingStatus.of(aheadCount, behindCount));
    });
  }

  @Override
  public Option<IGitCoreLocalBranch> deriveLocalBranchByShortName(String localBranchShortName) {
    String localBranchFullName = getLocalBranchFullName(localBranchShortName);
    if (!isBranchPresent(localBranchFullName)) {
      return Option.none();
    }

    var remoteBranch = Try.of(() -> deriveRemoteBranchForLocalBranch(localBranchShortName).getOrNull()).getOrNull();
    var localBranch = new GitCoreLocalBranch(
        localBranchShortName,
        Lazy.of(() -> derivePointedCommitByBranchFullName(localBranchFullName)),
        Lazy.of(() -> deriveReflogByBranchFullName(localBranchFullName)),
        remoteBranch);

    return Option.some(localBranch);
  }

  private Option<GitCoreRemoteBranch> deriveRemoteBranchByName(String remoteName, String remoteBranchShortName) {
    String remoteBranchFullName = getRemoteBranchFullName(remoteName, remoteBranchShortName);
    if (!isBranchPresent(remoteBranchFullName)) {
      return Option.none();
    }
    var remoteBranch = new GitCoreRemoteBranch(
        remoteBranchShortName,
        Lazy.of(() -> derivePointedCommitByBranchFullName(remoteBranchFullName)),
        Lazy.of(() -> deriveReflogByBranchFullName(remoteBranchFullName)),
        remoteName);
    return Option.some(remoteBranch);
  }

  @Override
  public List<IGitCoreLocalBranch> deriveAllLocalBranches() throws GitCoreException {
    LOG.debug(() -> "Entering: repository = ${mainDirectoryPath} (${gitDirectoryPath})");
    LOG.debug("List of local branches:");
    return Try.of(() -> jgitRepo.getRefDatabase().getRefsByPrefix(Constants.R_HEADS))
        .getOrElseThrow(e -> new GitCoreException("Error while getting list of local branches", e))
        .stream()
        .filter(branch -> !branch.getName().equals(Constants.HEAD))
        .map(ref -> {
          String localBranchFullName = ref.getName();
          LOG.debug(() -> "* " + localBranchFullName);
          String localBranchShortName = localBranchFullName.replace(Constants.R_HEADS, /* replacement */ "");

          var remoteBranch = Try.of(() -> deriveRemoteBranchForLocalBranch(localBranchShortName).getOrNull()).getOrNull();
          return new GitCoreLocalBranch(
              localBranchShortName,
              Lazy.of(() -> derivePointedCommitByBranchFullName(localBranchFullName)),
              Lazy.of(() -> deriveReflogByBranchFullName(localBranchFullName)),
              remoteBranch);
        })
        .collect(List.collector());
  }

  private List<IGitCoreRemoteBranch> deriveRemoteBranchesForRemote(String remoteName) throws GitCoreException {
    LOG.debug(() -> "Entering: remoteName = ${remoteName}, repository = ${mainDirectoryPath} (${gitDirectoryPath})");
    LOG.debug("List of remote branches of '${remoteName}':");
    String remoteBranchFullNamePrefix = Constants.R_REMOTES + remoteName + "/";
    return Try.of(() -> jgitRepo.getRefDatabase().getRefsByPrefix(remoteBranchFullNamePrefix))
        .getOrElseThrow(e -> new GitCoreException("Error while getting list of remote branches", e))
        .stream()
        .filter(branch -> !branch.getName().equals(Constants.HEAD))
        .map(ref -> {
          String remoteBranchFullName = ref.getName();
          LOG.debug(() -> "* " + remoteBranchFullName);

          String remoteBranchShortName = remoteBranchFullName.replace(remoteBranchFullNamePrefix, /* replacement */ "");
          return new GitCoreRemoteBranch(
              remoteBranchShortName,
              Lazy.of(() -> derivePointedCommitByBranchFullName(remoteBranchFullName)),
              Lazy.of(() -> deriveReflogByBranchFullName(remoteBranchFullName)),
              remoteName);
        })
        .collect(List.collector());
  }

  private List<String> deriveAllRemotes() {
    return List.ofAll(jgitRepo.getRemoteNames());
  }

  private Try<List<IGitCoreRemoteBranch>> tryDeriveRemoteBranchesForRemote(String remoteName) {
    return Try.of(() -> deriveRemoteBranchesForRemote(remoteName));
  }

  @Override
  public List<IGitCoreRemoteBranch> deriveAllRemoteBranches() throws GitCoreException {
    return Try.traverse(deriveAllRemotes(), this::tryDeriveRemoteBranchesForRemote)
        .getOrElseThrow(GitCoreException::getOrWrap)
        .flatMap(e -> e)
        .map(IGitCoreRemoteBranch.class::cast)
        .toList();
  }

  private Option<GitCoreRemoteBranch> deriveRemoteBranchForLocalBranch(String localBranchShortName) {
    return deriveConfiguredRemoteBranchForLocalBranch(localBranchShortName)
        .orElse(() -> deriveInferredRemoteBranchForLocalBranch(localBranchShortName));
  }

  private Option<GitCoreRemoteBranch> deriveConfiguredRemoteBranchForLocalBranch(String localBranchShortName) {
    return deriveConfiguredRemoteNameForLocalBranch(localBranchShortName)
        .flatMap(remoteName -> deriveConfiguredRemoteBranchNameForLocalBranch(localBranchShortName)
            .flatMap(remoteShortBranchName -> Try.of(() -> deriveRemoteBranchByName(remoteName, remoteShortBranchName)).get()));
  }

  private Option<String> deriveConfiguredRemoteNameForLocalBranch(String localBranchShortName) {
    return Option.of(jgitRepo.getConfig().getString(CONFIG_BRANCH_SECTION, localBranchShortName, CONFIG_KEY_REMOTE));
  }

  private Option<String> deriveConfiguredRemoteBranchNameForLocalBranch(String localBranchShortName) {
    return Option.of(jgitRepo.getConfig().getString(CONFIG_BRANCH_SECTION, localBranchShortName, CONFIG_KEY_MERGE))
        .map(branchFullName -> branchFullName.replace(Constants.R_HEADS, /* replacement */ ""));
  }

  private Option<GitCoreRemoteBranch> deriveInferredRemoteBranchForLocalBranch(String localBranchShortName) {
    var remotes = deriveAllRemotes();

    if (remotes.contains(ORIGIN)) {
      var maybeRemoteBranch = deriveRemoteBranchByName(ORIGIN, localBranchShortName);
      if (maybeRemoteBranch.isDefined()) {
        return maybeRemoteBranch;
      }
    }
    for (String otherRemote : remotes.reject(r -> r.equals(ORIGIN))) {
      var maybeRemoteBranch = deriveRemoteBranchByName(otherRemote, localBranchShortName);
      if (maybeRemoteBranch.isDefined()) {
        return maybeRemoteBranch;
      }
    }
    return Option.none();
  }

  @SuppressWarnings("IllegalCatch")
  private <T> T withRevWalk(CheckedFunction1<RevWalk, T> fun) throws GitCoreException {
    try (RevWalk walk = new RevWalk(jgitRepo)) {
      return fun.apply(walk);
    } catch (Throwable e) {
      throw new GitCoreException(e);
    }
  }

  private boolean isBranchPresent(String branchFullName) {
    return Try.of(() -> jgitRepo.resolve(branchFullName)).getOrNull() != null;
  }

  private GitCoreCommit revStringToGitCoreCommit(String revStr) throws GitCoreException {
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

  private ObjectId gitCoreCommitToObjectId(IGitCoreCommit commit) throws GitCoreException {
    return revStringToObjectId(commit.getHash().getHashString());
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
      GitCoreCommitHash mergeBaseHash = mergeBase != null ? GitCoreCommitHash.of(mergeBase.getId()) : null;
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
  public boolean isAncestor(IGitCoreCommit presumedAncestor, IGitCoreCommit presumedDescendant) throws GitCoreException {
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

  @Override
  public List<IGitCoreCommit> deriveCommitRange(IGitCoreCommit fromInclusive, IGitCoreCommit untilExclusive)
      throws GitCoreException {
    LOG.debug(() -> "Entering: fromInclusive = '${fromInclusive}', untilExclusive = ${untilExclusive}");

    return withRevWalk(walk -> {
      walk.sort(RevSort.TOPO);
      walk.sort(RevSort.BOUNDARY);

      walk.markStart(walk.parseCommit(gitCoreCommitToObjectId(fromInclusive)));
      walk.markUninteresting(walk.parseCommit(gitCoreCommitToObjectId(untilExclusive)));

      LOG.debug("Starting revwalk");
      return Iterator.ofAll(walk.iterator())
          .takeUntil(revCommit -> revCommit.getId().getName().equals(untilExclusive.getHash().getHashString()))
          .map(revCommit -> {
            LOG.debug(() -> "* " + revCommit.getId().getName());
            return new GitCoreCommit(revCommit);
          })
          .collect(List.collector());
    });
  }

  @Override
  @SuppressWarnings("aliasing:enhancedfor.type.incompatible")
  public Option<IGitCoreCommit> findFirstAncestor(
      IGitCoreCommit fromInclusive,
      Predicate<IGitCoreCommitHash> predicate) throws GitCoreException {
    RevWalk walk = new RevWalk(jgitRepo);
    walk.sort(RevSort.TOPO);

    ObjectId objectId = gitCoreCommitToObjectId(fromInclusive);
    try {
      walk.markStart(walk.parseCommit(objectId));
    } catch (IOException e) {
      throw new GitCoreException(e);
    }

    // There's apparently no way for AliasingChecker to work correctly with generics
    // (in particular, with enhanced `for` loops, which are essentially syntax sugar over Iterator<...>);
    // hence we need to suppress `aliasing:enhancedfor.type.incompatible` here.
    for (@Unique RevCommit commit : walk) {
      if (predicate.test(GitCoreCommitHash.of(commit.getId()))) {
        return Option.some(new GitCoreCommit(commit));
      }
    }
    return Option.none();
  }
}
