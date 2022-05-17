package com.virtuslab.gitcore.impl.jgit;

import static com.virtuslab.gitcore.impl.jgit.BranchFullNameUtils.getLocalBranchFullName;
import static com.virtuslab.gitcore.impl.jgit.BranchFullNameUtils.getRemoteBranchFullName;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static io.vavr.Predicates.isIn;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_BRANCH_SECTION;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_KEY_MERGE;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_KEY_REMOTE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.vavr.CheckedFunction1;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Iterator;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.CustomLog;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.val;
import org.checkerframework.common.aliasing.qual.Unique;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.ReflogReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevWalkUtils;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import com.virtuslab.gitcore.api.GitCoreCannotAccessGitDirectoryException;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.GitCoreNoSuchRevisionException;
import com.virtuslab.gitcore.api.GitCoreRelativeCommitCount;
import com.virtuslab.gitcore.api.GitCoreRepositoryState;
import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreHeadSnapshot;
import com.virtuslab.gitcore.api.IGitCoreLocalBranchSnapshot;
import com.virtuslab.gitcore.api.IGitCoreReflogEntry;
import com.virtuslab.gitcore.api.IGitCoreRepository;

@CustomLog
@ToString(onlyExplicitlyIncluded = true)
public final class GitCoreRepository implements IGitCoreRepository {
  @Getter
  @ToString.Include
  private final Path rootDirectoryPath;
  @Getter
  @ToString.Include
  private final Path mainGitDirectoryPath;
  @Getter
  @ToString.Include
  private final Path worktreeGitDirectoryPath;

  // As of early 2022, JGit still doesn't have a first-class support for multiple worktrees in a single repository.
  // See https://bugs.eclipse.org/bugs/show_bug.cgi?id=477475
  // As a workaround, let's create two separate JGit Repositories:

  // The one for main .git/ directory, used for most purposes, including as the target location for machete file:
  private final Repository jgitRepoForMainGitDir;
  // The one for per-worktree .git/worktrees/<worktree> directory,
  // used for HEAD and checking repository state (rebasing/merging etc.):
  private final Repository jgitRepoForWorktreeGitDir;

  private static final String ORIGIN = "origin";

  public GitCoreRepository(Path rootDirectoryPath, Path mainGitDirectoryPath, Path worktreeGitDirectoryPath)
      throws GitCoreException {
    this.rootDirectoryPath = rootDirectoryPath;
    this.mainGitDirectoryPath = mainGitDirectoryPath;
    this.worktreeGitDirectoryPath = worktreeGitDirectoryPath;

    val builderForMainGitDir = new FileRepositoryBuilder();
    builderForMainGitDir.setWorkTree(rootDirectoryPath.toFile());
    builderForMainGitDir.setGitDir(mainGitDirectoryPath.toFile());
    this.jgitRepoForMainGitDir = Try.of(() -> builderForMainGitDir.build()).getOrElseThrow(
        e -> new GitCoreCannotAccessGitDirectoryException("Cannot create a repository object for " +
            "rootDirectoryPath=${rootDirectoryPath}, mainGitDirectoryPath=${mainGitDirectoryPath}", e));

    val builderForWorktreeGitDir = new FileRepositoryBuilder();
    builderForWorktreeGitDir.setWorkTree(rootDirectoryPath.toFile());
    builderForWorktreeGitDir.setGitDir(worktreeGitDirectoryPath.toFile());
    this.jgitRepoForWorktreeGitDir = Try.of(() -> builderForWorktreeGitDir.build()).getOrElseThrow(
        e -> new GitCoreCannotAccessGitDirectoryException("Cannot create a repository object for " +
            "rootDirectoryPath=${rootDirectoryPath}, worktreeGitDirectoryPath=${worktreeGitDirectoryPath}", e));

    LOG.debug(() -> "Created ${this})");
  }

  @Override
  public Option<String> deriveConfigValue(String section, String subsection, String name) {
    return Option.of(jgitRepoForMainGitDir.getConfig().getString(section, subsection, name));
  }

  @Override
  public Option<String> deriveConfigValue(String section, String name) {
    return Option.of(jgitRepoForMainGitDir.getConfig().getString(section, null, name));
  }

  @Override
  public Option<IGitCoreCommit> parseRevision(String revision) throws GitCoreException {
    return Option.narrow(convertRevisionToGitCoreCommit(revision));
  }

