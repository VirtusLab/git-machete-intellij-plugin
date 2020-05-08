package com.virtuslab.gitcore.impl.jgit;

import java.io.IOException;
import java.nio.file.Path;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;

import com.virtuslab.gitcore.api.BaseGitCoreCommit;
import com.virtuslab.gitcore.api.GitCoreCannotAccessGitDirectoryException;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.GitCoreNoSuchBranchException;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranch;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

@Getter
public class GitCoreRepository implements IGitCoreRepository {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("gitCore");

  private final Repository jgitRepo;
  private final Git jgitGit;
  private final Path mainDirectoryPath;
  private final Path gitDirectoryPath;

  public GitCoreRepository(Path mainDirectoryPath, Path gitDirectoryPath) throws GitCoreException {
    LOG.debug(() -> "Creating GitCoreRepository(mainDirectoryPath = ${mainDirectoryPath}, " +
        "gitDirectoryPath = ${gitDirectoryPath})");
    this.mainDirectoryPath = mainDirectoryPath;
    this.gitDirectoryPath = gitDirectoryPath;

    jgitRepo = Try.of(() -> new FileRepository(gitDirectoryPath.toString())).getOrElseThrow(
        e -> new GitCoreCannotAccessGitDirectoryException("Cannot access .git directory under ${gitDirectoryPath}", e));
    jgitGit = new Git(jgitRepo);
  }

