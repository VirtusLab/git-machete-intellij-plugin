package com.virtuslab.gitmachete.frontend.ui.impl.table;

import java.time.Instant;

import io.vavr.NotImplementedError;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.NotImplementedException;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.ArrayLen;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteForkPointCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRemoteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitMergeParameters;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;
import com.virtuslab.gitmachete.backend.api.OngoingRepositoryOperation;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.backend.api.hook.IExecutionResult;

public class DemoGitMacheteRepositorySnapshot implements IGitMacheteRepositorySnapshot {

  private final List<IGitMacheteRootBranch> roots;

  public DemoGitMacheteRepositorySnapshot() {
    var nullPointedCommit = new Commit("");
    var fp = new FpCommit("Fork point commit");
    NonRoot[] nonRoots = {
        new NonRoot(/* name */ "allow-ownership-link",
            /* fullName */ "refs/heads/allow-ownership-link",
            /* customAnnotation */ "# Gray edge: branch is merged to its parent branch",
            nullPointedCommit,
            /* fork point */ null,
            /* childBranches */ List.empty(),
            /* commits */ List.empty(),
            SyncToParentStatus.MergedToParent),
        new NonRoot(/* name */ "build-chain",
            /* fullName */ "refs/heads/build-chain",
            /* customAnnotation */ "# Green edge: branch is in sync with its parent branch",
            nullPointedCommit,
            /* fork point */ null,
            /* childBranches */ List.empty(),
            /* commits */ List.of(new Commit("Second commit of build-chain"),
                new Commit("First commit of build-chain")),
            SyncToParentStatus.InSync),
        new NonRoot(/* name */ "call-ws",
            /* fullName */ "refs/heads/call-ws",
            /* customAnnotation */ "# Yellow edge: Branch is in sync with its parent branch but the fork point is NOT equal to parent branch",
            nullPointedCommit,
            /* fork point */ fp,
            /* childBranches */ List.empty(),
            /* commits */ List.of(fp),
            SyncToParentStatus.InSyncButForkPointOff),
        new NonRoot(/* name */ "remove-ff",
            /* fullName */ "refs/heads/remove-ff",
            /* customAnnotation */ "# Red edge: branch is out of sync to its parent branch",
            nullPointedCommit,
            /* fork point */ null,
            /* childBranches */ List.empty(),
            /* commits */ List.of(new Commit("Some commit")),
            SyncToParentStatus.OutOfSync)
    };

    var root = new Root(/* name */ "develop",
        /* fullName */ "refs/heads/develop",
        /* customAnnotation */ "# This is a root branch, the underline indicates that it is the currently checked out branch",
        nullPointedCommit,
        /* childBranches */ List.of(nonRoots));

    for (var nr : nonRoots) {
      nr.setParentBranch(root);
    }

    this.roots = List.of(root);
  }

  static SyncToRemoteStatus getSTRSofRelation(SyncToRemoteStatus.Relation relation) {
    return SyncToRemoteStatus.of(relation, "origin");
  }

  @Override
  public Option<IBranchLayout> getBranchLayout() {
    throw new NotImplementedError();
  }

  @Override
  public List<IGitMacheteRootBranch> getRootBranches() {
    return roots;
  }

  @Override
  public Option<IGitMacheteBranch> getCurrentBranchIfManaged() {
    return Option.narrow(roots.headOption());
  }

  @Override
  public List<IGitMacheteBranch> getManagedBranches() {
    throw new NotImplementedError();
  }

  @Override
  public Option<IGitMacheteBranch> getManagedBranchByName(String branchName) {
    throw new NotImplementedError();
  }

  @Override
  public List<String> getSkippedBranchNames() {
    return List.empty();
  }

  @Override
  public Option<IExecutionResult> executeMachetePreRebaseHookIfPresent(IGitRebaseParameters gitRebaseParameters) {
    return Option.none();
  }

  @Override
  public OngoingRepositoryOperation getOngoingRepositoryOperation() {
    return OngoingRepositoryOperation.NO_OPERATION;
  }

  @AllArgsConstructor
  private static class Commit implements IGitMacheteCommit {

    private final String msg;

    @Override
    public String getShortMessage() {
      return msg;
    }

    @Override
    public @ArrayLen(40) String getHash() {
      throw new NotImplementedError();
    }

    @Override
    public @ArrayLen(7) String getShortHash() {
      throw new NotImplementedException();
    }

    @Override
    public Instant getCommitTime() {
      throw new NotImplementedError();
    }
  }

  private static final class FpCommit extends Commit implements IGitMacheteForkPointCommit {
    FpCommit(String msg) {
      super(msg);
    }

    @Override
    public @ArrayLen(7) String getShortHash() {
      return "1461ce9";
    }

    @Override
    public List<String> getBranchesContainingInReflog() {
      return List.of("some-other-branch");
    }

    @Override
    public boolean isOverridden() {
      return false;
    }
  }

  @Getter
  @AllArgsConstructor
  private static final class Root implements IGitMacheteRootBranch {
    private final String name;
    private final String fullName;
    private final String customAnnotation;
    private final Commit pointedCommit;
    private final SyncToRemoteStatus syncToRemoteStatus = getSTRSofRelation(SyncToRemoteStatus.Relation.InSyncToRemote);
    private final List<IGitMacheteNonRootBranch> childBranches;

    @Override
    public Option<String> getCustomAnnotation() {
      return Option.of(customAnnotation);
    }

    @Override
    public Option<String> getStatusHookOutput() {
      return Option.none();
    }

    @Override
    public Option<IGitMacheteRemoteBranch> getRemoteTrackingBranch() {
      return Option.none();
    }
  }

  @Getter
  @RequiredArgsConstructor
  private static final class NonRoot implements IGitMacheteNonRootBranch {
    private final String name;
    private final String fullName;
    private final String customAnnotation;
    private final Commit pointedCommit;
    private final @Nullable IGitMacheteForkPointCommit forkPoint;
    private final SyncToRemoteStatus syncToRemoteStatus = getSTRSofRelation(SyncToRemoteStatus.Relation.InSyncToRemote);
    private final List<IGitMacheteNonRootBranch> childBranches;

    private final List<IGitMacheteCommit> commits;
    @MonotonicNonNull
    private IGitMacheteBranch parentBranch = null;
    private final SyncToParentStatus syncToParentStatus;

    @Override
    public IGitMacheteBranch getParentBranch() {
      assert parentBranch != null : "parentBranch hasn't been set yet";
      return parentBranch;
    }

    void setParentBranch(IGitMacheteBranch givenParentBranch) {
      assert parentBranch == null : "parentBranch has already been set";
      parentBranch = givenParentBranch;
    }

    @Override
    public Option<String> getCustomAnnotation() {
      return Option.of(customAnnotation);
    }

    @Override
    public Option<String> getStatusHookOutput() {
      return Option.none();
    }

    @Override
    public Option<IGitMacheteForkPointCommit> getForkPoint() {
      return Option.of(forkPoint);
    }

    @Override
    public IGitRebaseParameters getParametersForRebaseOntoParent() {
      throw new NotImplementedError();
    }

    @Override
    public IGitMergeParameters getParametersForMergeIntoParent() {
      throw new NotImplementedError();
    }

    @Override
    public Option<IGitMacheteRemoteBranch> getRemoteTrackingBranch() {
      return Option.none();
    }
  }
}
