package com.virtuslab.gitmachete.frontend.actions.expectedkeys;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.frontend.actions.base.IWithLogger;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;

public interface IExpectsKeyGitMacheteRepository extends IWithLogger {
  default Option<IGitMacheteRepositorySnapshot> getGitMacheteRepositorySnapshot(AnActionEvent anActionEvent) {
    var gitMacheteRepositorySnapshot = Option.of(anActionEvent.getData(DataKeys.KEY_GIT_MACHETE_REPOSITORY_SNAPSHOT));
    if (isLoggingAcceptable() && gitMacheteRepositorySnapshot.isEmpty()) {
      log().warn("Git Machete repository snapshot is undefined");
    }
    return gitMacheteRepositorySnapshot;
  }

  default Option<IBranchLayout> getBranchLayout(AnActionEvent anActionEvent) {
    var branchLayout = getGitMacheteRepositorySnapshot(anActionEvent)
        .flatMap(repository -> repository.getBranchLayout());
    if (isLoggingAcceptable() && branchLayout.isEmpty()) {
      log().warn("Branch layout is undefined");
    }
    return branchLayout;
  }

  default Option<IManagedBranchSnapshot> getCurrentMacheteBranchIfManaged(AnActionEvent anActionEvent) {
    return getGitMacheteRepositorySnapshot(anActionEvent)
        .flatMap(repository -> repository.getCurrentBranchIfManaged());
  }

  default Option<String> getCurrentBranchNameIfManaged(AnActionEvent anActionEvent) {
    var currentBranchName = getCurrentMacheteBranchIfManaged(anActionEvent).map(branch -> branch.getName());
    if (isLoggingAcceptable() && currentBranchName.isEmpty()) {
      log().warn("Current Git Machete branch name is undefined");
    }
    return currentBranchName;
  }

  default Option<IManagedBranchSnapshot> getManagedBranchByName(AnActionEvent anActionEvent, String branchName) {
    var branch = getGitMacheteRepositorySnapshot(anActionEvent)
        .flatMap(r -> r.getManagedBranchByName(branchName));
    if (isLoggingAcceptable() && branch.isEmpty()) {
      log().warn(branchName + " Git Machete branch is undefined");
    }
    return branch;
  }
}
