package com.virtuslab.gitmachete.frontend.actions.common;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.manager.IBranchLayoutWriter;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.gitmachete.frontend.ui.api.table.IGraphTableManager;

public final class ActionUtils {

  private ActionUtils() {}

  public static Option<IBranchLayout> getBranchLayout(AnActionEvent anActionEvent) {
    return Option.of(anActionEvent.getData(DataKeys.KEY_BRANCH_LAYOUT));
  }

  public static Option<IBranchLayoutWriter> getBranchLayoutWriter(AnActionEvent anActionEvent) {
    return Option.of(anActionEvent.getData(DataKeys.KEY_BRANCH_LAYOUT_WRITER));
  }

  public static Option<BaseGitMacheteNonRootBranch> getCurrentMacheteNonRootBranch(AnActionEvent anActionEvent) {
    return getGitMacheteRepository(anActionEvent).flatMap(repository -> repository.getCurrentBranchIfManaged().flatMap(
        currentBranch -> currentBranch.isNonRootBranch() ? Option.some(currentBranch.asNonRootBranch()) : Option.none()));
  }

  public static Option<IGitMacheteRepository> getGitMacheteRepository(AnActionEvent anActionEvent) {
    return Option.of(anActionEvent.getData(DataKeys.KEY_GIT_MACHETE_REPOSITORY));
  }

  public static IGraphTableManager getGraphTableManager(AnActionEvent anActionEvent) {
    return anActionEvent.getData(DataKeys.KEY_GRAPH_TABLE_MANAGER);
  }

  public static Project getProject(AnActionEvent anActionEvent) {
    var project = anActionEvent.getProject();
    assert project != null : "Can't get project from action event";
    return project;
  }

  public static Option<String> getSelectedBranchName(AnActionEvent anActionEvent) {
    return Option.of(anActionEvent.getData(DataKeys.KEY_SELECTED_BRANCH_NAME));
  }

  public static Option<BaseGitMacheteBranch> getSelectedMacheteBranch(AnActionEvent anActionEvent) {
    return getGitMacheteRepository(anActionEvent).flatMap(
        repository -> getSelectedBranchName(anActionEvent).flatMap(repository::getBranchByName));
  }

  public static Option<GitRepository> getSelectedVcsRepository(AnActionEvent anActionEvent) {
    return Option.of(anActionEvent.getData(DataKeys.KEY_SELECTED_VCS_REPOSITORY));
  }
}
