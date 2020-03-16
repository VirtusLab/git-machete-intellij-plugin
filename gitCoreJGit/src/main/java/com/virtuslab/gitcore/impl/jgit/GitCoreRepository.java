package com.virtuslab.gitcore.impl.jgit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.vavr.collection.List;
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
import org.eclipse.jgit.submodule.SubmoduleWalk;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.GitCoreNoSuchBranchException;
import com.virtuslab.gitcore.api.GitCoreNoSuchRepositoryException;
import com.virtuslab.gitcore.api.IGitCoreCommitHash;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranch;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitcore.api.IGitCoreSubmoduleEntry;

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
    } else if (Files.isRegularFile(gitPath)) {
      this.gitFolderPath = getGitFolderPathFromGitFile(gitPath);
    } else {
      throw new GitCoreNoSuchRepositoryException(
          MessageFormat.format("Repository in path \"{0}\" does not exists", repositoryPath));
    }

    jgitRepo = new FileRepository(this.gitFolderPath.toString());
    jgitGit = new Git(jgitRepo);
  }

  private Path getGitFolderPathFromGitFile(Path gitFilePath) throws IOException, GitCoreNoSuchRepositoryException {
    String gitFile = Files.readString(gitFilePath);
    Matcher matcher = GIT_DIR_PATTERN.matcher(gitFile);
    if (matcher.find()) {
      return gitFilePath.getParent().resolve(matcher.group(1)).normalize();
    }

    throw new GitCoreNoSuchRepositoryException(
        MessageFormat.format("Path \"{0}\" does not contain any submodule", this.repositoryPath));
  }

  @Override
  public Optional<IGitCoreLocalBranch> getCurrentBranch() throws GitCoreException {
    Ref ref = Try.of(() -> jgitRepo.getRefDatabase().findRef(Constants.HEAD))
        .getOrElseThrow(e -> new GitCoreException("Cannot get current branch", e));

    if (ref == null) {
      throw new GitCoreException("Error occur while getting current branch ref");
    }

    if (ref.isSymbolic()) {
      return Optional.of(new GitCoreLocalBranch(this, Repository.shortenRefName(ref.getTarget().getName())));
    }
    return Optional.empty();
  }

  @Override
  public GitCoreLocalBranch getLocalBranch(String branchName) throws GitCoreException {
    if (isBranchMissing(GitCoreLocalBranch.branchesPath + branchName)) {
      throw new GitCoreNoSuchBranchException(
          MessageFormat.format("Local branch \"{0}\" does not exist in this repository", branchName));
    }
    return new GitCoreLocalBranch(/* repo */ this, branchName);
  }

  @Override
  public GitCoreRemoteBranch getRemoteBranch(String branchName) throws GitCoreException {
    if (isBranchMissing(GitCoreRemoteBranch.branchesPath + branchName)) {
      throw new GitCoreNoSuchBranchException(
          MessageFormat.format("Remote branch \"{0}\" does not exist in this repository", branchName));
    }
    return new GitCoreRemoteBranch(/* repo */ this, branchName);
  }

  @Override
  public List<IGitCoreLocalBranch> getLocalBranches() throws GitCoreException {
    return Try.of(() -> getJgitGit().branchList().call()
        .stream()
        .map(ref -> (IGitCoreLocalBranch) new GitCoreLocalBranch(/* repo */ this,
            ref.getName().replace(GitCoreLocalBranch.branchesPath, /* replacement */ "")))
        .collect(List.collector()))
        .getOrElseThrow(e -> new GitCoreException("Error while getting list of local branches", e));
  }

  @Override
  public List<IGitCoreRemoteBranch> getRemoteBranches() throws GitCoreException {
    return Try.of(() -> getJgitGit().branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call()
        .stream()
        .map(ref -> (IGitCoreRemoteBranch) new GitCoreRemoteBranch(/* repo */ this,
            ref.getName().replace(GitCoreRemoteBranch.branchesPath, "")))
        .collect(List.collector()))
        .getOrElseThrow(e -> new GitCoreException("Error while getting list of remote branches", e));
  }

  @Override
  public List<IGitCoreSubmoduleEntry> getSubmodules() throws GitCoreException {
    SubmoduleWalk sWalk;
    try {
      sWalk = SubmoduleWalk.forIndex(this.jgitRepo);
    } catch (IOException e) {
      throw new GitCoreException("Error while initializing submodule walk", e);
    }

    return Try.of(() -> {

      List<IGitCoreSubmoduleEntry> submodules = List.empty();
      while (sWalk.next()) {
        submodules = submodules.append(new GitCoreSubmoduleEntry(sWalk.getDirectory().toPath(), sWalk.getModuleName()));
      }
      return submodules;

    }).getOrElseThrow(e -> new GitCoreException("Error while fetching next submodule", e));
  }

  private boolean isBranchMissing(String path) throws GitCoreException {
    return Try.of(() -> jgitRepo.resolve(path))
        .map(Objects::isNull)
        .getOrElseThrow(e -> new GitCoreException(e));
  }

  @Override
  public boolean isAncestor(IGitCoreCommitHash presumedAncestor, IGitCoreCommitHash presumedDescendant)
      throws GitCoreException {
    RevWalk walk = new RevWalk(jgitRepo);
    walk.sort(RevSort.TOPO);
    try {
      ObjectId objectId = jgitRepo.resolve(presumedDescendant.getHashString());
      walk.markStart(walk.parseCommit(jgitRepo.resolve(presumedAncestor.getHashString())));

      for (var c : walk) {
        if (c.getId().equals(objectId)) {
          return true;
        }
      }
    } catch (IOException e) {
      throw new GitCoreException(e);
    }

    return false;
  }
}