  @SuppressWarnings("IllegalCatch")
  private <T> T withRevWalk(CheckedFunction1<RevWalk, T> fun) throws GitCoreException {
    try (RevWalk walk = new RevWalk(jgitRepoForMainGitDir)) {
      return fun.apply(walk);
    } catch (Throwable e) {
      throw new GitCoreException(e);
    }
  }

  @SneakyThrows
  private <T> T withRevWalkUnchecked(CheckedFunction1<RevWalk, T> fun) {
    try (RevWalk walk = new RevWalk(jgitRepoForMainGitDir)) {
      return fun.apply(walk);
    }
  }

  private boolean isBranchPresent(String branchFullName) {
    return Try.of(() -> jgitRepoForMainGitDir.resolve(branchFullName)).getOrNull() != null;
  }

  private GitCoreCommit convertExistingRevisionToGitCoreCommit(String revision) throws GitCoreException {
    return withRevWalk(walk -> new GitCoreCommit(walk.parseCommit(convertExistingRevisionToObjectId(revision))));
  }

  private GitCoreCommit convertObjectIdToGitCoreCommit(ObjectId objectId) throws GitCoreException {
    return withRevWalk(walk -> new GitCoreCommit(walk.parseCommit(objectId)));
  }

  private Option<GitCoreCommit> convertRevisionToGitCoreCommit(String revision) throws GitCoreException {
    return convertRevisionToObjectId(revision)
        .map(objectId -> withRevWalkUnchecked(walk -> new GitCoreCommit(walk.parseCommit(objectId))));
  }

  private ObjectId convertExistingRevisionToObjectId(String revision) throws GitCoreException {
    return convertRevisionToObjectId(revision)
        .getOrElseThrow(() -> new GitCoreNoSuchRevisionException("Commit '${revision}' does not exist in this repository"));
  }

  private Option<ObjectId> convertRevisionToObjectId(String revision) throws GitCoreException {
    try {
      return Option.of(jgitRepoForMainGitDir.resolve(revision));
    } catch (IOException e) {
      throw new GitCoreException(e);
    }
  }

  private ObjectId convertGitCoreCommitToObjectId(IGitCoreCommit commit) throws GitCoreException {
    return convertExistingRevisionToObjectId(commit.getHash().getHashString());
  }

  @Override
  public IGitCoreHeadSnapshot deriveHead() throws GitCoreException {
    Ref ref = Try.of(() -> jgitRepoForWorktreeGitDir.getRefDatabase().findRef(Constants.HEAD))
        .getOrElseThrow(e -> new GitCoreException("Cannot get current branch", e));

    if (ref == null) {
      throw new GitCoreException("Error occurred while getting current branch ref");
    }

    // Unlike branches which are shared between all worktrees, HEAD is defined on per-worktree basis.
    val reflog = deriveReflogByRefFullName(Constants.HEAD, jgitRepoForWorktreeGitDir);

    String currentBranchName = null;

    if (ref.isSymbolic()) {
      currentBranchName = Repository.shortenRefName(ref.getTarget().getName());
    } else {
      Option<Path> headNamePath = Stream.of("rebase-apply", "rebase-merge")
          .map(dir -> jgitRepoForWorktreeGitDir.getDirectory().toPath().resolve(dir).resolve("head-name"))
          .find(path -> path.toFile().isFile());

      if (headNamePath.isDefined()) {
        currentBranchName = Try.of(() -> Stream.ofAll(Files.readAllLines(headNamePath.get())))
            .getOrElseThrow(e -> new GitCoreException("Error occurred while getting current branch ref", e))
            .headOption()
            .map(Repository::shortenRefName)
            .getOrNull();
      }
    }

    IGitCoreLocalBranchSnapshot targetBranch;
    if (currentBranchName != null) {
      targetBranch = deriveLocalBranchByName(currentBranchName).getOrNull();
    } else {
      targetBranch = null;
    }
    return new GitCoreHeadSnapshot(targetBranch, reflog);
  }

  private List<IGitCoreReflogEntry> deriveReflogByRefFullName(String refFullName, Repository repository)
      throws GitCoreException {
    try {
      ReflogReader reflogReader = repository.getReflogReader(refFullName);
      if (reflogReader == null) {
        throw new GitCoreNoSuchRevisionException("Ref '${refFullName}' does not exist in this repository");
      }
      return reflogReader
          .getReverseEntries()
          .stream()
          .map(GitCoreReflogEntry::new)
          .collect(List.collector());
    } catch (IOException e) {
      throw new GitCoreException(e);
    }
  }

