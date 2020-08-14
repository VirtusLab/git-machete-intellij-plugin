package com.virtuslab.gitmachete.frontend.actions.expectedkeys;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;

public interface IExpectsKeyGitMacheteRepository extends IWithLogger {
  default Option<IGitMacheteRepositorySnapshot> getGitMacheteRepositorySnapshot(AnActionEvent anActionEvent) {
    return Option.of(anActionEvent.getData(DataKeys.KEY_GIT_MACHETE_REPOSITORY_SNAPSHOT));
  }

  default Option<IGitMacheteRepositorySnapshot> getGitMacheteRepositorySnapshotWithLogging(AnActionEvent anActionEvent) {
    var gitMacheteRepositorySnapshot = getGitMacheteRepositorySnapshot(anActionEvent);
    if (gitMacheteRepositorySnapshot.isEmpty()) {
      log().warn("Git Machete repository snapshot is undefined");
    }
    return gitMacheteRepositorySnapshot;
  }

  default Option<IBranchLayout> getBranchLayoutWithLogging(AnActionEvent anActionEvent) {
    var branchLayout = getGitMacheteRepositorySnapshotWithLogging(anActionEvent)
        .flatMap(repository -> repository.getBranchLayout());
    if (branchLayout.isEmpty()) {
      log().warn("Branch layout is undefined");
    }
    return branchLayout;
  }

  default Option<IManagedBranchSnapshot> getCurrentMacheteBranchIfManaged(AnActionEvent anActionEvent) {
    return getGitMacheteRepositorySnapshot(anActionEvent)
        .flatMap(repository -> repository.getCurrentBranchIfManaged());
  }

  default Option<IGitMacheteBranch> getCurrentMacheteBranchIfManagedWithLogging(AnActionEvent anActionEvent) {
    return getGitMacheteRepositorySnapshotWithLogging(anActionEvent)
        .flatMap(repository -> repository.getCurrentBranchIfManaged());
  }

  default Option<String> getCurrentBranchNameIfManaged(AnActionEvent anActionEvent) {
    return getCurrentMacheteBranchIfManaged(anActionEvent).map(branch -> branch.getName());
  }

  default Option<String> getCurrentBranchNameIfManagedWithLogging(AnActionEvent anActionEvent) {
    var currentBranchName = getCurrentMacheteBranchIfManagedWithLogging(anActionEvent).map(branch -> branch.getName());
    if (currentBranchName.isEmpty()) {
      log().warn("Current Git Machete branch name is undefined");
    }
    return currentBranchName;
  }

  default Option<IManagedBranchSnapshot> getGitMacheteBranchByNameWithLogging(AnActionEvent anActionEvent, String branchName) {
    var gitMacheteBranch = getGitMacheteRepositorySnapshotWithLogging(anActionEvent)
        .flatMap(r -> r.getManagedBranchByName(branchName));
    if (gitMacheteBranch.isEmpty()) {
      log().warn(branchName + " Git Machete branch is undefined");
    }
    return gitMacheteBranch;
  }

  default Option<IGitMacheteBranch> getGitMacheteBranchByName(AnActionEvent anActionEvent, String branchName) {
    return getGitMacheteRepositorySnapshot(anActionEvent).flatMap(r -> r.getManagedBranchByName(branchName));
  }
}
