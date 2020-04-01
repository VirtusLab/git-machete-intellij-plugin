package com.virtuslab.gitmachete.frontend.actions;

import java.util.List;
import java.util.Optional;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
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
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 */
public class RebaseCurrentBranchOntoParentAction extends BaseRebaseBranchOntoParentAction {
  private static final String ACTION_TEXT = "Rebase Current Branch Onto Parent";
  private static final String ACTION_DESCRIPTION = "Rebase current branch onto parent";

  public RebaseCurrentBranchOntoParentAction() {
    super(ACTION_TEXT, ACTION_DESCRIPTION, AllIcons.Actions.Menu_cut);
  }

  @Override
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);
    prohibitRebaseOfNonManagedRevisionOrRootBranch(anActionEvent);
    updateDescriptionIfPresentationVisible(anActionEvent);
  }

  /**
   * Assumption to following code:
   * - the result of {@link com.virtuslab.gitmachete.backend.api.IGitMacheteRepository#getCurrentBranchIfManaged}
   * is present and it is not a root branch because if it was not the user wouldn't be able to perform action in the first place
   * - the result of {@link BaseRebaseBranchOntoParentAction#deriveGitRebaseOntoParentParameters}
   * may not be be present (due to exceptions thrown by {@link IGitMacheteRepository#deriveParametersForRebaseOntoParent})
   */
  @Override
  public void actionPerformed(AnActionEvent anActionEvent) {
    Optional<BaseGitMacheteBranch> branchToRebase = getMacheteRepository(anActionEvent).getCurrentBranchIfManaged();
    assert branchToRebase.isPresent();

    Project project = anActionEvent.getProject();
    assert project != null;

    IGitMacheteRepository macheteRepository = getMacheteRepository(anActionEvent);
    Optional<IGitRebaseParameters> gitRebaseParameters = deriveGitRebaseOntoParentParameters(macheteRepository,
        branchToRebase.get().asNonRootBranch());
    assert gitRebaseParameters.isPresent();

    GitRepository repository = getIdeaRepository(anActionEvent);

    new Task.Backgroundable(project, "Rebasing") {
      @Override
      public void run(ProgressIndicator indicator) {
        GitRebaseParams params = getIdeaRebaseParamsOf(anActionEvent, gitRebaseParameters.get());
        GitRebaseUtils.rebase(project, List.of(repository), params, indicator);
      }

      // TODO (#95): on success, refresh only sync statuses (not the whole repository). Keep in mind potential changes
      // to commits (eg. commits may get squashed so the graph structure changes).
    }.queue();
  }

  private void updateDescriptionIfPresentationVisible(AnActionEvent anActionEvent) {
    Presentation presentation = anActionEvent.getPresentation();
    if (presentation.isEnabledAndVisible()) {
      var branchToRebaseOptional = getMacheteRepository(anActionEvent).getCurrentBranchIfManaged();
      assert branchToRebaseOptional.isPresent();
      var upstreamBranch = branchToRebaseOptional.get().asNonRootBranch().getUpstreamBranch();

      var description = String.format("Rebase \"%s\" onto \"%s\"",
          branchToRebaseOptional.get().getName(), upstreamBranch.getName());

      presentation.setDescription(description);
    }
  }

  private void prohibitRebaseOfNonManagedRevisionOrRootBranch(AnActionEvent anActionEvent) {
    IGitMacheteRepository gitMacheteRepository = getMacheteRepository(anActionEvent);

    Presentation presentation = anActionEvent.getPresentation();
    var currentBranchOption = gitMacheteRepository.getCurrentBranchIfManaged();

    if (presentation.isEnabledAndVisible()) {
      if (!currentBranchOption.isPresent()) {
        presentation.setDescription("Current revision is not a branch managed by Git Machete");
        presentation.setEnabled(false);

      } else if (currentBranchOption.get().isRootBranch()) {
        String description = String.format("Can't rebase git machete root branch \"%s\"",
            currentBranchOption.get().getName());
        presentation.setDescription(description);
        presentation.setEnabled(false);
      }
    }
  }
}
