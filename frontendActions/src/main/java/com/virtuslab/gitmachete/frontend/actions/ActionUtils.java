package com.virtuslab.gitmachete.frontend.actions;

import java.nio.file.Path;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.gitmachete.frontend.ui.api.table.IGraphTableManager;

final class ActionUtils {

  private ActionUtils() {}

  static Option<IBranchLayout> getBranchLayout(AnActionEvent anActionEvent) {
    return Option.of(anActionEvent.getData(DataKeys.KEY_BRANCH_LAYOUT));
  }

  static Option<BaseGitMacheteNonRootBranch> getCurrentMacheteNonRootBranch(AnActionEvent anActionEvent) {
    return getGitMacheteRepository(anActionEvent).flatMap(repository -> repository.getCurrentBranchIfManaged().flatMap(
        currentBranch -> currentBranch.isNonRootBranch() ? Option.some(currentBranch.asNonRootBranch()) : Option.none()));
  }

  static Option<IGitMacheteRepository> getGitMacheteRepository(AnActionEvent anActionEvent) {
    return Option.of(anActionEvent.getData(DataKeys.KEY_GIT_MACHETE_REPOSITORY));
  }

  static Option<Path> getGitMacheteFilePath(AnActionEvent anActionEvent) {
    return Option.of(anActionEvent.getData(DataKeys.KEY_GIT_MACHETE_FILE_PATH));
  }

  static IGraphTableManager getGraphTableManager(AnActionEvent anActionEvent) {
    return anActionEvent.getData(DataKeys.KEY_GRAPH_TABLE_MANAGER);
  }

  static Project getProject(AnActionEvent anActionEvent) {
    var project = anActionEvent.getProject();
    assert project != null : "Can't get project from action event";
    return project;
  }

  static Option<String> getSelectedBranchName(AnActionEvent anActionEvent) {
    return Option.of(anActionEvent.getData(DataKeys.KEY_SELECTED_BRANCH_NAME));
  }

  static Option<BaseGitMacheteBranch> getSelectedMacheteBranch(AnActionEvent anActionEvent) {
    return getGitMacheteRepository(anActionEvent).flatMap(
        repository -> getSelectedBranchName(anActionEvent).flatMap(repository::getBranchByName));
  }

  static Option<GitRepository> getSelectedVcsRepository(AnActionEvent anActionEvent) {
    return Option.of(anActionEvent.getData(DataKeys.KEY_SELECTED_VCS_REPOSITORY));
  }
}