  @Override
  public Option<GitCoreRelativeCommitCount> deriveRelativeCommitCount(
      IGitCoreCommit fromPerspectiveOf,
      IGitCoreCommit asComparedTo) throws GitCoreException {

    return withRevWalk(walk -> {
      val mergeBaseHash = deriveMergeBaseIfNeeded(fromPerspectiveOf, asComparedTo);
      if (mergeBaseHash.isEmpty()) {
        return Option.none();
      }

      @Unique RevCommit fromPerspectiveOfCommit = walk.parseCommit(convertGitCoreCommitToObjectId(fromPerspectiveOf));
      @Unique RevCommit asComparedToCommit = walk.parseCommit(convertGitCoreCommitToObjectId(asComparedTo));
      @Unique RevCommit mergeBase = walk.parseCommit(mergeBaseHash.get().getObjectId());

      // Yes, `walk` is leaked here.
      // `count()` calls `walk.reset()` at the very beginning but NOT at the end.
      // `walk` must NOT be used afterwards (or at least without a prior `reset()` call).
      @SuppressWarnings("aliasing:unique.leaked") int aheadCount = RevWalkUtils.count(walk, fromPerspectiveOfCommit, mergeBase);
      @SuppressWarnings("aliasing:unique.leaked") int behindCount = RevWalkUtils.count(walk, asComparedToCommit, mergeBase);

      return Option.some(GitCoreRelativeCommitCount.of(aheadCount, behindCount));
    });
  }

  private Option<IGitCoreLocalBranchSnapshot> deriveLocalBranchByName(String localBranchName) throws GitCoreException {
    String localBranchFullName = getLocalBranchFullName(localBranchName);
    if (!isBranchPresent(localBranchFullName)) {
      return Option.none();
    }

    val remoteBranch = Try.of(() -> deriveRemoteBranchForLocalBranch(localBranchName).getOrNull()).getOrNull();
    val localBranch = new GitCoreLocalBranchSnapshot(
        localBranchName,
        convertExistingRevisionToGitCoreCommit(localBranchFullName),
        deriveReflogByRefFullName(localBranchFullName, jgitRepoForMainGitDir),
        remoteBranch);

    return Option.some(localBranch);
  }

  private Option<GitCoreRemoteBranchSnapshot> deriveRemoteBranchByName(
      String remoteName,
      String remoteBranchName) throws GitCoreException {

    String remoteBranchFullName = getRemoteBranchFullName(remoteName, remoteBranchName);
    if (!isBranchPresent(remoteBranchFullName)) {
      return Option.none();
    }
    val remoteBranch = new GitCoreRemoteBranchSnapshot(
        remoteBranchName,
        convertExistingRevisionToGitCoreCommit(remoteBranchFullName),
        deriveReflogByRefFullName(remoteBranchFullName, jgitRepoForMainGitDir),
        remoteName);
    return Option.some(remoteBranch);
  }

  @Override
  public List<IGitCoreLocalBranchSnapshot> deriveAllLocalBranches() throws GitCoreException {
    LOG.debug(() -> "Entering: this = ${this}");
    LOG.debug("List of local branches:");
    List<Try<GitCoreLocalBranchSnapshot>> result = Try
        .of(() -> jgitRepoForMainGitDir.getRefDatabase().getRefsByPrefix(Constants.R_HEADS))
        .getOrElseThrow(e -> new GitCoreException("Error while getting list of local branches", e))
        .stream()
        .filter(branch -> !branch.getName().equals(Constants.HEAD))
        .map(ref -> Try.of(() -> {
          String localBranchFullName = ref.getName();
          LOG.debug(() -> "* " + localBranchFullName);

          String localBranchName = localBranchFullName.replace(Constants.R_HEADS, /* replacement */ "");
          val objectId = ref.getObjectId();
          if (objectId == null) {
            throw new GitCoreException("Cannot access git object id corresponding to ${localBranchFullName}");
          }
          val pointedCommit = convertObjectIdToGitCoreCommit(objectId);
          val reflog = deriveReflogByRefFullName(localBranchFullName, jgitRepoForMainGitDir);
          val remoteBranch = deriveRemoteBranchForLocalBranch(localBranchName).getOrNull();

          return new GitCoreLocalBranchSnapshot(localBranchName, pointedCommit, reflog, remoteBranch);
        }))
        .collect(List.collector());
    return List.narrow(Try.sequence(result).getOrElseThrow(GitCoreException::getOrWrap).toList().sortBy(b -> b.getName()));
  }

  @Override
  public List<String> deriveAllRemoteNames() {
    return List.ofAll(jgitRepoForMainGitDir.getRemoteNames());
  }

