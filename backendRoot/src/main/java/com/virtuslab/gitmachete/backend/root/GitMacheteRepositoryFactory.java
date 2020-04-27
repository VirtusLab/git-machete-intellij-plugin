package com.virtuslab.gitmachete.backend.root;

import static com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus.Relation.Ahead;
import static com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus.Relation.Behind;
import static com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus.Relation.Diverged;
import static com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus.Relation.InSync;
import static com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus.Relation.Untracked;

import java.nio.file.Path;

import io.vavr.Tuple;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import io.vavr.control.Try;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BaseBranchLayoutEntry;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.gitcore.api.BaseGitCoreCommit;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreBranchTrackingStatus;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitcore.api.IGitCoreRepositoryFactory;
import com.virtuslab.gitcore.impl.jgit.GitCoreRepositoryFactory;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryFactory;
import com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.impl.GitMacheteCommit;
import com.virtuslab.gitmachete.backend.impl.GitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepository;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRootBranch;
import com.virtuslab.gitmachete.backend.impl.SyncToRemoteStatus;

public class GitMacheteRepositoryFactory implements IGitMacheteRepositoryFactory {
  private static final LambdaLogger LOG = LambdaLoggerFactory.getLogger("backendRoot");

  private Map<String, BaseGitMacheteBranch> branchByName = HashMap.empty();

  private final IGitCoreRepositoryFactory gitCoreRepositoryFactory;

  public GitMacheteRepositoryFactory() {
    gitCoreRepositoryFactory = new GitCoreRepositoryFactory();
  }

  // TODO (#202): possible this should be included in IGitMacheteRepositoryFactory as well...
  // this might require some changes in Gradle subprojects structure (likely moving IGitMacheteRepositoryFactory to a
  // "backendRootApi" or something like that)
  @Override
  public IGitMacheteRepository create(Path mainDirectoryPath, Path gitDirectoryPath, IBranchLayout branchLayout)
      throws GitMacheteException {
    LOG.debug(() -> "Enter GitMacheteRepositoryFactory#create(mainDirectoryPath = ${mainDirectoryPath}, "
        + "gitDirectoryPath = ${gitDirectoryPath})");
    // To make sure there are no leftovers from the previous invocations.
    branchByName = HashMap.empty();

    IGitCoreRepository gitCoreRepository = Try
        .of(() -> gitCoreRepositoryFactory.create(mainDirectoryPath, gitDirectoryPath))
        .getOrElseThrow(
            e -> new GitMacheteException("Can't create an ${IGitCoreRepository.class.getSimpleName()} instance " +
                "under ${mainDirectoryPath} (with git directory under ${gitDirectoryPath})", e));

    var rootBranchTries = branchLayout.getRootBranches()
        .map(entry -> Try.of(() -> createGitMacheteRootBranch(gitCoreRepository, entry)));
    var rootBranches = Try.sequence(rootBranchTries).getOrElseThrow(GitMacheteException::castOrWrap);

    var rootBranchByName = rootBranches.toMap(branch -> Tuple.of(branch.getName(), branch));
    branchByName = branchByName.merge(rootBranchByName);

    BaseGitMacheteBranch currentBranch = Try.of(() -> gitCoreRepository.getCurrentBranch())
        .getOrElseThrow(e -> new GitMacheteException("Can't get current branch", e))
        .flatMap(cb -> branchByName.get(cb.getName()))
        .getOrNull();

    LOG.debug(() -> "Current branch: ${currentBranch != null ? currentBranch.getName() : null}");

    return new GitMacheteRepository(List.ofAll(rootBranches), branchLayout, currentBranch, branchByName);
  }

  private GitMacheteRootBranch createGitMacheteRootBranch(IGitCoreRepository gitCoreRepository,
      BaseBranchLayoutEntry entry) throws GitMacheteException {
    IGitCoreLocalBranch coreLocalBranch = Try.of(() -> gitCoreRepository.getLocalBranch(entry.getName()))
        .getOrElseThrow(e -> new GitMacheteException(e));

    BaseGitCoreCommit corePointedCommit = Try.of(() -> coreLocalBranch.getPointedCommit())
        .getOrElseThrow(e -> new GitMacheteException(e));

    var pointedCommit = new GitMacheteCommit(corePointedCommit);
    var syncToRemoteStatus = deriveSyncToRemoteStatus(coreLocalBranch);
    var customAnnotation = entry.getCustomAnnotation().getOrNull();
    var subbranches = deriveDownstreamBranches(gitCoreRepository, coreLocalBranch, entry);

    return new GitMacheteRootBranch(entry.getName(), subbranches, pointedCommit, syncToRemoteStatus, customAnnotation);
  }

