package com.virtuslab.gitcore.impl.jgit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import io.vavr.collection.Iterator;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.Getter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;

import com.virtuslab.gitcore.api.BaseGitCoreCommit;
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
  private final Path gitFolderPath;

  @Getter(AccessLevel.NONE)
  private static final Pattern GIT_DIR_PATTERN = Pattern.compile("^gitdir:\\s*(.*)");

  @Inject
  public GitCoreRepository(@Assisted Path repositoryPath) throws IOException, GitCoreNoSuchRepositoryException {
    this.repositoryPath = repositoryPath;
    Path gitPath = repositoryPath.resolve(".git");

    if (Files.isDirectory(gitPath)) {
      this.gitFolderPath = gitPath;
      // .git file can be also in worktrees not only in submodules
    } else if (Files.isRegularFile(gitPath)) {
      this.gitFolderPath = getGitFolderPathFromGitFile(gitPath, repositoryPath);
    } else {
      throw new GitCoreNoSuchRepositoryException(
          String.format("Repository in path \"%s\" does not exists", repositoryPath));
    }

    jgitRepo = new FileRepository(this.gitFolderPath.toString());
    jgitGit = new Git(jgitRepo);
  }

  private static Path getGitFolderPathFromGitFile(Path gitFilePath, Path repositoryPath)
      throws IOException, GitCoreNoSuchRepositoryException {
    String gitFile = Files.readString(gitFilePath);
    Matcher matcher = GIT_DIR_PATTERN.matcher(gitFile);

    if (matcher.find()) {
      String firstGroup = matcher.group(1);
      if (firstGroup == null) {
        throw new GitCoreNoSuchRepositoryException(
            String.format("Path \"%s\" does not contain any submodule", repositoryPath));
      }

      Path parentDirectory = gitFilePath.getParent();
      assert parentDirectory != null : "Can't get parent directory";

      return parentDirectory.resolve(firstGroup).normalize();
    }

    throw new GitCoreNoSuchRepositoryException(
        String.format("Path \"%s\" does not contain any submodule", repositoryPath));
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
      throw new GitCoreNoSuchBranchException(
          String.format("Local branch \"%s\" does not exist in this repository", branchName));
    }
    return new GitCoreLocalBranch(/* repo */ this, branchName);
  }

  @Override
  public GitCoreRemoteBranch getRemoteBranch(String branchName) throws GitCoreException {
    if (isBranchMissing(GitCoreRemoteBranch.BRANCHES_PATH + branchName)) {
      throw new GitCoreNoSuchBranchException(
          String.format("Remote branch \"%s\" does not exist in this repository", branchName));
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

  private boolean isBranchMissing(String path) throws GitCoreException {
    return Try.of(() -> Option.of(jgitRepo.resolve(path)))
        .getOrElseThrow(e -> new GitCoreException(e))
        .map(Objects::isNull)
        .get();
  }

  @Override
  public boolean isAncestor(BaseGitCoreCommit presumedAncestor, BaseGitCoreCommit presumedDescendant)
      throws GitCoreException {
    RevWalk walk = new RevWalk(jgitRepo);
    walk.sort(RevSort.TOPO);
    try {
      ObjectId descendantObjectId = jgitRepo.resolve(presumedDescendant.getHash().getHashString());
      assert descendantObjectId != null : "Cannot find descendant";

      ObjectId ancestorObjectId = jgitRepo.resolve(presumedAncestor.getHash().getHashString());
      assert ancestorObjectId != null : "Cannot find ancestor";

      walk.markStart(walk.parseCommit(descendantObjectId));

      return Iterator.ofAll(walk).find(revCommit -> revCommit.getId().equals(ancestorObjectId)).isDefined();

    } catch (IOException e) {
      throw new GitCoreException(e);
    }
  }
}
