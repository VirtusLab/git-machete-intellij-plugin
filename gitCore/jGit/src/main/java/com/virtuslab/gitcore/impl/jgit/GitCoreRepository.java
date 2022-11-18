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
import org.checkerframework.checker.nullness.qual.Nullable;
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
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

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
  public @Nullable String deriveConfigValue(String section, String subsection, String name) {
    return jgitRepoForMainGitDir.getConfig().getString(section, subsection, name);
  }

  @Override
  public @Nullable String deriveConfigValue(String section, String name) {
    return jgitRepoForMainGitDir.getConfig().getString(section, null, name);
  }

  @Override
  public @Nullable IGitCoreCommit parseRevision(String revision) throws GitCoreException {
    return convertRevisionToGitCoreCommit(revision);
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
    // If '/' characters exist in the branch name, then loop-based testing is needed in order to avoid
    // possible IDE errors, which could appear in scenarios similar to the one explained below.
    // - If a branch 'foo' exists locally (which means that .git/refs/heads/foo file exists in the repository)
    // and
    // - There is a branch name entry "foo/bar" in the machete file
    // Then `org.eclipse.jgit.lib.Repository#resolve` called to check if `foo/bar` branch exists will try to
    // find the branch using the following file path:
    // .git/refs/heads/foo/bar
    // which will end in an IDE error with a "Not a directory" `java.nio.file.FileSystemException`.
    // Explanation:
    // 1) One of the classes used by `org.eclipse.jgit` to resolve the git branch is
    //   `org.eclipse.jgit.internal.storage.file.FileSnapshot`.
    // 2) `org.eclipse.jgit.internal.storage.file.FileSnapshot.<init>` called to find if .git/refs/heads/foo/bar exists
    //    will try to resolve this path, which will produce "Not a directory" `java.nio.file.FileSystemException`,
    //    because file .git/refs/heads/foo (part of the resolved path) is NOT a directory.
    // 3) Catching `FileSystemException` will produce a `LOG.error` - `org.slf4j.Logger#error(java.lang.String, java.lang.Throwable)`
    // 4) `LOG.error` will generate an IDE error. Note that it would NOT happen if `org.slf4j.Logger#error(java.lang.String)`
    //    was called instead.
    // So, the cause of the loop-based testing below is to avoid such IDE errors.

    val segments = List.of(branchFullName.split("/"));
    // loop-based test below checks if there is a branch that has a name equal to a part of the `branchFullName` -
    // - without the last segment (last part of the path). If such a branch exists, `isBranchPresent` should return
    // false. Reasoning: if branch 'foo' exists, then for sure branch 'foo/bar' does not exist in the same directory.
    // Starting with `numOfSegmentsToUse = 3` as 3 is the least number that can contain
    // the branch name (for `refs/heads/<branch_name>`)
    for (int numOfSegmentsToUse = 3; numOfSegmentsToUse < segments.size(); numOfSegmentsToUse++) {
      val testedPrefix = segments.take(numOfSegmentsToUse).mkString("/");
      if (Try.of(() -> jgitRepoForMainGitDir.resolve(testedPrefix)).getOrNull() != null) {
        return false;
      }
    }

    // test below is executed for the actual branch name
    return Try.of(() -> jgitRepoForMainGitDir.resolve(branchFullName)).getOrNull() != null;
  }

  private GitCoreCommit convertExistingRevisionToGitCoreCommit(String revision) throws GitCoreException {
    return withRevWalk(walk -> new GitCoreCommit(walk.parseCommit(convertExistingRevisionToObjectId(revision))));
  }

  private GitCoreCommit convertObjectIdToGitCoreCommit(ObjectId objectId) throws GitCoreException {
    return withRevWalk(walk -> new GitCoreCommit(walk.parseCommit(objectId)));
  }

  private @Nullable GitCoreCommit convertRevisionToGitCoreCommit(String revision) throws GitCoreException {
    val objectId = convertRevisionToObjectId(revision);
    return objectId != null
        ? Try.of(() -> withRevWalkUnchecked(walk -> new GitCoreCommit(walk.parseCommit(objectId)))).getOrNull()
        : null;
  }

  private ObjectId convertExistingRevisionToObjectId(String revision) throws GitCoreException {
    val objectId = convertRevisionToObjectId(revision);
    if (objectId == null) {
      throw new GitCoreNoSuchRevisionException("Commit '${revision}' does not exist in this repository");
    }
    return objectId;
  }

  private @Nullable ObjectId convertRevisionToObjectId(String revision) throws GitCoreException {
    try {
      return jgitRepoForMainGitDir.resolve(revision);
    } catch (IOException e) {
      throw new GitCoreException(e);
    }
  }

  private ObjectId convertGitCoreCommitToObjectId(IGitCoreCommit commit) throws GitCoreException {
    return convertExistingRevisionToObjectId(commit.getHash().getHashString());
  }

  @UIThreadUnsafe
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
      targetBranch = deriveLocalBranchByName(currentBranchName);
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
  public @Nullable GitCoreRelativeCommitCount deriveRelativeCommitCount(
      IGitCoreCommit fromPerspectiveOf,
      IGitCoreCommit asComparedTo) throws GitCoreException {

    return (GitCoreRelativeCommitCount) withRevWalk(walk -> {
      val mergeBaseHash = deriveMergeBaseIfNeeded(fromPerspectiveOf, asComparedTo);
      if (mergeBaseHash == null) {
        return Option.none(); // the nullness checker does not allow this method to return null.
      }

      @Unique RevCommit fromPerspectiveOfCommit = walk.parseCommit(convertGitCoreCommitToObjectId(fromPerspectiveOf));
      @Unique RevCommit asComparedToCommit = walk.parseCommit(convertGitCoreCommitToObjectId(asComparedTo));
      @Unique RevCommit mergeBase = walk.parseCommit(mergeBaseHash.getObjectId());

      // Yes, `walk` is leaked here.
      // `count()` calls `walk.reset()` at the very beginning but NOT at the end.
      // `walk` must NOT be used afterwards (or at least without a prior `reset()` call).
      @SuppressWarnings("aliasing:unique.leaked") int aheadCount = RevWalkUtils.count(walk, fromPerspectiveOfCommit, mergeBase);
      @SuppressWarnings("aliasing:unique.leaked") int behindCount = RevWalkUtils.count(walk, asComparedToCommit, mergeBase);

      return Option.some(GitCoreRelativeCommitCount.of(aheadCount, behindCount));
    }).getOrNull();
  }

  private @Nullable IGitCoreLocalBranchSnapshot deriveLocalBranchByName(String localBranchName) throws GitCoreException {
    String localBranchFullName = getLocalBranchFullName(localBranchName);
    if (!isBranchPresent(localBranchFullName)) {
      return null;
    }

    val remoteBranch = Try.of(() -> deriveRemoteBranchForLocalBranch(localBranchName)).getOrNull();
    val localBranch = new GitCoreLocalBranchSnapshot(
        localBranchName,
        convertExistingRevisionToGitCoreCommit(localBranchFullName),
        deriveReflogByRefFullName(localBranchFullName, jgitRepoForMainGitDir),
        remoteBranch);

    return localBranch;
  }

  private @Nullable GitCoreRemoteBranchSnapshot deriveRemoteBranchByName(
      String remoteName,
      String remoteBranchName) throws GitCoreException {

    String remoteBranchFullName = getRemoteBranchFullName(remoteName, remoteBranchName);
    if (!isBranchPresent(remoteBranchFullName)) {
      return null;
    }
    val remoteBranch = new GitCoreRemoteBranchSnapshot(
        remoteBranchName,
        convertExistingRevisionToGitCoreCommit(remoteBranchFullName),
        deriveReflogByRefFullName(remoteBranchFullName, jgitRepoForMainGitDir),
        remoteName);
    return remoteBranch;
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
          val remoteBranch = deriveRemoteBranchForLocalBranch(localBranchName);

          return new GitCoreLocalBranchSnapshot(localBranchName, pointedCommit, reflog, remoteBranch);
        }))
        .collect(List.collector());
    return List.narrow(Try.sequence(result).getOrElseThrow(GitCoreException::getOrWrap).toList().sortBy(b -> b.getName()));
  }

  @Override
  public List<String> deriveAllRemoteNames() {
    return List.ofAll(jgitRepoForMainGitDir.getRemoteNames());
  }

  @UIThreadUnsafe
  @Override
  public @Nullable String deriveRebasedBranch() throws GitCoreException {
    Option<Path> headNamePath = Stream.of("rebase-apply", "rebase-merge")
        .map(dir -> jgitRepoForWorktreeGitDir.getDirectory().toPath().resolve(dir).resolve("head-name"))
        .find(path -> path.toFile().isFile());
    return !headNamePath.isEmpty()
        ? Try.of(() -> Stream.ofAll(Files.readAllLines(headNamePath.get())))
            .getOrElseThrow(e -> new GitCoreException("Error occurred while getting currently rebased branch name", e))
            .headOption()
            .map(Repository::shortenRefName).getOrNull()
        : null;
  }

  @UIThreadUnsafe
  @Override
  public @Nullable String deriveBisectedBranch() throws GitCoreException {
    Path headNamePath = jgitRepoForWorktreeGitDir.getDirectory().toPath().resolve("BISECT_START");

    return headNamePath.toFile().isFile()
        ? Try.of(() -> Stream.ofAll(Files.readAllLines(headNamePath)))
            .getOrElseThrow(e -> new GitCoreException("Error occurred while getting currently bisected branch name", e))
            .headOption()
            .map(Repository::shortenRefName).getOrNull()
        : null;
  }

  private @Nullable GitCoreRemoteBranchSnapshot deriveRemoteBranchForLocalBranch(String localBranchName) {
    val configuredRemoteBranchForLocalBranch = deriveConfiguredRemoteBranchForLocalBranch(localBranchName);
    return configuredRemoteBranchForLocalBranch != null
        ? configuredRemoteBranchForLocalBranch
        : Try.of(() -> deriveInferredRemoteBranchForLocalBranch(localBranchName)).getOrNull();
  }

  private @Nullable GitCoreRemoteBranchSnapshot deriveConfiguredRemoteBranchForLocalBranch(String localBranchName) {
    val remoteName = deriveConfiguredRemoteNameForLocalBranch(localBranchName);
    val remoteShortBranchName = remoteName != null ? deriveConfiguredRemoteBranchNameForLocalBranch(localBranchName) : null;
    return remoteShortBranchName != null && remoteName != null
        ? Try.of(() -> deriveRemoteBranchByName(remoteName, remoteShortBranchName)).getOrNull()
        : null;
  }

  private @Nullable String deriveConfiguredRemoteNameForLocalBranch(String localBranchName) {
    return jgitRepoForMainGitDir.getConfig().getString(CONFIG_BRANCH_SECTION, localBranchName, CONFIG_KEY_REMOTE);
  }

  private @Nullable String deriveConfiguredRemoteBranchNameForLocalBranch(String localBranchName) {
    val branchFullName = jgitRepoForMainGitDir.getConfig().getString(CONFIG_BRANCH_SECTION, localBranchName, CONFIG_KEY_MERGE);
    return branchFullName != null ? branchFullName.replace(Constants.R_HEADS, /* replacement */ "") : null;
  }

  private @Nullable GitCoreRemoteBranchSnapshot deriveInferredRemoteBranchForLocalBranch(String localBranchName)
      throws GitCoreException {
    val remotes = deriveAllRemoteNames();

    if (remotes.contains(ORIGIN)) {
      val maybeRemoteBranch = deriveRemoteBranchByName(ORIGIN, localBranchName);
      if (maybeRemoteBranch != null) {
        return maybeRemoteBranch;
      }
    }
    for (String otherRemote : remotes.reject(r -> r.equals(ORIGIN))) {
      val maybeRemoteBranch = deriveRemoteBranchByName(otherRemote, localBranchName);
      if (maybeRemoteBranch != null) {
        return maybeRemoteBranch;
      }
    }
    return null;
  }

  private @Nullable GitCoreCommitHash deriveMergeBase(IGitCoreCommit c1, IGitCoreCommit c2) throws GitCoreException {
    LOG.debug(() -> "Entering: this = ${this}");

    return (GitCoreCommitHash) withRevWalk(walk -> {
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
        return Option.some(GitCoreCommitHash.toGitCoreCommitHash(mergeBase.getId()));
      } else {
        return Option.none();
      }
    }).getOrNull();
  }

  // Note that this cache can be static since merge-base for the given two commits
  // will never change thanks to git commit graph immutability.
  private static final java.util.Map<Tuple2<IGitCoreCommit, IGitCoreCommit>, @Nullable GitCoreCommitHash> mergeBaseCache = new java.util.HashMap<>();

  private @Nullable GitCoreCommitHash deriveMergeBaseIfNeeded(IGitCoreCommit a, IGitCoreCommit b) throws GitCoreException {
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
    if (mergeBaseHash == null) {
      LOG.debug("Merge base of presumedAncestor and presumedDescendant not found " +
          "=> presumedAncestor is not ancestor of presumedDescendant");
      return false;
    }
    boolean isAncestor = mergeBaseHash.equals(presumedAncestor.getHash());
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
