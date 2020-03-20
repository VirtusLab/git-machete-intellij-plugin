package com.virtuslab.gitmachete.backend.impl;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.Data;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IAncestorityChecker;
import com.virtuslab.gitcore.api.IGitCoreBranchTrackingStatus;
import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreCommitHash;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.IGitMergeParameters;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;
import com.virtuslab.gitmachete.backend.api.SyncToOriginStatus;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;

@Data
public class GitMacheteBranch implements IGitMacheteBranch {
  private final IGitCoreLocalBranch coreLocalBranch;
  @Nullable
  private final IGitMacheteBranch upstreamBranch;
  @Nullable
  private final String customAnnotation;
  private final IAncestorityChecker ancestorityChecker;
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

  public List<IGitMacheteCommit> computeCommits() throws GitMacheteException {
    if (upstreamBranch == null) {
      return Collections.emptyList();
    }

    try {
      Optional<IGitCoreCommit> forkPoint = coreLocalBranch.computeForkPoint();
      if (!forkPoint.isPresent()) {
        return Collections.emptyList();
      }

      // translate IGitCoreCommit list to IGitMacheteCommit list
      return coreLocalBranch.computeCommitsUntil(forkPoint.get())
          .map(GitMacheteCommit::new)
          .collect(Collectors.toList());

    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

  @Override
  public IGitMacheteCommit getPointedCommit() throws GitMacheteException {
    try {
      return new GitMacheteCommit(coreLocalBranch.getPointedCommit());
    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

  public List<IGitMacheteBranch> getDownstreamBranches() {
    return childBranches;
  }

  public SyncToOriginStatus computeSyncToOriginStatus() throws GitMacheteException {
    try {
      Optional<IGitCoreBranchTrackingStatus> tsOption = coreLocalBranch.computeRemoteTrackingStatus();
      if (!tsOption.isPresent()) {
        return SyncToOriginStatus.Untracked;
      }

      var ts = tsOption.get();
      if (ts.getAhead() > 0 && ts.getBehind() > 0) {
        return SyncToOriginStatus.Diverged;
      } else if (ts.getAhead() > 0) {
        return SyncToOriginStatus.Ahead;
      } else if (ts.getBehind() > 0) {
        return SyncToOriginStatus.Behind;
      } else {
        return SyncToOriginStatus.InSync;
      }

    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

  @Override
  public IGitRebaseParameters computeRebaseParameters() throws GitMacheteException {
    try {
      var upstreamBranch = getUpstreamBranch();
      if (!upstreamBranch.isPresent()) {
        throw new GitMacheteException(
            MessageFormat.format("Cannot get rebase parameters for root branch \"{0}\"", getName()));
      }

      var forkPoint = coreLocalBranch.computeForkPoint();
      if (!forkPoint.isPresent()) {
        throw new GitMacheteException(MessageFormat.format("Cannot find fork point for branch \"{0}\"", getName()));
      }

      return new GitRebaseParameters(/* currentBranch */ this, upstreamBranch.get().getPointedCommit(),
          new GitMacheteCommit(forkPoint.get()));

    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

  public SyncToParentStatus computeSyncToParentStatus() throws GitMacheteException {
    try {
      if (upstreamBranch == null) {
        return SyncToParentStatus.InSync;
      }

      IGitCoreCommitHash upstreamBranchPointedCommitHash = upstreamBranch.getPointedCommit()::getHash;
      IGitCoreCommitHash myPointedCommitHash = coreLocalBranch.getPointedCommit().getHash();

      if (myPointedCommitHash.equals(upstreamBranchPointedCommitHash)) {
        if (coreLocalBranch.hasJustBeenCreated()) {
          return SyncToParentStatus.InSync;
        } else {
          return SyncToParentStatus.Merged;
        }
      } else {
        var isParentAncestorOfChild = ancestorityChecker.isAncestor(/* presumedAncestor */ myPointedCommitHash,
            /* presumedDescendant */ upstreamBranchPointedCommitHash);

        if (isParentAncestorOfChild) {
          Optional<IGitCoreCommit> forkPoint = coreLocalBranch.computeForkPoint();
          if (!forkPoint.isPresent()
              || !forkPoint.get().getHash().getHashString().equals(upstreamBranchPointedCommitHash.getHashString())) {
            return SyncToParentStatus.InSyncButForkPointOff;
          } else {
            return SyncToParentStatus.InSync;
          }
        } else {
          var isChildAncestorOfParent = ancestorityChecker.isAncestor(
              /* presumedAncestor */ upstreamBranchPointedCommitHash, /* presumedDescendant */ myPointedCommitHash);

          if (isChildAncestorOfParent) {
            return SyncToParentStatus.Merged;
          } else {
            return SyncToParentStatus.OutOfSync;
          }
        }
      }

    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

  @Override
  public IGitMergeParameters getMergeParameters() throws GitMacheteException {
    var upstreamBranch = getUpstreamBranch();
    if (!upstreamBranch.isPresent()) {
      throw new GitMacheteException(
          MessageFormat.format("Cannot get merge parameters for root branch \"{0}\"", getName()));
    }

    return new GitMergeParameters(/* currentBranch */ this, upstreamBranch.get());
  }
}