  private GitMacheteNonRootBranch createGitMacheteNonRootBranch(IGitCoreRepository gitCoreRepository,
      IGitCoreLocalBranch parentEntryCoreLocalBranch,
      BaseBranchLayoutEntry entry)
      throws GitMacheteException {

    IGitCoreLocalBranch coreLocalBranch = Try.of(() -> gitCoreRepository.getLocalBranch(entry.getName()))
        .getOrElseThrow(e -> new GitMacheteException(e));

    Option<BaseGitCoreCommit> deducedForkPoint = deduceForkPoint(gitCoreRepository, coreLocalBranch,
        parentEntryCoreLocalBranch);

    BaseGitCoreCommit corePointedCommit = Try.of(() -> coreLocalBranch.getPointedCommit())
        .getOrElseThrow(e -> new GitMacheteException(e));

    // translate IGitCoreCommit list to IGitMacheteCommit list
    List<IGitMacheteCommit> commits = deducedForkPoint.isDefined()
        ? Try.of(() -> coreLocalBranch.deriveCommitsUntil(deducedForkPoint.get()))
            .getOrElseThrow(e -> new GitMacheteException(e))
            .map(GitMacheteCommit::new)
            .collect(List.collector())
        : List.empty();

    var pointedCommit = new GitMacheteCommit(corePointedCommit);
    var forkPoint = deducedForkPoint.isDefined() ? new GitMacheteCommit(deducedForkPoint.get()) : null;
    var syncToRemoteStatus = deriveSyncToRemoteStatus(coreLocalBranch);
    var syncToParentStatus = deriveSyncToParentStatus(gitCoreRepository, coreLocalBranch, parentEntryCoreLocalBranch,
        deducedForkPoint.getOrNull());
    var customAnnotation = entry.getCustomAnnotation().getOrNull();
    var subbranches = deriveDownstreamBranches(gitCoreRepository, coreLocalBranch, entry);

    return new GitMacheteNonRootBranch(entry.getName(), subbranches, forkPoint, pointedCommit,
        commits, syncToRemoteStatus, syncToParentStatus, customAnnotation);
  }

  private Option<BaseGitCoreCommit> deduceForkPoint(
      IGitCoreRepository gitCoreRepository,
      IGitCoreLocalBranch coreLocalBranch,
      IGitCoreLocalBranch parentCoreLocalBranch) throws GitMacheteException {
    LOG.debug("Enter deduceForkPoint");
    return Try.of(() -> {

      var forkPointOption = coreLocalBranch.deriveForkPoint();
      var parentPointedCommit = parentCoreLocalBranch.getPointedCommit();
      var pointedCommit = coreLocalBranch.getPointedCommit();

      LOG.debug(
          () -> "forkPointOption = ${forkPointOption.isDefined() ? forkPointOption.get().getHash().getHashString() : \"empty\"}, "
              + "parentPointedCommit = ${parentPointedCommit.getHash().getHashString()}, " +
              "pointedCommit = ${pointedCommit.getHash().getHashString()}");

      var isParentAncestorOfChild = gitCoreRepository.isAncestor(parentPointedCommit, pointedCommit);

      LOG.debug(() -> "Parent branch commit (${parentPointedCommit.getHash().getHashString()}) " +
          "is${isParentAncestorOfChild ? \"\" : \" NOT\"} ancestor of child commit " +
          "(${pointedCommit.getHash().getHashString()})");

      if (isParentAncestorOfChild) {
        if (forkPointOption.isDefined()) {
          var isParentAncestorOfForkPoint = gitCoreRepository.isAncestor(parentPointedCommit, forkPointOption.get());

          if (!isParentAncestorOfForkPoint) {
            // If parent(A) is ancestor of A, and parent(A) is NOT ancestor of fork-point(A),
            // then assume fork-point(A)=parent(A)
            LOG.debug(() -> "Parent branch commit (${parentPointedCommit.getHash().getHashString()}) is ancestor of " +
                "pointed commit (${pointedCommit.getHash().getHashString()}) but parent branch commit is NOT ancestor " +
                "of pointed commit fork point (${forkPointOption.get().getHash().getHashString()}), " +
                "so we assume that pointed commit fork point = parent branch commit");
            return Option.of(parentPointedCommit);
          }

        } else {
          // If parent(A) is ancestor of A, and fork-point(A) is missing,
          // then assume fork-point(A)=parent(A)
          LOG.debug(
              () -> "Parent branch commit (${parentPointedCommit.getHash().getHashString()}) is ancestor of pointed commit " +
                  "(${pointedCommit.getHash().getHashString()}) but fork point of pointed commit is missing, " +
                  "so we assume that pointed commit fork point = parent branch commit");
          return Option.of(parentPointedCommit);
        }
      }

      LOG.debug(() -> "Deduced fork point for branch '${coreLocalBranch.getName()}' is " +
          "${forkPointOption.isDefined() ? forkPointOption.get().getHash().getHashString() : \"empty\"}");

      return forkPointOption;

    }).getOrElseThrow(e -> new GitMacheteException(e));
  }

  private List<GitMacheteNonRootBranch> deriveDownstreamBranches(
      IGitCoreRepository gitCoreRepository,
      IGitCoreLocalBranch parentCoreLocalBranch,
      BaseBranchLayoutEntry directUpstreamEntry) throws GitMacheteException {

    var downstreamBranchTries = directUpstreamEntry.getSubentries().map(entry -> Try.of(
        () -> createGitMacheteNonRootBranch(gitCoreRepository, parentCoreLocalBranch, entry)));
    var downstreamBranches = Try.sequence(downstreamBranchTries).getOrElseThrow(GitMacheteException::castOrWrap);

    var downstreamBranchByName = downstreamBranches.toMap(branch -> Tuple.of(branch.getName(), branch));
    branchByName = branchByName.merge(downstreamBranchByName);

    return List.ofAll(downstreamBranches);
  }

