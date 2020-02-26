package com.virtuslab.gitmachete.gitmachetejgit;

import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreBranchTrackingStatus;
import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteCommit;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMergeParameters;
import com.virtuslab.gitmachete.gitmacheteapi.IGitRebaseParameters;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToParentStatus;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import lombok.Data;

@Data
public class GitMacheteBranch implements IGitMacheteBranch {
  private final IGitCoreLocalBranch coreLocalBranch;
  private final IGitMacheteBranch upstreamBranch;
  private final String customAnnotation;
  private final List<IGitMacheteBranch> childBranches = new LinkedList<>();

  @Override
  public Optional<String> getCustomAnnotation() {
    return Optional.ofNullable(customAnnotation);
  }

  @Override
  public String getName() {
    return coreLocalBranch.getName();
  }

  @Override
  public Optional<IGitMacheteBranch> getUpstreamBranch() {
    return Optional.ofNullable(upstreamBranch);
  }

  public List<IGitMacheteCommit> computeCommits() throws GitCoreException {
    if (upstreamBranch == null) {
      return List.of();
    }

    Optional<IGitCoreCommit> forkPoint = coreLocalBranch.computeForkPoint();
    if (forkPoint.isEmpty()) {
      return List.of();
    }

    return translateIGitCoreCommitListToIGitMacheteCommitList(
        coreLocalBranch.computeCommitsUntil(forkPoint.get()));
  }

  @Override
  public IGitMacheteCommit getPointedCommit() throws GitCoreException {
    return new GitMacheteCommit(coreLocalBranch.getPointedCommit());
  }

  public List<IGitMacheteBranch> getDownstreamBranches() {
    return childBranches;
  }

  public SyncToOriginStatus computeSyncToOriginStatus() throws GitCoreException {
    Optional<IGitCoreBranchTrackingStatus> ts = coreLocalBranch.computeRemoteTrackingStatus();
    if (ts.isEmpty()) {
      return SyncToOriginStatus.Untracked;
    }

    if (ts.get().getAhead() > 0 && ts.get().getBehind() > 0) return SyncToOriginStatus.Diverged;
    else if (ts.get().getAhead() > 0) return SyncToOriginStatus.Ahead;
    else if (ts.get().getBehind() > 0) return SyncToOriginStatus.Behind;
    else return SyncToOriginStatus.InSync;
  }

  @Override
  public IGitRebaseParameters computeRebaseParameters()
      throws GitMacheteException, GitCoreException {
    if (getUpstreamBranch().isEmpty()) {
      throw new GitMacheteException(
          MessageFormat.format("Can not get rebase parameters for root branch \"{0}\"", getName()));
    }

    var forkPoint = coreLocalBranch.computeForkPoint();
    if (forkPoint.isEmpty()) {
      throw new GitMacheteException(
          MessageFormat.format("Can not find fork point for branch \"{0}\"", getName()));
    }

    return new GitRebaseParameters(
        /*currentBranch*/ this,
        getUpstreamBranch().get().getPointedCommit(),
        new GitMacheteCommit(forkPoint.get()));
  }

  public SyncToParentStatus computeSyncToParentStatus() throws GitCoreException {
    if (upstreamBranch == null) {
      return SyncToParentStatus.InSync;
    }

    IGitCoreLocalBranch parentBranch = upstreamBranch.getCoreLocalBranch();

    if (coreLocalBranch.getPointedCommit().equals(parentBranch.getPointedCommit())) {
      if (coreLocalBranch.hasJustBeenCreated()) {
        return SyncToParentStatus.InSync;
      } else {
        return SyncToParentStatus.Merged;
      }
    } else {
      Optional<IGitCoreCommit> forkPoint = coreLocalBranch.computeForkPoint();
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

  private List<IGitMacheteCommit> translateIGitCoreCommitListToIGitMacheteCommitList(
      List<IGitCoreCommit> list) throws GitCoreException {
    var l = new LinkedList<IGitMacheteCommit>();

    for (var c : list) {
      l.add(new GitMacheteCommit(c));
    }

    return l;
  }

  @Override
  public IGitMergeParameters getMergeParameters() throws GitMacheteException {
    if (getUpstreamBranch().isEmpty()) {
      throw new GitMacheteException(
          MessageFormat.format("Can not get merge parameters for root branch \"{0}\"", getName()));
    }

    return new GitMergeParameters(/*currentBranch*/ this, getUpstreamBranch().get());
  }
}