  @Override
  public Option<String> deriveRebaseBranch() throws GitCoreException {
    Path pathToRebasingBranchName = jgitRepoForWorktreeGitDir.getDirectory().toPath().resolve("rebase-merge")
        .resolve("head-name");
    return pathToRebasingBranchName.toFile().isFile()
        ? Try.of(() -> Stream.ofAll(Files.readAllLines(pathToRebasingBranchName)))
            .getOrElseThrow(e -> new GitCoreException("Error occurred while getting current rebasing branch name", e))
            .headOption()
            .map(Repository::shortenRefName)
        : Option.none();
  }

  private Option<GitCoreRemoteBranchSnapshot> deriveRemoteBranchForLocalBranch(String localBranchName) {
    return deriveConfiguredRemoteBranchForLocalBranch(localBranchName)
        .orElse(() -> Try.of(() -> deriveInferredRemoteBranchForLocalBranch(localBranchName)).getOrElse(Option.none()));
  }

  private Option<GitCoreRemoteBranchSnapshot> deriveConfiguredRemoteBranchForLocalBranch(String localBranchName) {
    return deriveConfiguredRemoteNameForLocalBranch(localBranchName)
        .flatMap(remoteName -> deriveConfiguredRemoteBranchNameForLocalBranch(localBranchName)
            .flatMap(remoteShortBranchName -> Try.of(() -> deriveRemoteBranchByName(remoteName, remoteShortBranchName))
                .getOrElse(Option.none())));
  }

  private Option<String> deriveConfiguredRemoteNameForLocalBranch(String localBranchName) {
    return Option.of(jgitRepoForMainGitDir.getConfig().getString(CONFIG_BRANCH_SECTION, localBranchName, CONFIG_KEY_REMOTE));
  }

  private Option<String> deriveConfiguredRemoteBranchNameForLocalBranch(String localBranchName) {
    return Option.of(jgitRepoForMainGitDir.getConfig().getString(CONFIG_BRANCH_SECTION, localBranchName, CONFIG_KEY_MERGE))
        .map(branchFullName -> branchFullName.replace(Constants.R_HEADS, /* replacement */ ""));
  }

  private Option<GitCoreRemoteBranchSnapshot> deriveInferredRemoteBranchForLocalBranch(String localBranchName)
      throws GitCoreException {
    val remotes = deriveAllRemoteNames();

    if (remotes.contains(ORIGIN)) {
      val maybeRemoteBranch = deriveRemoteBranchByName(ORIGIN, localBranchName);
      if (maybeRemoteBranch.isDefined()) {
        return maybeRemoteBranch;
      }
    }
    for (String otherRemote : remotes.reject(r -> r.equals(ORIGIN))) {
      val maybeRemoteBranch = deriveRemoteBranchByName(otherRemote, localBranchName);
      if (maybeRemoteBranch.isDefined()) {
        return maybeRemoteBranch;
      }
    }
    return Option.none();
  }

  private Option<GitCoreCommitHash> deriveMergeBase(IGitCoreCommit c1, IGitCoreCommit c2) throws GitCoreException {
    LOG.debug(() -> "Entering: this = ${this}");

    return withRevWalk(walk -> {
      walk.setRevFilter(RevFilter.MERGE_BASE);
      walk.markStart(walk.parseCommit(convertGitCoreCommitToObjectId(c1)));
      walk.markStart(walk.parseCommit(convertGitCoreCommitToObjectId(c2)));

      // Note that we're asking for only one merge-base here
      // even if there is more than one (in the rare case of criss-cross histories).
      // This is still okay from the perspective of is-ancestor checks:
      // * if any of c1, c2 is an ancestor of another,
      //   then there is exactly one merge-base - the ancestor,
      // * if neither of c1, c2 is an ancestor of another,
      //   then none of the (possibly more than one) merge-bases is equal to either of c1 or c2 anyway.
      // This might NOT necessarily be OK from the perspective of remote tracking status
      // i.e. the number of commits ahead of/behind remote, but in case of criss-cross histories
      // it's basically impossible to get these numbers correctly in a unambiguous manner.
      @Unique RevCommit mergeBase = walk.next();
      LOG.debug(() -> "Detected merge base for ${c1.getHash().getHashString()} " +
          "and ${c2.getHash().getHashString()} is " + (mergeBase != null ? mergeBase.getId().getName() : "<none>"));
      if (mergeBase != null) {
        return Option.some(GitCoreCommitHash.of(mergeBase.getId()));
      } else {
        return Option.none();
      }
    });
  }

