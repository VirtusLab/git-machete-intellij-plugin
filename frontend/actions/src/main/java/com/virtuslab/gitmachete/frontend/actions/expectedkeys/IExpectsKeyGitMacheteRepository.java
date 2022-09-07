package com.virtuslab.gitmachete.frontend.actions.expectedkeys;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.frontend.actions.base.IWithLogger;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;

public interface IExpectsKeyGitMacheteRepository extends IWithLogger {
  default Option<IGitMacheteRepositorySnapshot> getGitMacheteRepositorySnapshot(AnActionEvent anActionEvent) {
    val gitMacheteRepositorySnapshot = Option.of(anActionEvent.getData(DataKeys.KEY_GIT_MACHETE_REPOSITORY_SNAPSHOT));
    if (isLoggingAcceptable() && gitMacheteRepositorySnapshot.isEmpty()) {
      log().warn("Git Machete repository snapshot is undefined");
    }
    return gitMacheteRepositorySnapshot;
  }

  default Option<IBranchLayout> getBranchLayout(AnActionEvent anActionEvent) {
    val branchLayout = getGitMacheteRepositorySnapshot(anActionEvent)
        .map(repository -> repository.getBranchLayout());
    if (isLoggingAcceptable() && branchLayout.isEmpty()) {
      log().warn("Branch layout is undefined");
    }
    return branchLayout;
  }

  default @Nullable IManagedBranchSnapshot getCurrentMacheteBranchIfManaged(AnActionEvent anActionEvent) {
    return getGitMacheteRepositorySnapshot(anActionEvent)
        .map(repository -> repository.getCurrentBranchIfManaged()).get();
  }

  default @Nullable String getCurrentBranchNameIfManaged(AnActionEvent anActionEvent) {
    val currentBranchName = getCurrentMacheteBranchIfManaged(anActionEvent).getName();
    if (isLoggingAcceptable() && currentBranchName == null) {
      log().warn("Current Git Machete branch name is undefined");
    }
    return currentBranchName;
  }

  default @Nullable IManagedBranchSnapshot getManagedBranchByName(AnActionEvent anActionEvent, String branchName) {
    val branch = getGitMacheteRepositorySnapshot(anActionEvent)
        .map(r -> r.getManagedBranchByName(branchName)).get();
    if (isLoggingAcceptable() && branch == null) {
      log().warn(branchName + " Git Machete branch is undefined");
    }
    return branch;
  }
}
