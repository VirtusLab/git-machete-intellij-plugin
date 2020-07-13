package com.virtuslab.gitmachete.frontend.actions.expectedkeys;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;

public interface IExpectsKeyGitMacheteRepository extends IWithLogger {
  default Option<IGitMacheteRepositorySnapshot> getGitMacheteRepositorySnapshot(AnActionEvent anActionEvent) {
    return Option.of(anActionEvent.getData(DataKeys.KEY_GIT_MACHETE_REPOSITORY_SNAPSHOT));
  }

  default Option<IGitMacheteRepositorySnapshot> getGitMacheteRepositorySnapshotWithLoggingOnEmpty(AnActionEvent anActionEvent) {
    var gitMacheteRepositorySnapshot = getGitMacheteRepositorySnapshot(anActionEvent);
    if (gitMacheteRepositorySnapshot.isEmpty()) {
      log().warn("Git Machete repository snapshot is undefined");
    }
    return gitMacheteRepositorySnapshot;
  }

  default Option<IBranchLayout> getBranchLayout(AnActionEvent anActionEvent) {
    var branchLayout = getGitMacheteRepositorySnapshotWithLoggingOnEmpty(anActionEvent)
        .flatMap(repository -> repository.getBranchLayout());
    if (branchLayout.isEmpty()) {
      log().warn("Branch layout is undefined");
    }
    return branchLayout;
  }

  default Option<IGitMacheteBranch> getCurrentMacheteBranchIfManaged(AnActionEvent anActionEvent) {
    return getGitMacheteRepositorySnapshotWithLoggingOnEmpty(anActionEvent)
        .flatMap(repository -> repository.getCurrentBranchIfManaged());
  }

  default Option<String> getCurrentBranchNameIfManaged(AnActionEvent anActionEvent) {
    return getCurrentMacheteBranchIfManaged(anActionEvent).map(branch -> branch.getName());
  }

  default Option<String> getCurrentBranchNameIfManagedWithLoggingOnEmpty(AnActionEvent anActionEvent) {
    var currentBranchName = getCurrentBranchNameIfManaged(anActionEvent);
    if (currentBranchName.isEmpty()) {
      log().warn("Current Git Machete branch name is undefined");
    }
    return currentBranchName;
  }

  default Option<IGitMacheteBranch> getGitMacheteBranchByName(AnActionEvent anActionEvent, String branchName) {
    var gitMacheteBranch = getGitMacheteRepositorySnapshotWithLoggingOnEmpty(anActionEvent)
        .flatMap(r -> r.getManagedBranchByName(branchName));
    if (gitMacheteBranch.isEmpty()) {
      log().warn(branchName + " Git Machete branch is undefined");
    }
    return gitMacheteBranch;
  }
}