  // Note that this cache can be static since merge-base for the given two commits
  // will never change thanks to git commit graph immutability.
  private static final java.util.Map<Tuple2<IGitCoreCommit, IGitCoreCommit>, Option<GitCoreCommitHash>> mergeBaseCache = new java.util.HashMap<>();

  private Option<GitCoreCommitHash> deriveMergeBaseIfNeeded(IGitCoreCommit a, IGitCoreCommit b) throws GitCoreException {
    LOG.debug(() -> "Entering: commit1 = ${a.getHash().getHashString()}, commit2 = ${b.getHash().getHashString()}");
    val abKey = Tuple.of(a, b);
    val baKey = Tuple.of(b, a);
    if (mergeBaseCache.containsKey(abKey)) {
      LOG.debug(() -> "Merge base for ${a.getHash().getHashString()} and ${b.getHash().getHashString()} found in cache");
      return mergeBaseCache.get(abKey);
    } else if (mergeBaseCache.containsKey(baKey)) {
      LOG.debug(() -> "Merge base for ${b.getHash().getHashString()} and ${a.getHash().getHashString()} found in cache");
      return mergeBaseCache.get(baKey);
    } else {
      val result = deriveMergeBase(a, b);
      mergeBaseCache.put(abKey, result);
      return result;
    }
  }

  @Override
  public boolean isAncestorOrEqual(IGitCoreCommit presumedAncestor, IGitCoreCommit presumedDescendant) throws GitCoreException {
    LOG.debug(() -> "Entering: presumedAncestor = ${presumedAncestor.getHash().getHashString()}, " +
        "presumedDescendant = ${presumedDescendant.getHash().getHashString()}");

    if (presumedAncestor.equals(presumedDescendant)) {
      LOG.debug("presumedAncestor is equal to presumedDescendant");
      return true;
    }
    val mergeBaseHash = deriveMergeBaseIfNeeded(presumedAncestor, presumedDescendant);
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
    LOG.debug(() -> "Entering: fromInclusive = '${fromInclusive}', untilExclusive = '${untilExclusive}'");

    return withRevWalk(walk -> {
      walk.sort(RevSort.TOPO);
      walk.sort(RevSort.BOUNDARY);

      walk.markStart(walk.parseCommit(convertGitCoreCommitToObjectId(fromInclusive)));
      walk.markUninteresting(walk.parseCommit(convertGitCoreCommitToObjectId(untilExclusive)));

      LOG.debug("Starting revwalk");
      return Iterator.ofAll(walk.iterator())
          .takeWhile(revCommit -> !revCommit.getId().getName().equals(untilExclusive.getHash().getHashString()))
          .toJavaStream()
          .peek(revCommit -> LOG.debug(() -> "* " + revCommit.getId().getName()))
          .map(GitCoreCommit::new)
          .collect(List.collector());
    });
  }

  @Override
  public GitCoreRepositoryState deriveRepositoryState() {
    return Match(jgitRepoForWorktreeGitDir.getRepositoryState()).of(
    // @formatter:off
        Case($(isIn(RepositoryState.CHERRY_PICKING, RepositoryState.CHERRY_PICKING_RESOLVED)),
            GitCoreRepositoryState.CHERRY_PICK),
        Case($(isIn(RepositoryState.MERGING, RepositoryState.MERGING_RESOLVED)),
            GitCoreRepositoryState.MERGING),
        Case($(isIn(RepositoryState.REBASING, RepositoryState.REBASING_INTERACTIVE, RepositoryState.REBASING_MERGE, RepositoryState.REBASING_REBASING)),
            GitCoreRepositoryState.REBASING),
        Case($(isIn(RepositoryState.REVERTING, RepositoryState.REVERTING_RESOLVED)),
            GitCoreRepositoryState.REVERTING),
        Case($(RepositoryState.APPLY),
            GitCoreRepositoryState.APPLYING),
        Case($(RepositoryState.BISECTING),
            GitCoreRepositoryState.BISECTING),
        Case($(), GitCoreRepositoryState.NORMAL));
    // @formatter:on
  }

  @Override
  public Stream<IGitCoreCommit> ancestorsOf(IGitCoreCommit commitInclusive) throws GitCoreException {
    RevWalk walk = new RevWalk(jgitRepoForMainGitDir);
    walk.sort(RevSort.TOPO);

    ObjectId objectId = convertGitCoreCommitToObjectId(commitInclusive);
    Try.run(() -> walk.markStart(walk.parseCommit(objectId)))
        .getOrElseThrow(e -> new GitCoreException(e));

    return Stream.ofAll(walk).map(GitCoreCommit::new);
  }
}