  @Override
  public Option<IGitCoreLocalBranch> getCurrentBranch() throws GitCoreException {
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
      return Try.of(() -> getLocalBranch(currentBranchName)).toOption();
    }
    return Option.none();
  }

  @Override
  public IGitCoreLocalBranch getLocalBranch(String branchName) throws GitCoreException {
    if (isBranchMissing(GitCoreLocalBranch.BRANCHES_PATH + branchName)) {
      throw new GitCoreNoSuchBranchException("Local branch '${branchName}' does not exist in this repository");
    }

    String remoteName = deriveRemoteName(branchName);
    IGitCoreRemoteBranch remoteBranch = getRemoteBranch(branchName, remoteName).getOrNull();

    return new GitCoreLocalBranch(/* repo */ this, branchName, remoteName, remoteBranch);
  }

  @Override
  public Option<IGitCoreRemoteBranch> getRemoteBranch(String branchName, String remoteName) throws GitCoreException {
    if (isBranchMissing(GitCoreRemoteBranch.BRANCHES_PATH + remoteName + "/" + branchName)) {
      return Option.none();
    }
    return Option.of(new GitCoreRemoteBranch(/* repo */ this, branchName, remoteName));
  }

  @Override
  public List<IGitCoreLocalBranch> getLocalBranches() throws GitCoreException {
    LOG.debug(() -> "Entering: repository = ${mainDirectoryPath} (${gitDirectoryPath})");
    LOG.debug("List of local branches:");
    return Try.of(() -> getJgitGit().branchList().call())
        .getOrElseThrow(e -> new GitCoreException("Error while getting list of local branches", e))
        .stream()
        .filter(branch -> !branch.getName().equals(Constants.HEAD))
        .map(branch -> {
          LOG.debug(() -> "* ${branch.getName()}");
          return branch;
        })
        .map(ref -> {
          String shortBranchName = ref.getName().replace(GitCoreLocalBranch.BRANCHES_PATH, /* replacement */ "");
          String remoteName = deriveRemoteName(shortBranchName);
          return new GitCoreLocalBranch(/* repo */ this, shortBranchName, remoteName,
              Try.of(() -> getRemoteBranch(shortBranchName, remoteName).getOrNull()).getOrNull());
        })
        .collect(List.collector());
  }

  @Override
  public List<IGitCoreRemoteBranch> getRemoteBranches(String remoteName) throws GitCoreException {
    LOG.debug(() -> "Entering: remoteName = ${remoteName}, repository = ${mainDirectoryPath} (${gitDirectoryPath})");
    LOG.debug("List of remote branches of '${remoteName}':");
    return List
        .ofAll(
            Try.of(() -> getJgitRepo().getRefDatabase().getRefsByPrefix(GitCoreRemoteBranch.BRANCHES_PATH + remoteName + "/"))
                .getOrElseThrow(e -> new GitCoreException("Error while getting list of remote branches", e)))
        .filter(branch -> !branch.getName().equals(Constants.HEAD))
        .map(branch -> {
          LOG.debug(() -> "* ${branch.getName()}");
          return branch;
        })
        .map(ref -> {
          String shortBranchName = ref.getName().replace(GitCoreRemoteBranch.BRANCHES_PATH + remoteName + "/",
              /* replacement */ "");
          return new GitCoreRemoteBranch(/* repo */ this, shortBranchName, remoteName);
        });
  }

  @Override
  public List<String> getRemotes() {
    return List.ofAll(getJgitRepo().getRemoteNames());
  }

  @Override
  public List<IGitCoreRemoteBranch> getAllRemoteBranches() throws GitCoreException {
    return getRemotes().flatMap(remoteName -> Try.of(() -> getRemoteBranches(remoteName)).get());
  }

  private String deriveRemoteName(String localBranchShortName) {
    return jgitRepo.getConfig().getString(ConfigConstants.CONFIG_BRANCH_SECTION, localBranchShortName,
        ConfigConstants.CONFIG_KEY_REMOTE);
  }

  private boolean isBranchMissing(String fullBranchName) throws GitCoreException {
    return Try.of(() -> Option.of(jgitRepo.resolve(fullBranchName)))
        .getOrElseThrow(e -> new GitCoreException(e))
        .isEmpty();
  }

  private ObjectId toExistingObjectId(BaseGitCoreCommit c) throws GitCoreException {
    try {
      ObjectId result = jgitRepo.resolve(c.getHash().getHashString());
      assert result != null : "Invalid commit ${c}";
      return result;
    } catch (IOException e) {
      throw new GitCoreException(e);
    }
  }

  @Nullable
  private RevCommit deriveMergeBase(BaseGitCoreCommit c1, BaseGitCoreCommit c2) throws GitCoreException {
    LOG.debug(() -> "Entering: repository = ${mainDirectoryPath} (${gitDirectoryPath})");
    RevWalk walk = new RevWalk(jgitRepo);
    walk.setRevFilter(RevFilter.MERGE_BASE);
    try {
      walk.markStart(walk.parseCommit(toExistingObjectId(c1)));
      walk.markStart(walk.parseCommit(toExistingObjectId(c2)));
      RevCommit mergeBase = walk.next();
      LOG.debug(() -> "Detected merge base for ${c1.getHash().getHashString()} " +
          "and ${c2.getHash().getHashString()} is ${mergeBase}");
      return mergeBase;
    } catch (IOException e) {
      throw new GitCoreException(e);
    }
  }

  private final java.util.Map<Tuple2<BaseGitCoreCommit, BaseGitCoreCommit>, @Nullable GitCoreCommitHash> mergeBaseCache = new java.util.HashMap<>();

  @Nullable
  private GitCoreCommitHash deriveMergeBaseIfNeeded(BaseGitCoreCommit a, BaseGitCoreCommit b) throws GitCoreException {
    LOG.debug(() -> "Entering: commit1 = ${a.getHash().getHashString()}, commit2 = ${b.getHash().getHashString()}");
    var abKey = Tuple.of(a, b);
    var baKey = Tuple.of(b, a);
    if (mergeBaseCache.containsKey(abKey)) {
      LOG.debug(
          () -> "Merge base for ${a.getHash().getHashString()} and ${b.getHash().getHashString()} found in cache");
      return mergeBaseCache.get(abKey);
    } else if (mergeBaseCache.containsKey(baKey)) {
      LOG.debug(
          () -> "Merge base for ${b.getHash().getHashString()} and ${a.getHash().getHashString()} found in cache");
      return mergeBaseCache.get(baKey);
    } else {
      var mergeBase = deriveMergeBase(a, b);
      GitCoreCommitHash mergeBaseHash = mergeBase != null ? GitCoreCommitHash.of(mergeBase) : null;
      mergeBaseCache.put(abKey, mergeBaseHash);
      return mergeBaseHash;
    }
  }

  @Override
  public boolean isAncestor(BaseGitCoreCommit presumedAncestor, BaseGitCoreCommit presumedDescendant)
      throws GitCoreException {
    LOG.debug(() -> "Entering: presumedAncestor = ${presumedAncestor.getHash().getHashString()}, " +
        "presumedDescendant = ${presumedDescendant.getHash().getHashString()}");

    if (presumedAncestor.equals(presumedDescendant)) {
      LOG.debug("presumedAncestor is equal to presumedDescendant");
      return true;
    }
    var mergeBaseHash = deriveMergeBaseIfNeeded(presumedAncestor, presumedDescendant);
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
}
