package com.virtuslab.gitmachete.gitmachetejgit;

import com.virtuslab.gitcore.gitcoreapi.GitException;
import com.virtuslab.gitcore.gitcoreapi.IGitCoreBranchTrackingStatus;
import com.virtuslab.gitcore.gitcoreapi.IGitCoreCommit;
import com.virtuslab.gitcore.gitcoreapi.IGitCoreLocalBranch;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteCommit;
import com.virtuslab.gitmachete.gitmacheteapi.IGitRebaseParameters;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToParentStatus;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GitMacheteBranch implements IGitMacheteBranch {
  @Getter private IGitCoreLocalBranch coreLocalBranch;

  @EqualsAndHashCode.Include @Getter private String name;
  @Getter private Optional<IGitMacheteBranch> upstreamBranch;
  @Getter private Optional<String> customAnnotation;

  private List<IGitMacheteBranch> childBranches = new LinkedList<>();

  SyncToParentStatus syncToParentStatus = null;

  public GitMacheteBranch(IGitCoreLocalBranch coreLocalBranch) throws GitException {
    this.coreLocalBranch = coreLocalBranch;
    this.name = this.coreLocalBranch.getName();
  }

  public GitMacheteBranch(IGitCoreLocalBranch coreLocalBranch, Optional<String> customAnnotation)
      throws GitException {
    this.coreLocalBranch = coreLocalBranch;
    this.name = this.coreLocalBranch.getName();
    this.customAnnotation = customAnnotation;
  }

  public GitMacheteBranch(
      IGitCoreLocalBranch coreLocalBranch,
      Optional<String> customAnnotation,
      Optional<IGitMacheteBranch> upstreamBranch)
      throws GitException {
    this.coreLocalBranch = coreLocalBranch;
    this.name = this.coreLocalBranch.getName();
    this.customAnnotation = customAnnotation;
    this.upstreamBranch = upstreamBranch;
  }

  public List<IGitMacheteCommit> computeCommits() throws GitException {
    if (upstreamBranch.isEmpty()) return List.of();

    Optional<IGitCoreCommit> forkPoint = coreLocalBranch.getForkPoint();
    if (forkPoint.isEmpty()) {
      return List.of();
    }

    return translateIGitCoreCommitsToIGitMacheteCommits(
        coreLocalBranch.getCommitsUntil(forkPoint.get()));
  }

  public List<IGitMacheteBranch> getDownstreamBranches() {
    return childBranches;
  }

  public SyncToOriginStatus computeSyncToOriginStatus() throws GitException {
    Optional<IGitCoreBranchTrackingStatus> ts = coreLocalBranch.getRemoteTrackingStatus();
    if (ts.isEmpty()) {
      return SyncToOriginStatus.Untracked;
    }

    if (ts.get().getAhead() > 0 && ts.get().getBehind() > 0) return SyncToOriginStatus.Diverged;
    else if (ts.get().getAhead() > 0) return SyncToOriginStatus.Ahead;
    else if (ts.get().getBehind() > 0) return SyncToOriginStatus.Behind;
    else return SyncToOriginStatus.InSync;
  }

  @Override
  public IGitRebaseParameters getRebaseParameters() throws GitMacheteException, GitException {
    if (getUpstreamBranch().isEmpty())
      throw new GitMacheteException(
          MessageFormat.format("Can not get rebase parameters for root branch \"{0}\"", getName()));

    var forkPoint = coreLocalBranch.getForkPoint();
    if (forkPoint.isEmpty())
      throw new GitMacheteException(
          MessageFormat.format("Can not find fork point for branch \"{0}\"", getName()));

    return new GitRebaseParameters(
        getName(), getUpstreamBranch().get().getName(), forkPoint.get().getHash().getHashString());
  }

  public SyncToParentStatus computeSyncToParentStatus() throws GitException {
    if (upstreamBranch.isEmpty()) {
      return SyncToParentStatus.InSync;
    }

    IGitCoreLocalBranch parentBranch = upstreamBranch.get().getCoreLocalBranch();

    if (coreLocalBranch.getPointedCommit().equals(parentBranch.getPointedCommit())) {
      if (coreLocalBranch.hasJustBeenCreated()) {
        return SyncToParentStatus.InSync;
      } else {
        return SyncToParentStatus.Merged;
      }
    } else {
      Optional<IGitCoreCommit> forkPoint = coreLocalBranch.getForkPoint();
      boolean isParentAncestorOfChild =
          parentBranch.getPointedCommit().isAncestorOf(coreLocalBranch.getPointedCommit());

      if (isParentAncestorOfChild) {
        if (forkPoint.isEmpty() || !forkPoint.get().equals(parentBranch.getPointedCommit())) {
          return SyncToParentStatus.InSyncButForkPointOff;
        } else {
          return SyncToParentStatus.InSync;
        }
      } else {
        boolean isChildAncestorOfParent =
            coreLocalBranch.getPointedCommit().isAncestorOf(parentBranch.getPointedCommit());

        if (isChildAncestorOfParent) {
          return SyncToParentStatus.Merged;
        } else {
          return SyncToParentStatus.OutOfSync;
        }
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
