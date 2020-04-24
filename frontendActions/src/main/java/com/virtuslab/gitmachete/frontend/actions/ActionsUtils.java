package com.virtuslab.gitmachete.frontend.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.keys.DataKeys;

public final class ActionsUtils {

  private ActionsUtils() {}

  static GitRepository getIdeaRepository(AnActionEvent anActionEvent) {
    GitRepository repository = anActionEvent.getData(DataKeys.KEY_SELECTED_VCS_REPOSITORY);
    assert repository != null : "Can't get selected GitRepository";
    return repository;
  }

  /**
   * This method relies on key `KEY_GIT_MACHETE_REPOSITORY` corresponding to a non-null value
   * and hence must always be called after checking the git machete repository readiness.
   * See {@link BaseRebaseBranchOntoParentAction#update} and {@link DataKeys#KEY_IS_GIT_MACHETE_REPOSITORY_READY}.
   */
  static IGitMacheteRepository getMacheteRepository(AnActionEvent anActionEvent) {
    IGitMacheteRepository gitMacheteRepository = anActionEvent.getData(DataKeys.KEY_GIT_MACHETE_REPOSITORY);
    assert gitMacheteRepository != null : "Can't get gitMacheteRepository";

    return gitMacheteRepository;
  }

  static Option<BaseGitMacheteBranch> getSelectedMacheteBranch(AnActionEvent anActionEvent) {
    IGitMacheteRepository gitMacheteRepository = getMacheteRepository(anActionEvent);
    String selectedBranchName = anActionEvent.getData(DataKeys.KEY_SELECTED_BRANCH_NAME);
    assert selectedBranchName != null : "Can't get selected branch";
    return gitMacheteRepository.getBranchByName(selectedBranchName);
  }
}
