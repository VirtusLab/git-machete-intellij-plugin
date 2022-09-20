package com.virtuslab.gitmachete.frontend.actions.expectedkeys;

import com.intellij.openapi.actionSystem.AnActionEvent;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.frontend.actions.base.IWithLogger;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;

public interface IExpectsKeyGitMacheteRepository extends IWithLogger {
  default @Nullable IGitMacheteRepositorySnapshot getGitMacheteRepositorySnapshot(AnActionEvent anActionEvent) {
    IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot = anActionEvent != null
        ? anActionEvent.getData(DataKeys.KEY_GIT_MACHETE_REPOSITORY_SNAPSHOT)
        : null;
    if (isLoggingAcceptable() && gitMacheteRepositorySnapshot == null) {
      log().warn("Git Machete repository snapshot is undefined");
    }
    return gitMacheteRepositorySnapshot;
  }

  default @Nullable IBranchLayout getBranchLayout(AnActionEvent anActionEvent) {
    val repoSnapshot = getGitMacheteRepositorySnapshot(anActionEvent);
    val branchLayout = repoSnapshot != null ? repoSnapshot.getBranchLayout() : null;
    if (isLoggingAcceptable() && branchLayout == null) {
      log().warn("Branch layout is undefined");
    }
    return branchLayout;
  }

  default @Nullable IManagedBranchSnapshot getCurrentMacheteBranchIfManaged(AnActionEvent anActionEvent) {
    val repoSnapshot = getGitMacheteRepositorySnapshot(anActionEvent);
    return repoSnapshot != null ? repoSnapshot.getCurrentBranchIfManaged() : null;
  }

  default @Nullable String getCurrentBranchNameIfManaged(AnActionEvent anActionEvent) {
    IManagedBranchSnapshot currentMacheteBranchIfManaged = getCurrentMacheteBranchIfManaged(anActionEvent);
    String currentBranchName = currentMacheteBranchIfManaged != null ? currentMacheteBranchIfManaged.getName() : null;
    if (isLoggingAcceptable() && currentBranchName == null) {
      log().warn("Current Git Machete branch name is undefined");
    }
    return currentBranchName;
  }

  default @Nullable IManagedBranchSnapshot getManagedBranchByName(AnActionEvent anActionEvent, @Nullable String branchName) {
    IGitMacheteRepositorySnapshot repositorySnapShot = getGitMacheteRepositorySnapshot(anActionEvent);
    IManagedBranchSnapshot branch = branchName != null && repositorySnapShot != null
        ? repositorySnapShot.getManagedBranchByName(branchName)
        : null;
    if (isLoggingAcceptable() && branch == null) {
      log().warn(branchName + " Git Machete branch is undefined");
    }
    return branch;
  }
}
