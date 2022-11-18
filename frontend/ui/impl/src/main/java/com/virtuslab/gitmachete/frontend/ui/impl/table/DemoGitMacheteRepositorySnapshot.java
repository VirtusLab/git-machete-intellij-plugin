package com.virtuslab.gitmachete.frontend.ui.impl.table;

import java.time.Instant;

import io.vavr.NotImplementedError;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import io.vavr.collection.TreeSet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.ArrayLen;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.gitmachete.backend.api.IBranchReference;
import com.virtuslab.gitmachete.backend.api.ICommitOfManagedBranch;
import com.virtuslab.gitmachete.backend.api.IForkPointCommitOfManagedBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;
import com.virtuslab.gitmachete.backend.api.ILocalBranchReference;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.IRemoteTrackingBranchReference;
import com.virtuslab.gitmachete.backend.api.IRootManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.OngoingRepositoryOperationType;
import com.virtuslab.gitmachete.backend.api.RelationToRemote;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.backend.api.hooks.IExecutionResult;

public class DemoGitMacheteRepositorySnapshot implements IGitMacheteRepositorySnapshot {

  private final List<IRootManagedBranchSnapshot> roots;

  public DemoGitMacheteRepositorySnapshot() {
    val nullPointedCommit = new Commit("");
    val fp = new FpCommit("Fork point commit");
    NonRoot[] nonRoots = {
        new NonRoot(/* name */ "allow-ownership-link",
            /* fullName */ "refs/heads/allow-ownership-link",
            /* customAnnotation */ "# Gray edge: branch is merged to its parent branch",
            nullPointedCommit,
            /* forkPoint */ null,
            /* childBranches */ List.empty(),
            /* commits */ List.empty(),
            SyncToParentStatus.MergedToParent),
        new NonRoot(/* name */ "build-chain",
            /* fullName */ "refs/heads/build-chain",
            /* customAnnotation */ "# Green edge: branch is in sync with its parent branch",
            nullPointedCommit,
            /* forkPoint */ null,
            /* childBranches */ List.empty(),
            /* commits */ List.of(new Commit("Second commit of build-chain"),
                new Commit("First commit of build-chain")),
            SyncToParentStatus.InSync),
        new NonRoot(/* name */ "call-ws",
            /* fullName */ "refs/heads/call-ws",
            /* customAnnotation */ "# Yellow edge: Branch is in sync with its parent branch but the fork point is NOT equal to parent branch",
            nullPointedCommit,
            /* forkPoint */ fp,
            /* childBranches */ List.empty(),
            /* commits */ List.of(fp),
            SyncToParentStatus.InSyncButForkPointOff),
        new NonRoot(/* name */ "remove-ff",
            /* fullName */ "refs/heads/remove-ff",
            /* customAnnotation */ "# Red edge: branch is out of sync to its parent branch",
            nullPointedCommit,
            /* forkPoint */ null,
            /* childBranches */ List.empty(),
            /* commits */ List.of(new Commit("Some commit")),
            SyncToParentStatus.OutOfSync)
    };

    val root = new Root(/* name */ "develop",
        /* fullName */ "refs/heads/develop",
        /* customAnnotation */ "# This is a root branch, the underline indicates that it is the currently checked out branch",
        nullPointedCommit,
        /* childBranches */ List.of(nonRoots));

    for (val nr : nonRoots) {
      nr.setParent(root);
    }

    this.roots = List.of(root);
  }

  static RelationToRemote getRelationOfSTRS(SyncToRemoteStatus syncToRemoteStatus) {
    return RelationToRemote.of(syncToRemoteStatus, "origin");
  }

  @Override
  public BranchLayout getBranchLayout() {
    throw new NotImplementedError();
  }

  @Override
  public List<IRootManagedBranchSnapshot> getRootBranches() {
    return roots;
  }

  @Override
  public @Nullable IManagedBranchSnapshot getCurrentBranchIfManaged() {
    return roots.headOption().getOrNull();
  }

  @Override
  public List<IManagedBranchSnapshot> getManagedBranches() {
    throw new NotImplementedError();
  }

  @Override
  public @Nullable IManagedBranchSnapshot getManagedBranchByName(@Nullable String branchName) {
    throw new NotImplementedError();
  }

