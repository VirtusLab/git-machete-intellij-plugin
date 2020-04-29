package com.virtuslab.gitmachete.frontend.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.gitmachete.frontend.ui.api.table.IGraphTableManager;

public final class ActionUtils {

  private ActionUtils() {}

  static GitRepository getPresentIdeaRepository(AnActionEvent anActionEvent) {
    GitRepository repository = anActionEvent.getData(DataKeys.KEY_SELECTED_VCS_REPOSITORY);
    assert repository != null : "Can't get selected GitRepository";
    return repository;
  }

  static IGraphTableManager getPresentGraphTableManager(AnActionEvent anActionEvent) {
    IGraphTableManager graphTableManager = anActionEvent.getData(DataKeys.KEY_GRAPH_TABLE_MANAGER);
    assert graphTableManager != null : "Can't get graph table manager";
    return graphTableManager;
  }

  /**
   * This method relies on key `KEY_GIT_MACHETE_REPOSITORY` corresponding to a non-null value
   * and hence must always be called after checking the git machete repository readiness.
   * See {@link BaseRebaseBranchOntoParentAction#update} and {@link DataKeys#KEY_IS_GIT_MACHETE_REPOSITORY_READY}.
   */
  static IGitMacheteRepository getPresentMacheteRepository(AnActionEvent anActionEvent) {
    IGitMacheteRepository gitMacheteRepository = anActionEvent.getData(DataKeys.KEY_GIT_MACHETE_REPOSITORY);
    assert gitMacheteRepository != null : "Can't get gitMacheteRepository";

    return gitMacheteRepository;
  }

  static Option<BaseGitMacheteBranch> getSelectedMacheteBranch(AnActionEvent anActionEvent) {
    IGitMacheteRepository gitMacheteRepository = getPresentMacheteRepository(anActionEvent);
    String selectedBranchName = anActionEvent.getData(DataKeys.KEY_SELECTED_BRANCH_NAME);
    assert selectedBranchName != null : "Can't get selected branch";
    return gitMacheteRepository.getBranchByName(selectedBranchName);
  }

  static BaseGitMacheteNonRootBranch getCurrentBaseMacheteNonRootBranch(AnActionEvent anActionEvent) {
    var gitMacheteRepository = getPresentMacheteRepository(anActionEvent);
    var currentBranchOption = gitMacheteRepository.getCurrentBranchIfManaged();
    assert currentBranchOption.isDefined() : "Can't get current branch";
    var baseGitMacheteBranch = currentBranchOption.get();
    assert !baseGitMacheteBranch.isRootBranch() : "Selected branch is a root branch";

    return baseGitMacheteBranch.asNonRootBranch();
  }
}
