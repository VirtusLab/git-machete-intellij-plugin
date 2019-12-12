package com.virtuslab.gitmachete.gitmachetejgit;

import com.virtuslab.gitcore.gitcoreapi.*;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteCommit;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToParentStatus;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class GitMacheteBranch implements IGitMacheteBranch {
  private IGitCoreLocalBranch coreLocalBranch;
  private String name;
  List<IGitMacheteCommit> commits = new LinkedList<>();
  Optional<IGitMacheteBranch> upstreamBranch;
  Optional<String> customAnnotation;

  @Getter(AccessLevel.NONE)
  List<IGitMacheteBranch> childBranches = new LinkedList<>();

  SyncToParentStatus syncToParentStatus = null;

  public GitMacheteBranch(IGitCoreLocalBranch coreLocalBranch, String name) {
    this.coreLocalBranch = coreLocalBranch;
    this.name = name;
  }

  public List<IGitMacheteBranch> getBranches() {
    return childBranches;
  }

  public SyncToOriginStatus getSyncToOriginStatus() throws GitException {
    Optional<IGitCoreBranchTrackingStatus> ts = coreLocalBranch.getTrackingStatus();
    if (ts.isEmpty()) return SyncToOriginStatus.Untracked;

    if (ts.get().getAhead() > 0 && ts.get().getBehind() > 0) return SyncToOriginStatus.Diverged;
    else if (ts.get().getAhead() > 0) return SyncToOriginStatus.Ahead;
    else if (ts.get().getBehind() > 0) return SyncToOriginStatus.Behind;
    else return SyncToOriginStatus.InSync;
  }

  public SyncToParentStatus getSyncToParentStatus() throws GitException {
    if (upstreamBranch.isEmpty()) return SyncToParentStatus.InSync;

    IGitCoreLocalBranch parentBranch = upstreamBranch.get().getCoreLocalBranch();

    if (coreLocalBranch.getPointedCommit().equals(parentBranch.getPointedCommit())) {
      if (coreLocalBranch.hasJustBeenCreated()) return SyncToParentStatus.InSync;
      else return SyncToParentStatus.Merged;
    } else {
      Optional<IGitCoreCommit> forkPoint = coreLocalBranch.getForkPoint(parentBranch);
      boolean isParentAncestorOfChild =
          parentBranch.getPointedCommit().isAncestorOf(coreLocalBranch.getPointedCommit());

      if (isParentAncestorOfChild) {
        if (forkPoint.isEmpty() || !forkPoint.get().equals(parentBranch.getPointedCommit()))
          return SyncToParentStatus.NotADirectDescendant;
        else return SyncToParentStatus.InSync;
      } else {
        boolean isChildAncestorOfParent =
            coreLocalBranch.getPointedCommit().isAncestorOf(parentBranch.getPointedCommit());

        if (isChildAncestorOfParent) return SyncToParentStatus.Merged;
        else return SyncToParentStatus.OutOfSync;
      }
    }
  }
}
