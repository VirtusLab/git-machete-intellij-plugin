package com.virtuslab.gitmachete.frontend.actions.expectedkeys;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;

public interface IExpectsKeyGitMacheteRepository extends IWithLogger {
  default Option<IGitMacheteRepository> getGitMacheteRepository(AnActionEvent anActionEvent) {
    return Option.of(anActionEvent.getData(DataKeys.KEY_GIT_MACHETE_REPOSITORY));
  }

  default Option<IGitMacheteRepository> getGitMacheteRepositoryWithLoggingOnEmpty(AnActionEvent anActionEvent) {
    var gitMacheteRepository = getGitMacheteRepository(anActionEvent);
    if (gitMacheteRepository.isEmpty()) {
      log().warn("Git Machete repository is undefined");
    }
    return gitMacheteRepository;
  }

  default Option<IBranchLayout> getBranchLayout(AnActionEvent anActionEvent) {
    var branchLayout = getGitMacheteRepositoryWithLoggingOnEmpty(anActionEvent)
        .flatMap(repository -> repository.getBranchLayout());
    if (branchLayout.isEmpty()) {
      log().warn("Branch layout is undefined");
    }
    return branchLayout;
  }

  default Option<IGitMacheteBranch> getCurrentMacheteBranchIfManaged(AnActionEvent anActionEvent) {
    return getGitMacheteRepositoryWithLoggingOnEmpty(anActionEvent)
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
    var gitMacheteBranch = getGitMacheteRepositoryWithLoggingOnEmpty(anActionEvent)
        .flatMap(r -> r.getManagedBranchByName(branchName));
    if (gitMacheteBranch.isEmpty()) {
      log().warn(branchName + " Git Machete branch is undefined");
    }
    return gitMacheteBranch;
  }
}
