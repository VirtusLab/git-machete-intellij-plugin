package com.virtuslab.gitcore.impl.jgit;

import static com.virtuslab.gitcore.impl.jgit.BranchFullNameUtils.getLocalBranchFullName;
import static com.virtuslab.gitcore.impl.jgit.BranchFullNameUtils.getRemoteBranchFullName;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_BRANCH_SECTION;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_KEY_MERGE;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_KEY_REMOTE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.vavr.CheckedFunction1;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.Iterator;
import io.vavr.collection.List;
import io.vavr.collection.Map;
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
import org.eclipse.jgit.errors.RevisionSyntaxException;
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
  // A single JGit Repository inferred from `rootDirectoryPath` via FileRepositoryBuilder#findGitDir,
  // which follows the `.git` gitlink file inside a linked worktree (yielding `.git/worktrees/<wt>`)
  // or returns `.git` directly for a plain single-worktree repo.
  //
  // Since JGit 7.0 (Sep 2024), BaseRepositoryBuilder honors the `commondir` pointer file that
  // `git worktree add` drops next to the per-worktree HEAD, so this single Repository handle
  // correctly routes:
  //   - HEAD, reflog of HEAD, in-progress operation files (MERGE_HEAD, CHERRY_PICK_HEAD,
  //     rebase-merge/, rebase-apply/, BISECT_START, repository state) -> per-worktree git dir,
  //   - refs, config, object database, per-ref reflogs -> common (main) git dir.
  //
  // The three Path getters on `IGitCoreRepository` (root / main git dir / worktree git dir) are
  // backed by `jgitRepo.getWorkTree()` / `getCommonDirectory()` / `getDirectory()` respectively.
  private final Repository jgitRepo;
  // Memoized for cheap getter access (the JGit getters return File and we'd be allocating a
  // fresh Path on every call otherwise; some callers like CreateGitMacheteRepositoryHelper hit
  // these getters repeatedly during snapshot construction).
  @Getter
  @ToString.Include
  private final Path rootDirectoryPath;
  @Getter
  @ToString.Include
  private final Path mainGitDirectoryPath;
  @Getter
  @ToString.Include
  private final Path worktreeGitDirectoryPath;

  private static final String ORIGIN = "origin";

  // Note that these caches can be static since merge-base and commit range for the given two commits
  // will never change thanks to git commit graph immutability.
  private static final java.util.Map<Tuple2<IGitCoreCommit, IGitCoreCommit>, @Nullable GitCoreCommitHash> mergeBaseCache = new java.util.HashMap<>();
  private static final java.util.Map<Tuple2<IGitCoreCommit, IGitCoreCommit>, List<IGitCoreCommit>> commitRangeCache = new java.util.HashMap<>();

  @UIThreadUnsafe
  public GitCoreRepository(Path rootDirectoryPath) throws GitCoreException {
    val builder = new FileRepositoryBuilder()
        .findGitDir(rootDirectoryPath.toFile())
        .setMustExist(true);

    try {
      this.jgitRepo = builder.build();
    } catch (IOException e) {
      throw new GitCoreCannotAccessGitDirectoryException("Cannot create a repository object for " +
          "rootDirectoryPath=${rootDirectoryPath}", e);
    }

    this.rootDirectoryPath = jgitRepo.getWorkTree().toPath();
    this.mainGitDirectoryPath = jgitRepo.getCommonDirectory().toPath();
    this.worktreeGitDirectoryPath = jgitRepo.getDirectory().toPath();

    LOG.debug(() -> "Created ${this})");
  }

  @Override
  @UIThreadUnsafe
  public @Nullable String deriveConfigValue(String section, String subsection, String name) {
    return jgitRepo.getConfig().getString(section, subsection, name);
  }

  @Override
  @UIThreadUnsafe
  public @Nullable String deriveConfigValue(String section, String name) {
    return jgitRepo.getConfig().getString(section, null, name);
  }

  @Override
  @UIThreadUnsafe
  public @Nullable IGitCoreCommit parseRevision(String revision) throws GitCoreException {
    return convertRevisionToGitCoreCommit(revision);
  }

  @UIThreadUnsafe
  @SuppressWarnings("IllegalCatch")
  private <T> T withRevWalk(CheckedFunction1<RevWalk, T> fun) throws GitCoreException {
    try (RevWalk walk = new RevWalk(jgitRepo)) {
      return fun.apply(walk);
    } catch (Throwable e) {
      throw new GitCoreException(e);
    }
  }

  @UIThreadUnsafe
  @SneakyThrows
  private <T> T withRevWalkUnchecked(CheckedFunction1<RevWalk, T> fun) {
    try (RevWalk walk = new RevWalk(jgitRepo)) {
      return fun.apply(walk);
    }
  }

  // Visible only for the sake of tests, not a part of the interface
  @UIThreadUnsafe
  boolean isBranchPresent(String branchFullName) {
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
    // A loop-based test below checks if there is a branch that has a name equal to a part of the `branchFullName` -
    // - without the last segment (last part of the path). If such a branch exists, `isBranchPresent` should return false.
    // Reasoning: if branch 'foo' exists, then for sure branch 'foo/bar' does not exist in the same directory.
    // Starting with `numOfSegmentsToUse = 3` as 3 is the lowest number of segments that can correspond
    // to a branch name (for `refs/heads/<branch_name>`)
    for (int numOfSegmentsToUse = 3; numOfSegmentsToUse < segments.size(); numOfSegmentsToUse++) {
      val testedPrefix = segments.take(numOfSegmentsToUse).mkString("/");
      try {
        val objectId = jgitRepo.resolve(testedPrefix);
        if (objectId != null) {
          return false;
        }
      } catch (IOException ignored) {
        // See https://github.com/VirtusLab/git-machete-intellij-plugin/issues/1298
      } catch (RevisionSyntaxException ignored) {
        // See https://github.com/VirtusLab/git-machete-intellij-plugin/issues/1826
      }
    }

    try {
      return jgitRepo.resolve(branchFullName) != null;
    } catch (IOException | RevisionSyntaxException e) {
      return false;
    }
  }

  @UIThreadUnsafe
  private GitCoreCommit convertExistingRevisionToGitCoreCommit(String revision) throws GitCoreException {
    return withRevWalk(walk -> new GitCoreCommit(walk.parseCommit(convertExistingRevisionToObjectId(revision))));
  }

  @UIThreadUnsafe
  private GitCoreCommit convertObjectIdToGitCoreCommit(ObjectId objectId) throws GitCoreException {
    return withRevWalk(walk -> new GitCoreCommit(walk.parseCommit(objectId)));
  }

  @UIThreadUnsafe
  private @Nullable GitCoreCommit convertRevisionToGitCoreCommit(String revision) throws GitCoreException {
    val objectId = convertRevisionToObjectId(revision);
    return objectId != null
        ? Try.of(() -> withRevWalkUnchecked(walk -> new GitCoreCommit(walk.parseCommit(objectId)))).getOrNull()
        : null;
  }

  @UIThreadUnsafe
  private ObjectId convertExistingRevisionToObjectId(String revision) throws GitCoreException {
    val objectId = convertRevisionToObjectId(revision);
    if (objectId == null) {
      throw new GitCoreNoSuchRevisionException("Commit '${revision}' does not exist in this repository");
    }
    return objectId;
  }

  @UIThreadUnsafe
  private @Nullable ObjectId convertRevisionToObjectId(String revision) throws GitCoreException {
    try {
      return jgitRepo.resolve(revision);
    } catch (IOException e) {
      throw new GitCoreException(e);
    } catch (RevisionSyntaxException e) {
      // See https://github.com/VirtusLab/git-machete-intellij-plugin/issues/1826
      LOG.warn("convertRevisionToObjectId failed on invalid revision syntax", e);
      return null;
    }
  }

  @UIThreadUnsafe
  private ObjectId convertGitCoreCommitToObjectId(IGitCoreCommit commit) throws GitCoreException {
    return convertExistingRevisionToObjectId(commit.getHash().getHashString());
  }

  @UIThreadUnsafe
  @Override
  public IGitCoreHeadSnapshot deriveHead() throws GitCoreException {
    try {
      Ref ref = jgitRepo.getRefDatabase().findRef(Constants.HEAD);

      if (ref == null) {
        throw new GitCoreException("Error occurred while getting current branch ref");
      }

      // Unlike branches which are shared between all worktrees, HEAD is defined on per-worktree basis;
      // JGit honors that via the `commondir` pointer file inside `.git/worktrees/<wt>`.
      val reflog = deriveReflogByRefFullName(Constants.HEAD);

      String currentBranchName = null;

      if (ref.isSymbolic()) {
        currentBranchName = Repository.shortenRefName(ref.getTarget().getName());
      } else {
        Option<Path> headNamePath = Stream.of("rebase-apply", "rebase-merge")
            .map(dir -> jgitRepo.getDirectory().toPath().resolve(dir).resolve("head-name"))
            .find(path -> path.toFile().isFile());

        if (headNamePath.isDefined()) {
          currentBranchName = Stream.ofAll(Files.readAllLines(headNamePath.get()))
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
    } catch (IOException e) {
      throw new GitCoreException("Cannot get current branch", e);
    }
  }

  @UIThreadUnsafe
  private List<IGitCoreReflogEntry> deriveReflogByRefFullName(String refFullName) throws GitCoreException {
    try {
      ReflogReader reflogReader = jgitRepo.getRefDatabase().getReflogReader(refFullName);
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
  @UIThreadUnsafe
  public @Nullable GitCoreRelativeCommitCount deriveRelativeCommitCount(
      IGitCoreCommit fromPerspectiveOf,
      IGitCoreCommit asComparedTo) throws GitCoreException {

    return (GitCoreRelativeCommitCount) withRevWalk(walk -> {
      val mergeBaseHash = deriveAnyMergeBaseIfNeeded(fromPerspectiveOf, asComparedTo);
      if (mergeBaseHash == null) {
        // Nullness checker does not allow this method to return null, let's rely on Option instead
        return Option.none();
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

  @UIThreadUnsafe
  private @Nullable IGitCoreLocalBranchSnapshot deriveLocalBranchByName(String localBranchName) throws GitCoreException {
    String localBranchFullName = getLocalBranchFullName(localBranchName);
    if (!isBranchPresent(localBranchFullName)) {
      return null;
    }

    val remoteBranch = deriveRemoteBranchForLocalBranch(localBranchName);

    return new GitCoreLocalBranchSnapshot(
        localBranchName,
        convertExistingRevisionToGitCoreCommit(localBranchFullName),
        deriveReflogByRefFullName(localBranchFullName),
        remoteBranch);
  }

  @UIThreadUnsafe
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
        deriveReflogByRefFullName(remoteBranchFullName),
        remoteName);
    return remoteBranch;
  }

  @Override
  @UIThreadUnsafe
  public List<IGitCoreLocalBranchSnapshot> deriveAllLocalBranches() throws GitCoreException {
    LOG.debug(() -> "Entering: this = ${this}");
    LOG.debug("List of local branches:");
    List<Try<GitCoreLocalBranchSnapshot>> result = Try
        .of(() -> jgitRepo.getRefDatabase().getRefsByPrefix(Constants.R_HEADS))
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
          val reflog = deriveReflogByRefFullName(localBranchFullName);
          val remoteBranch = deriveRemoteBranchForLocalBranch(localBranchName);

          return new GitCoreLocalBranchSnapshot(localBranchName, pointedCommit, reflog, remoteBranch);
        }))
        .collect(List.collector());
    return List.narrow(Try.sequence(result).getOrElseThrow(GitCoreException::getOrWrap).toList().sortBy(b -> b.getName()));
  }

  @Override
  @UIThreadUnsafe
  public List<String> deriveAllRemoteNames() {
    return List.ofAll(jgitRepo.getRemoteNames());
  }

  @Override
  @UIThreadUnsafe
  public @Nullable String deriveRebasedBranch() throws GitCoreException {
    Option<Path> headNamePath = Stream.of("rebase-apply", "rebase-merge")
        .map(dir -> jgitRepo.getDirectory().toPath().resolve(dir).resolve("head-name"))
        .find(path -> path.toFile().isFile());

    try {
      return headNamePath.isDefined()
          ? Stream.ofAll(Files.readAllLines(headNamePath.get()))
              .headOption()
              .map(Repository::shortenRefName).getOrNull()
          : null;
    } catch (IOException e) {
      throw new GitCoreException("Error occurred while getting currently rebased branch name", e);
    }
  }

  @UIThreadUnsafe
  @Override
  public @Nullable String deriveBisectedBranch() throws GitCoreException {
    Path headNamePath = jgitRepo.getDirectory().toPath().resolve("BISECT_START");

    try {
      return headNamePath.toFile().isFile()
          ? Stream.ofAll(Files.readAllLines(headNamePath))
              .headOption()
              .map(Repository::shortenRefName).getOrNull()
          : null;
    } catch (IOException e) {
      throw new GitCoreException("Error occurred while getting currently bisected branch name", e);
    }
  }

  @UIThreadUnsafe
  private @Nullable GitCoreRemoteBranchSnapshot deriveRemoteBranchForLocalBranch(String localBranchName) {
    val configuredRemoteBranchForLocalBranch = deriveConfiguredRemoteBranchForLocalBranch(localBranchName);

    try {
      return configuredRemoteBranchForLocalBranch != null
          ? configuredRemoteBranchForLocalBranch
          : deriveInferredRemoteBranchForLocalBranch(localBranchName);
    } catch (GitCoreException ignored) {}
    return null;
  }

  @UIThreadUnsafe
  private @Nullable GitCoreRemoteBranchSnapshot deriveConfiguredRemoteBranchForLocalBranch(String localBranchName) {
    val remoteName = deriveConfiguredRemoteNameForLocalBranch(localBranchName);
    val remoteShortBranchName = remoteName != null ? deriveConfiguredRemoteBranchNameForLocalBranch(localBranchName) : null;

    try {
      if (remoteShortBranchName != null && remoteName != null) {
        return deriveRemoteBranchByName(remoteName, remoteShortBranchName);
      }

    } catch (GitCoreException ignored) {}

    return null;
  }

  @UIThreadUnsafe
  private @Nullable String deriveConfiguredRemoteNameForLocalBranch(String localBranchName) {
    return jgitRepo.getConfig().getString(CONFIG_BRANCH_SECTION, localBranchName, CONFIG_KEY_REMOTE);
  }

  @UIThreadUnsafe
  private @Nullable String deriveConfiguredRemoteBranchNameForLocalBranch(String localBranchName) {
    val branchFullName = jgitRepo.getConfig().getString(CONFIG_BRANCH_SECTION, localBranchName, CONFIG_KEY_MERGE);
    return branchFullName != null ? branchFullName.replace(Constants.R_HEADS, /* replacement */ "") : null;
  }

  @UIThreadUnsafe
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

  @UIThreadUnsafe
  private @Nullable GitCoreCommitHash deriveAnyMergeBaseInternal(IGitCoreCommit c1, IGitCoreCommit c2) throws GitCoreException {
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

  @UIThreadUnsafe
  private @Nullable GitCoreCommitHash deriveAnyMergeBaseIfNeeded(IGitCoreCommit a, IGitCoreCommit b) throws GitCoreException {
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
      val result = deriveAnyMergeBaseInternal(a, b);
      mergeBaseCache.put(abKey, result);
      return result;
    }
  }

  @Override
  @UIThreadUnsafe
  public @Nullable IGitCoreCommit deriveAnyMergeBase(IGitCoreCommit commit1, IGitCoreCommit commit2) throws GitCoreException {
    if (commit1.equals(commit2)) {
      return commit1;
    }
    val mergeBaseHash = deriveAnyMergeBaseIfNeeded(commit1, commit2);
    if (mergeBaseHash == null) {
      return null;
    }
    return convertObjectIdToGitCoreCommit(mergeBaseHash.getObjectId());
  }

  @Override
  @UIThreadUnsafe
  public boolean isAncestor(IGitCoreCommit presumedAncestor, IGitCoreCommit presumedDescendant) throws GitCoreException {
    if (presumedAncestor.equals(presumedDescendant)) {
      LOG.debug("presumedAncestor is equal to presumedDescendant");
      return false;
    }
    return isAncestorOrEqual(presumedAncestor, presumedDescendant);
  }

  @Override
  @UIThreadUnsafe
  public boolean isAncestorOrEqual(IGitCoreCommit presumedAncestor, IGitCoreCommit presumedDescendant) throws GitCoreException {
    LOG.debug(() -> "Entering: presumedAncestor = ${presumedAncestor.getHash().getHashString()}, " +
        "presumedDescendant = ${presumedDescendant.getHash().getHashString()}");

    if (presumedAncestor.equals(presumedDescendant)) {
      LOG.debug("presumedAncestor is equal to presumedDescendant");
      return true;
    }
    val mergeBaseHash = deriveAnyMergeBaseIfNeeded(presumedAncestor, presumedDescendant);
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

  @UIThreadUnsafe
  private List<IGitCoreCommit> deriveCommitRangeInternal(IGitCoreCommit fromInclusive, IGitCoreCommit untilExclusive)
      throws GitCoreException {
    LOG.debug(() -> "Entering: fromInclusive = '${fromInclusive}', untilExclusive = '${untilExclusive}'");

    return withRevWalk(walk -> {
      // Note that `RevSort.COMMIT_TIME_DESC` is compatible with git-machete CLI,
      // which relies on vanilla `git log` under the hood,
      // which by default shows commits in reverse chronological order (https://git-scm.com/docs/git-log#_commit_ordering).
      // In this case (unlike with `ancestorsOf`), apparently there is no significant effect on performance.
      walk.sort(RevSort.COMMIT_TIME_DESC);
      // Note: deliberately NOT using `RevSort.BOUNDARY`. With BOUNDARY enabled, JGit yields the boundary commit
      // (an uninteresting commit with at least one interesting child) in addition to the interesting ones.
      // When `fromInclusive` is a descendant of `untilExclusive`, the boundary equals `untilExclusive` and could
      // be filtered out post-hoc; but when the two have diverged (e.g. red edges in `git machete status`), the
      // boundary equals the merge-base, which is neither `untilExclusive` nor reachable from it, and would leak
      // into the result. Without BOUNDARY the walk naturally stops before yielding uninteresting commits, which
      // matches `git log untilExclusive..fromInclusive` semantics for both descendant and divergent histories.

      walk.markStart(walk.parseCommit(convertGitCoreCommitToObjectId(fromInclusive)));
      walk.markUninteresting(walk.parseCommit(convertGitCoreCommitToObjectId(untilExclusive)));

      return Iterator.ofAll(walk.iterator())
          .toJavaStream()
          .peek(revCommit -> LOG.debug(() -> "* " + revCommit.getId().getName()))
          .map(GitCoreCommit::new)
          .collect(List.collector());
    });
  }

  @Override
  @UIThreadUnsafe
  public List<IGitCoreCommit> deriveCommitRange(IGitCoreCommit fromInclusive, IGitCoreCommit untilExclusive)
      throws GitCoreException {
    val key = Tuple.of(fromInclusive, untilExclusive);
    if (commitRangeCache.containsKey(key)) {
      return commitRangeCache.get(key);
    } else {
      val result = deriveCommitRangeInternal(fromInclusive, untilExclusive);
      commitRangeCache.put(key, result);
      return result;
    }
  }

  @Override
  @UIThreadUnsafe
  public GitCoreRepositoryState deriveRepositoryState() {
    val state = jgitRepo.getRepositoryState();
    return switch (state) {
      case CHERRY_PICKING, CHERRY_PICKING_RESOLVED -> GitCoreRepositoryState.CHERRY_PICKING;
      case MERGING, MERGING_RESOLVED -> GitCoreRepositoryState.MERGING;
      case REBASING, REBASING_INTERACTIVE, REBASING_MERGE, REBASING_REBASING -> GitCoreRepositoryState.REBASING;
      case REVERTING, REVERTING_RESOLVED -> GitCoreRepositoryState.REVERTING;
      case APPLY -> GitCoreRepositoryState.APPLYING;
      case BISECTING -> GitCoreRepositoryState.BISECTING;
      case SAFE -> GitCoreRepositoryState.NO_OPERATION;
      case BARE -> throw new IllegalStateException("Unexpected value: " + state);
    };
  }

  @Override
  @UIThreadUnsafe
  public Stream<IGitCoreCommit> ancestorsOf(IGitCoreCommit commitInclusive, int maxCommits) throws GitCoreException {
    RevWalk walk = new RevWalk(jgitRepo);
    // Note that `RevSort.COMMIT_TIME_DESC` is both:
    // * compatible with git-machete CLI, which relies on vanilla `git log` under the hood,
    //   which by default shows commits in reverse chronological order (https://git-scm.com/docs/git-log#_commit_ordering),
    // * significantly faster than `RevSort.TOPO` on repos with large histories (100,000's of commits),
    //   due to `org.eclipse.jgit.revwalk.TopoSortGenerator` constructor eagerly loading the entire git log.
    walk.sort(RevSort.COMMIT_TIME_DESC);

    ObjectId objectId = convertGitCoreCommitToObjectId(commitInclusive);
    try {
      walk.markStart(walk.parseCommit(objectId));
    } catch (IOException e) {
      throw new GitCoreException(e);
    }

    return Stream.ofAll(walk).take(maxCommits).map(GitCoreCommit::new);
  }

  @Override
  @UIThreadUnsafe
  public Map<String, Path> deriveWorktreeRootByLocalBranchName() throws GitCoreException {
    // We have to read .git/worktrees/<name>/{HEAD,gitdir} files directly here because, as of JGit 7.6, JGit exposes
    // no API to enumerate (or otherwise interrogate) linked worktrees - the entire `git worktree {list,add,remove,
    // move,prune,...}` family is unimplemented. Only *read* support for an already-known linked worktree was added
    // in JGit 7.0 (Sep 2024) via the `commondir` pointer, which is what backs the single `jgitRepo` field above;
    // but that lets us open a Repository given its per-worktree git dir, not discover those git dirs in the first
    // place. We could, in principle, list `worktrees/<name>` and build a fresh JGit Repository per linked worktree
    // just to call `getFullBranch()` on it, but that's strictly more work (a Repository is non-trivial to allocate)
    // for the same two files we'd be reading anyway.
    //
    // Tracking issues for full JGit worktree support:
    //   - https://bugs.eclipse.org/bugs/show_bug.cgi?id=477475 ("git 2.5 worktree support", open since 2015)
    //   - https://github.com/eclipse-jgit/jgit/issues/264 ("Feature Request: Git Worktree Support for JGit and EGit")
    // Until one of the management commands lands upstream, the manual layout walk below is the only option short
    // of shelling out to `git worktree list --porcelain`.
    //
    // The main worktree's HEAD lives at `<common-git-dir>/HEAD`; each linked worktree owns its own per-worktree git
    // directory under `<common-git-dir>/worktrees/<name>/`, with `HEAD` (same format as the main one) and `gitdir`
    // (an absolute path to the gitlink file at the worktree's root). The worktree root is therefore the parent of
    // the path stored in `gitdir`. We deliberately skip worktrees whose HEAD is detached - those cannot collide with
    // a checkout of a particular branch elsewhere.
    HashMap<String, Path> result = HashMap.empty();
    try {
      Path mainWorktreeRoot = mainGitDirectoryPath.getParent();
      if (mainWorktreeRoot != null) {
        String mainBranchName = readBranchShortNameFromHeadFile(mainGitDirectoryPath.resolve(Constants.HEAD));
        if (mainBranchName != null) {
          result = result.put(mainBranchName, mainWorktreeRoot);
        }
      }

      Path worktreesDir = mainGitDirectoryPath.resolve("worktrees");
      if (Files.isDirectory(worktreesDir)) {
        try (val entries = Files.list(worktreesDir)) {
          for (Path linkedWtGitDir : List.ofAll(entries).filter(Files::isDirectory)) {
            Path gitdirPointer = linkedWtGitDir.resolve("gitdir");
            Path headFile = linkedWtGitDir.resolve(Constants.HEAD);
            if (!Files.isRegularFile(gitdirPointer) || !Files.isRegularFile(headFile)) {
              continue;
            }
            String gitdirContent = Files.readString(gitdirPointer).trim();
            if (gitdirContent.isEmpty()) {
              continue;
            }
            Path linkedWtRoot = Path.of(gitdirContent).getParent();
            if (linkedWtRoot == null) {
              continue;
            }
            String linkedWtBranchName = readBranchShortNameFromHeadFile(headFile);
            if (linkedWtBranchName != null) {
              result = result.put(linkedWtBranchName, linkedWtRoot);
            }
          }
        }
      }
    } catch (IOException e) {
      throw new GitCoreException("Unable to enumerate worktrees under ${mainGitDirectoryPath}", e);
    }
    return result;
  }

  @UIThreadUnsafe
  private static @Nullable String readBranchShortNameFromHeadFile(Path headFile) throws IOException {
    if (!Files.isRegularFile(headFile)) {
      return null;
    }
    String content = Files.readString(headFile).trim();
    String prefix = "ref: " + Constants.R_HEADS;
    return content.startsWith(prefix) ? content.substring(prefix.length()).trim() : null;
  }
}