  private ISyncToRemoteStatus deriveSyncToRemoteStatus(IGitCoreLocalBranch coreLocalBranch) throws GitMacheteException {
    LOG.debug(
        () -> "Enter ${getClass().getSimpleName()}#deriveSyncToRemoteStatus" +
                "(coreLocalBranch = ${coreLocalBranch.getName()})");
    try {
      Option<IGitCoreBranchTrackingStatus> ts = coreLocalBranch.deriveRemoteTrackingStatus();
      if (ts.isEmpty()) {
        LOG.debug("Branch is untracked");
        return SyncToRemoteStatus.of(Untracked, "");
      }

      IGitCoreBranchTrackingStatus trackingStatus = ts.get();

      SyncToRemoteStatus syncToRemoteStatus;

      if (trackingStatus.getAhead() > 0 && trackingStatus.getBehind() > 0) {
        syncToRemoteStatus = SyncToRemoteStatus.of(Diverged, trackingStatus.getRemoteName());
      } else if (trackingStatus.getAhead() > 0) {
        syncToRemoteStatus = SyncToRemoteStatus.of(Ahead, trackingStatus.getRemoteName());
      } else if (trackingStatus.getBehind() > 0) {
        syncToRemoteStatus = SyncToRemoteStatus.of(Behind, trackingStatus.getRemoteName());
      } else {
        syncToRemoteStatus = SyncToRemoteStatus.of(InSync, trackingStatus.getRemoteName());
      }

      LOG.debug(() -> "Sync to remove status: ${syncToRemoteStatus.toString()}");

      return syncToRemoteStatus;

    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

  private SyncToParentStatus deriveSyncToParentStatus(
      IGitCoreRepository gitCoreRepository,
      IGitCoreLocalBranch coreLocalBranch,
      IGitCoreLocalBranch parentCoreLocalBranch,
      @Nullable BaseGitCoreCommit forkPoint)
      throws GitMacheteException {
    LOG.debug(() -> "Enter GitMacheteRepositoryFactory#deriveSyncToParentStatus" +
        "(gitCoreRepository = ${gitCoreRepository}, coreLocalBranch = ${coreLocalBranch.getName()}, " +
        "parentCoreLocalBranch = ${parentCoreLocalBranch.getName()}, " +
        "forkPoint = ${forkPoint != null ? forkPoint.getHash().getHashString() : \"null\"})");
    try {
      BaseGitCoreCommit parentPointedCommit = parentCoreLocalBranch.getPointedCommit();
      BaseGitCoreCommit pointedCommit = coreLocalBranch.getPointedCommit();

      LOG.debug(() -> "parentPointedCommit = ${parentPointedCommit.getHash().getHashString()}; " +
          "pointedCommit = ${pointedCommit.getHash().getHashString()}");

      if (pointedCommit.equals(parentPointedCommit)) {
        if (coreLocalBranch.hasJustBeenCreated()) {
          LOG.debug("This branch has been detected as just created, so we assume it's in sync");
          return SyncToParentStatus.InSync;
        } else {
          LOG.debug(
              "Parent commit is equal to this branch pointed commit and this branch "
                  + "hasn't been detected as just created, so we assume it's merged");
          return SyncToParentStatus.Merged;
        }
      } else {
        var isParentAncestorOfChild = gitCoreRepository.isAncestor(
            /* presumedAncestor */ parentPointedCommit, /* presumedDescendant */ pointedCommit);

        if (isParentAncestorOfChild) {
          if (forkPoint != null && !forkPoint.equals(parentPointedCommit)) {
            LOG.debug(
                "Parent commit is ancestor of this branch pointed commit but fork point is not equal "
                    + "to parent commit, so we assume that this branch is \"InSyncButForkPointOff\"");
            return SyncToParentStatus.InSyncButForkPointOff;
          } else {
            LOG.debug(
                "Parent commit is ancestor of this branch pointed commit and fork point "
                    + "is absent or equal to parent commit, so we assume that this branch is in sync");
            return SyncToParentStatus.InSync;
          }
        } else {
          var isChildAncestorOfParent = gitCoreRepository.isAncestor(
              /* presumedAncestor */ pointedCommit, /* presumedDescendant */ parentPointedCommit);

          if (isChildAncestorOfParent) {
            LOG.debug(
                "Parent commit is not ancestor of this branch pointed commit but this branch pointed commit "
                    + "is ancestor of parent branch commit, so we assume that this branch is merged");
            return SyncToParentStatus.Merged;
          } else {
            LOG.debug(
                "Parent commit is not ancestor of this branch pointed commit neither this branch pointed commit "
                    + "is ancestor of parent branch commit, so we assume that this branch is out of sync");
            return SyncToParentStatus.OutOfSync;
          }
        }
      }

    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }
}
