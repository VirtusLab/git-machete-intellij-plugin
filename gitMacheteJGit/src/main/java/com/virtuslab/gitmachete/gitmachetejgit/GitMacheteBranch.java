package com.virtuslab.gitmachete.gitmachetejgit;

import com.virtuslab.branchrelationfile.api.BranchRelationFileException;
import com.virtuslab.gitcore.gitcoreapi.*;
import com.virtuslab.gitmachete.gitmacheteapi.*;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class GitMacheteBranch implements IGitMacheteBranch {
  private IGitCoreLocalBranch coreLocalBranch;

  @Getter(AccessLevel.NONE)
  private IGitMacheteRepository macheteRepository;

  @EqualsAndHashCode.Include private String name;
  Optional<IGitMacheteBranch> upstreamBranch;
  Optional<String> customAnnotation;

  @Getter(AccessLevel.NONE)
  List<IGitMacheteBranch> childBranches = new LinkedList<>();

  SyncToParentStatus syncToParentStatus = null;

  public GitMacheteBranch(
      IGitCoreLocalBranch coreLocalBranch, IGitMacheteRepository macheteRepository)
      throws GitException {
    this.coreLocalBranch = coreLocalBranch;
    this.name = this.coreLocalBranch.getName();
    this.macheteRepository = macheteRepository;
  }

  public List<IGitMacheteCommit> getCommits() throws GitException {
    if (upstreamBranch.isEmpty()) return List.of();

    Optional<IGitCoreCommit> forkPoint = coreLocalBranch.getForkPoint();
    if (forkPoint.isEmpty()) return List.of();

    return translateIGitCoreCommitsToIGitMacheteCommits(
        coreLocalBranch.getCommitsUntil(forkPoint.get()));
  }

  public List<IGitMacheteBranch> getDownstreamBranches() {
    return childBranches;
  }

  public SyncToOriginStatus getSyncToOriginStatus() throws GitException {
    Optional<IGitCoreBranchTrackingStatus> ts = coreLocalBranch.getRemoteTrackingStatus();
    if (ts.isEmpty()) return SyncToOriginStatus.Untracked;

    if (ts.get().getAhead() > 0 && ts.get().getBehind() > 0) return SyncToOriginStatus.Diverged;
    else if (ts.get().getAhead() > 0) return SyncToOriginStatus.Ahead;
    else if (ts.get().getBehind() > 0) return SyncToOriginStatus.Behind;
    else return SyncToOriginStatus.InSync;
  }

  @Override
  public void slideOut() throws GitMacheteException {
    var macheteFile = macheteRepository.getBranchRelationFile();
    var macheteFileBranch = macheteFile.findBranchByName(getName());
    if (macheteFileBranch.isEmpty())
      throw new GitMacheteException(
          MessageFormat.format(
              "Branch {0} was not found in machete file, so can not be slided out", getName()));

    try {
      macheteFileBranch.get().slideOut();
      macheteFile.saveToFile();
    } catch (BranchRelationFileException | IOException e) {
      throw new GitMacheteException(
          MessageFormat.format(
              "Error occurred while sliding out branch {0}: {1}", getName(), e.getMessage()),
          e);
    }
  }

  public SyncToParentStatus getSyncToParentStatus() throws GitException {
    if (upstreamBranch.isEmpty()) return SyncToParentStatus.InSync;

    IGitCoreLocalBranch parentBranch = upstreamBranch.get().getCoreLocalBranch();

    if (coreLocalBranch.getPointedCommit().equals(parentBranch.getPointedCommit())) {
      if (coreLocalBranch.hasJustBeenCreated()) return SyncToParentStatus.InSync;
      else return SyncToParentStatus.Merged;
    } else {
      Optional<IGitCoreCommit> forkPoint = coreLocalBranch.getForkPoint();
      boolean isParentAncestorOfChild =
          parentBranch.getPointedCommit().isAncestorOf(coreLocalBranch.getPointedCommit());

      if (isParentAncestorOfChild) {
        if (forkPoint.isEmpty() || !forkPoint.get().equals(parentBranch.getPointedCommit()))
          return SyncToParentStatus.InSyncButForkPointOff;
        else return SyncToParentStatus.InSync;
      } else {
        boolean isChildAncestorOfParent =
            coreLocalBranch.getPointedCommit().isAncestorOf(parentBranch.getPointedCommit());

        if (isChildAncestorOfParent) return SyncToParentStatus.Merged;
        else return SyncToParentStatus.OutOfSync;
      }
    }
  }

  private List<IGitMacheteCommit> translateIGitCoreCommitsToIGitMacheteCommits(
      List<IGitCoreCommit> list) throws GitException {
    var l = new LinkedList<IGitMacheteCommit>();

    for (var c : list) {
      l.add(new GitMacheteCommit(c.getMessage()));
    }

    return l;
  }
}
