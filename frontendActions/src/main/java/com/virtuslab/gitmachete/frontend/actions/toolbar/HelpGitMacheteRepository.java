package com.virtuslab.gitmachete.frontend.actions.toolbar;

import java.time.Instant;

import io.vavr.NotImplementedError;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRemoteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMergeParameters;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;

public class HelpGitMacheteRepository implements IGitMacheteRepository {

  private final List<BaseGitMacheteRootBranch> roots;

  public HelpGitMacheteRepository() {
    var nullPointedCommit = new Commit("");
    NonRoot[] nonRoots = {
        new NonRoot(/* name */ "allow-ownership-link",
            /* customAnnotation */ "#Branch is merged to its parent branch",
            nullPointedCommit,
            getSTRSofRelation(SyncToRemoteStatus.Relation.InSyncToRemote),
            /* downstreamBranches */ List.empty(),
            /* commits */ List.empty(),
            SyncToParentStatus.MergedToParent),
        new NonRoot(/* name */ "build-chain",
            /* customAnnotation */ "#Branch is in sync to its parent branch",
            nullPointedCommit,
            getSTRSofRelation(SyncToRemoteStatus.Relation.InSyncToRemote),
            /* downstreamBranches */ List.empty(),
            /* commits */ List.empty(),
            SyncToParentStatus.InSync),
        new NonRoot(/* name */ "call-ws",
            /* customAnnotation */ "#Branch is in sync to its parent branch but the fork point is off",
            nullPointedCommit,
            getSTRSofRelation(SyncToRemoteStatus.Relation.InSyncToRemote),
            /* downstreamBranches */ List.empty(),
            /* commits */ List.empty(),
            SyncToParentStatus.InSyncButForkPointOff),
        new NonRoot(/* name */ "remove-ff",
            /* customAnnotation */ "#Branch is out of sync to its parent branch",
            nullPointedCommit,
            getSTRSofRelation(SyncToRemoteStatus.Relation.InSyncToRemote),
            /* downstreamBranches */ List.empty(),
            /* commits */ List.empty(),
            SyncToParentStatus.OutOfSync)
    };

    var root = new Root(/* name */ "develop",
        /* customAnnotation */ "this is a root branch",
        nullPointedCommit,
        getSTRSofRelation(SyncToRemoteStatus.Relation.InSyncToRemote),
        /* downstreamBranches */ List.of(nonRoots));

    for (var nr : nonRoots) {
      nr.setUpstreamBranch(root);
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
  public List<BaseGitMacheteRootBranch> getRootBranches() {
    return roots;
  }

  @Override
  public Option<BaseGitMacheteBranch> getCurrentBranchIfManaged() {
    return Option.narrow(roots.headOption());
  }

  @Override
  public Option<BaseGitMacheteBranch> getBranchByName(String branchName) {
    throw new NotImplementedError();
  }

  @AllArgsConstructor
  private final class Commit implements IGitMacheteCommit {

    private final String msg;

    @Override
    public String getMessage() {
      return msg;
    }

    @Override
    public String getHash() {
      throw new NotImplementedError();
    }

    @Override
    public Instant getCommitTime() {
      throw new NotImplementedError();
    }
  }

  @Getter
  @AllArgsConstructor
  private final class Root extends BaseGitMacheteRootBranch {
    private final String name;
    private final String customAnnotation;
    private final Commit pointedCommit;
    private final SyncToRemoteStatus syncToRemoteStatus;
    private final List<BaseGitMacheteNonRootBranch> downstreamBranches;

    @Override
    public Option<String> getCustomAnnotation() {
      return Option.of(customAnnotation);
    }

    @Override
    public Option<IGitMacheteRemoteBranch> getRemoteTrackingBranch() {
      return Option.none();
    }
  }

  @Getter
  @RequiredArgsConstructor
  private final class NonRoot extends BaseGitMacheteNonRootBranch {
    private final String name;
    private final String customAnnotation;
    private final Commit pointedCommit;
    private final SyncToRemoteStatus syncToRemoteStatus;
    private final List<BaseGitMacheteNonRootBranch> downstreamBranches;

    private final List<IGitMacheteCommit> commits;
    @MonotonicNonNull
    private BaseGitMacheteBranch upstreamBranch = null;
    private final SyncToParentStatus syncToParentStatus;

    @Override
    public BaseGitMacheteBranch getUpstreamBranch() {
      assert upstreamBranch != null : "upstreamBranch hasn't been set yet";
      return upstreamBranch;
    }

    void setUpstreamBranch(BaseGitMacheteBranch givenUpstreamBranch) {
      assert upstreamBranch == null : "upstreamBranch has already been set";
      upstreamBranch = givenUpstreamBranch;
    }

    @Override
    public Option<String> getCustomAnnotation() {
      return Option.of(customAnnotation);
    }

    @Override
    public Option<IGitMacheteCommit> getForkPoint() {
      return Option.none();
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
