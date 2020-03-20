package com.virtuslab.gitmachete.frontend.actions;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import git4idea.branch.GitRebaseParams;
import git4idea.rebase.GitRebaseUtils;
import git4idea.repo.GitRepository;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;

/**
 * Expects DataKeys:
 * none
 */
public class RebaseCurrentBranchOntoParentAction extends BaseRebaseBranchOntoParentAction {
  private static final String ACTION_TEXT = "Rebase Current Branch Onto Parent";
  private static final String ACTION_DESCRIPTION = "Rebase current branch onto parent";

  public RebaseCurrentBranchOntoParentAction() {
    super(ACTION_TEXT, ACTION_DESCRIPTION, AllIcons.Actions.Menu_cut);
  }

  @Override
  public void update(AnActionEvent anActionEvent) {
    anActionEvent.getPresentation().setDescription(ACTION_DESCRIPTION);
    super.update(anActionEvent);
    prohibitRebaseOfNonManagedOrRootBranch(anActionEvent);
  }

  @Override
  public void actionPerformedAfterChecks(@Nonnull AnActionEvent anActionEvent) {
    Optional<BaseGitMacheteBranch> branchToRebase = getMacheteRepository(anActionEvent).getCurrentBranchIfManaged();
    assert branchToRebase.isPresent();

    Project project = anActionEvent.getProject();
    assert project != null;

    IGitMacheteRepository macheteRepository = getMacheteRepository(anActionEvent);
    Optional<IGitRebaseParameters> gitRebaseParameters = deriveGitRebaseOntoParentParameters(macheteRepository,
        branchToRebase.get());
    assert gitRebaseParameters.isPresent();

    GitRepository repository = getIdeaRepository(anActionEvent);

    new Task.Backgroundable(project, "Rebasing") {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        GitRebaseParams params = getIdeaRebaseParamsOf(anActionEvent, gitRebaseParameters.get());
        GitRebaseUtils.rebase(project, List.of(repository), params, indicator);
      }

      // TODO (#95): on success, refresh only sync statuses (not the whole repository). Keep in mind potential changes
      // to commits (eg. commits may get squashed so the graph structure changes).
    }.queue();
  }

  private void prohibitRebaseOfNonManagedOrRootBranch(AnActionEvent anActionEvent) {
    IGitMacheteRepository gitMacheteRepository = getMacheteRepository(anActionEvent);

    Presentation presentation = anActionEvent.getPresentation();
    var currentBranchOption = gitMacheteRepository.getCurrentBranchIfManaged();

    if (presentation.isEnabledAndVisible()) {
      if (!currentBranchOption.isPresent()) {
        presentation.setDescription("Current branch is not managed by Git Machete");
        presentation.setEnabled(false);

      } else if (gitMacheteRepository.getRootBranches().contains(currentBranchOption.get())) {
        String description = String.format("Can't rebase git machete root branch \"%s\"",
            currentBranchOption.get().getName());
        presentation.setDescription(description);
        presentation.setEnabled(false);
      }
    }
  }
}
