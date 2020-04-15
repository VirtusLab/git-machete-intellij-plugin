package com.virtuslab.gitcore.impl.jgit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;

import com.virtuslab.gitcore.api.BaseGitCoreCommit;
import com.virtuslab.gitcore.api.GitCoreCannotAccessGitDirectoryException;
import com.virtuslab.gitcore.api.GitCoreCannotReadGitFileException;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.GitCoreNoSuchBranchException;
import com.virtuslab.gitcore.api.GitCoreNoSuchRepositoryException;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranch;
import com.virtuslab.gitcore.api.IGitCoreRepository;

@Getter
public class GitCoreRepository implements IGitCoreRepository {
  private final Repository jgitRepo;
  private final Git jgitGit;
  private final Path repositoryPath;
  private final Path gitDirectoryPath;

  @Getter(AccessLevel.NONE)
  private static final Pattern GIT_DIR_PATTERN = Pattern.compile("^gitdir:\\s*(.*)");

  public GitCoreRepository(Path repositoryPath) throws GitCoreException {
    this.repositoryPath = repositoryPath;

    this.gitDirectoryPath = getGitDirectoryPathByRepoRootPath(repositoryPath);

    jgitRepo = Try.of(() -> new FileRepository(gitDirectoryPath.toString())).getOrElseThrow(
        e -> new GitCoreCannotAccessGitDirectoryException("Cannot access .git directory under ${gitDirectoryPath}", e));
    jgitGit = new Git(jgitRepo);
  }

  public static Path getGitDirectoryPathByRepoRootPath(Path pathToRepoRoot) throws GitCoreException {
    Path gitPath = pathToRepoRoot.resolve(".git");

    // In submodules and worktrees, .git is a file rather than a directory
    if (Files.isRegularFile(gitPath)) {
      gitPath = getGitDirectoryPathFromGitFile(gitPath);
    } else if (!Files.isDirectory(gitPath)) {
      throw new GitCoreNoSuchRepositoryException("Git repository in path ${pathToRepoRoot} does not exist");
    }

    return gitPath;
  }

  private static Path getGitDirectoryPathFromGitFile(Path gitFilePath) throws GitCoreException {
    String gitFile = Try.of(() -> Files.readString(gitFilePath)).getOrElseThrow(
        e -> new GitCoreCannotReadGitFileException("Cannot access .git file under ${gitFilePath}", e));

    Matcher matcher = GIT_DIR_PATTERN.matcher(gitFile);

    if (matcher.find()) {
      String firstGroup = matcher.group(1);
      if (firstGroup == null) {
        throw new GitCoreNoSuchRepositoryException(
            "File ${gitFilePath} does not contain a valid reference to .git directory");
      }

      Path parentDirectory = gitFilePath.getParent();
      assert parentDirectory != null : "Can't get parent directory";

      return parentDirectory.resolve(firstGroup).normalize();
    }

    throw new GitCoreNoSuchRepositoryException(
        "File ${gitFilePath} does not contain a valid reference to .git directory");
  }

  @Override
  public Optional<IGitCoreLocalBranch> getCurrentBranch() throws GitCoreException {
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
      return Optional.of(new GitCoreLocalBranch(this, Repository.shortenRefName(ref.getTarget().getName())));
    }
    return Optional.empty();
  }

  @Override
  public GitCoreLocalBranch getLocalBranch(String branchName) throws GitCoreException {
    if (isBranchMissing(GitCoreLocalBranch.BRANCHES_PATH + branchName)) {
      throw new GitCoreNoSuchBranchException("Local branch '${branchName}' does not exist in this repository");
    }
    return new GitCoreLocalBranch(/* repo */ this, branchName);
  }

  @Override
  public GitCoreRemoteBranch getRemoteBranch(String branchName) throws GitCoreException {
    if (isBranchMissing(GitCoreRemoteBranch.BRANCHES_PATH + branchName)) {
      throw new GitCoreNoSuchBranchException("Remote branch '${branchName}' does not exist in this repository");
    }
    return new GitCoreRemoteBranch(/* repo */ this, branchName);
  }

  @Override
  public List<IGitCoreLocalBranch> getLocalBranches() throws GitCoreException {
    return Try.of(() -> getJgitGit().branchList().call())
        .getOrElseThrow(e -> new GitCoreException("Error while getting list of local branches", e))
        .stream()
        .filter(branch -> !branch.getName().equals(Constants.HEAD))
        .map(ref -> new GitCoreLocalBranch(/* repo */ this,
            ref.getName().replace(GitCoreLocalBranch.BRANCHES_PATH, /* replacement */ "")))
        .collect(List.collector());
  }

  @Override
  public List<IGitCoreRemoteBranch> getRemoteBranches() throws GitCoreException {
    return Try.of(() -> getJgitGit().branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call())
        .getOrElseThrow(e -> new GitCoreException("Error while getting list of remote branches", e))
        .stream()
        .filter(branch -> !branch.getName().equals(Constants.HEAD))
        .map(ref -> new GitCoreRemoteBranch(/* repo */ this,
            ref.getName().replace(GitCoreRemoteBranch.BRANCHES_PATH, /* replacement */ "")))
        .collect(List.collector());
  }

  private boolean isBranchMissing(String branchName) throws GitCoreException {
    return Try.of(() -> Optional.ofNullable(jgitRepo.resolve(branchName)))
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
    RevWalk walk = new RevWalk(jgitRepo);
    walk.setRevFilter(RevFilter.MERGE_BASE);
    try {
      walk.markStart(walk.parseCommit(toExistingObjectId(c1)));
      walk.markStart(walk.parseCommit(toExistingObjectId(c2)));
      return walk.next();
    } catch (IOException e) {
      throw new GitCoreException(e);
    }
  }

  private final java.util.Map<Tuple2<BaseGitCoreCommit, BaseGitCoreCommit>, @Nullable GitCoreCommitHash> mergeBaseCache = new java.util.HashMap<>();

  @Nullable
  private GitCoreCommitHash deriveMergeBaseIfNeeded(BaseGitCoreCommit a, BaseGitCoreCommit b) throws GitCoreException {
    var abKey = Tuple.of(a, b);
    var baKey = Tuple.of(b, a);
    if (mergeBaseCache.containsKey(abKey)) {
      return mergeBaseCache.get(abKey);
    } else if (mergeBaseCache.containsKey(baKey)) {
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
    var mergeBaseHash = deriveMergeBaseIfNeeded(presumedAncestor, presumedDescendant);
    if (mergeBaseHash == null) {
      return false;
    }
    return mergeBaseHash.equals(presumedAncestor.getHash());
  }
}