  @Override
  public Set<String> getDuplicatedBranchNames() {
    return TreeSet.empty();
  }

  @Override
  public Set<String> getSkippedBranchNames() {
    return TreeSet.empty();
  }

  @Override
  public @Nullable IExecutionResult executeMachetePreRebaseHookIfPresent(IGitRebaseParameters gitRebaseParameters) {
    return null;
  }

  @Getter
  public final OngoingRepositoryOperation ongoingRepositoryOperation = new OngoingRepositoryOperation(
      OngoingRepositoryOperationType.NO_OPERATION, null);

  @AllArgsConstructor
  private static class Commit implements ICommitOfManagedBranch {

    private final String msg;

    @Override
    public String getShortMessage() {
      return msg;
    }

    @Override
    public String getFullMessage() {
      throw new NotImplementedError();
    }

    @Override
    public @ArrayLen(40) String getHash() {
      throw new NotImplementedError();
    }

    @Override
    public @ArrayLen(7) String getShortHash() {
      throw new NotImplementedError();
    }

    @Override
    public Instant getCommitTime() {
      throw new NotImplementedError();
    }
  }

  @Getter
  @RequiredArgsConstructor
  private static final class LocalBranchRef implements ILocalBranchReference {
    private final String name;

    @Override
    public String getFullName() {
      return "refs/heads/" + getName();
    }
  }

  private static final class FpCommit extends Commit implements IForkPointCommitOfManagedBranch {
    FpCommit(String msg) {
      super(msg);
    }

    @Override
    public @ArrayLen(7) String getShortHash() {
      return "1461ce9";
    }

    @Override
    public List<IBranchReference> getBranchesContainingInReflog() {
      return List.of(new LocalBranchRef("some-other-branch"));
    }

    @Override
    public List<IBranchReference> getUniqueBranchesContainingInReflog() {
      return getBranchesContainingInReflog();
    }

    @Override
    public boolean isOverridden() {
      return false;
    }
  }

  @Getter
  @RequiredArgsConstructor
  private static final class Root implements IRootManagedBranchSnapshot {
    private final String name;
    private final String fullName;
    private final String customAnnotation;
    private final Commit pointedCommit;
    private final RelationToRemote relationToRemote = getRelationOfSTRS(SyncToRemoteStatus.InSyncToRemote);
    private final List<INonRootManagedBranchSnapshot> children;

    @Override
    public @Nullable String getCustomAnnotation() {
      return customAnnotation;
    }

    @Override
    public @Nullable String getStatusHookOutput() {
      return null;
    }

    @Override
    public @Nullable IRemoteTrackingBranchReference getRemoteTrackingBranch() {
      return null;
    }
  }

  @Getter
  @RequiredArgsConstructor
  private static final class NonRoot implements INonRootManagedBranchSnapshot {
    private final String name;
    private final String fullName;
    private final String customAnnotation;
    private final Commit pointedCommit;
    private final @Nullable IForkPointCommitOfManagedBranch forkPoint;
    private final RelationToRemote relationToRemote = getRelationOfSTRS(SyncToRemoteStatus.InSyncToRemote);
    private final List<INonRootManagedBranchSnapshot> children;

    private final List<ICommitOfManagedBranch> uniqueCommits;
    private final List<ICommitOfManagedBranch> commitsUntilParent = List.empty();
    @MonotonicNonNull
    private IManagedBranchSnapshot parent = null;
    private final SyncToParentStatus syncToParentStatus;

    @Override
    public IManagedBranchSnapshot getParent() {
      assert parent != null : "parentBranch hasn't been set yet";
      return parent;
    }

    void setParent(IManagedBranchSnapshot givenParentBranch) {
      assert parent == null : "parentBranch has already been set";
      parent = givenParentBranch;
    }

    @Override
    public @Nullable String getCustomAnnotation() {
      return customAnnotation;
    }

    @Override
    public @Nullable String getStatusHookOutput() {
      return null;
    }

    @Override
    public @Nullable IForkPointCommitOfManagedBranch getForkPoint() {
      return forkPoint;
    }

    @Override
    public IGitRebaseParameters getParametersForRebaseOntoParent() {
      throw new NotImplementedError();
    }

    @Override
    public @Nullable IRemoteTrackingBranchReference getRemoteTrackingBranch() {
      return null;
    }
  }
}
