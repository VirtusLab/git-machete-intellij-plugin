package com.virtuslab.gitmachete.frontend.actions;

import java.util.List;
import java.util.Optional;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import git4idea.branch.GitRebaseParams;
import git4idea.rebase.GitRebaseUtils;
import git4idea.repo.GitRepository;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_SELECTED_BRANCH_NAME}</li>
 * </ul>
 */
public class RebaseSelectedBranchOntoParentAction extends BaseRebaseBranchOntoParentAction {

  @Override
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);
    Optional<BaseGitMacheteBranch> selectedMacheteBranchOption = getSelectedMacheteBranch(anActionEvent);
    var presentation = anActionEvent.getPresentation();
    if (selectedMacheteBranchOption.isPresent()) {
      prohibitRootBranchRebase(anActionEvent);
      if (presentation.isEnabledAndVisible()) {
        updateIfVisibleDescription(selectedMacheteBranchOption.get(), presentation);
      }
    }
  }

  @Override
  public void actionPerformedAfterChecks(AnActionEvent anActionEvent) {
    Project project = anActionEvent.getProject();
    assert project != null;

    Optional<BaseGitMacheteBranch> selectedGitMacheteBranch = getSelectedMacheteBranch(anActionEvent);
    assert selectedGitMacheteBranch.isPresent();

    IGitMacheteRepository macheteRepository = getMacheteRepository(anActionEvent);
    // TODO prohibit rebasing a root branch.
    // The line below is completely unsafe (will throw if `branchToRebase` is a root).
    Optional<IGitRebaseParameters> gitRebaseParameters = deriveGitRebaseOntoParentParameters(macheteRepository,
        selectedGitMacheteBranch.get().asNonRootBranch());
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

  private void updateIfVisibleDescription(BaseGitMacheteBranch branch, Presentation presentation) {
    if (presentation.isVisible()) {
      assert branch.getUpstreamBranch().isPresent();
      @SuppressWarnings("method.invocation.invalid")
      BaseGitMacheteBranch upstream = branch.getUpstreamBranch().get();
      String description = String.format("Rebase \"%s\" onto \"%s\"", branch.getName(), upstream.getName());
      presentation.setDescription(description);
    }
  }

  private void prohibitRootBranchRebase(AnActionEvent anActionEvent) {
    IGitMacheteRepository gitMacheteRepository = getMacheteRepository(anActionEvent);
    Optional<BaseGitMacheteBranch> branchToRebase = getSelectedMacheteBranch(anActionEvent);

    var presentation = anActionEvent.getPresentation();
    if (presentation.isVisible() && branchToRebase.isPresent()) {
      if (gitMacheteRepository.getRootBranches().contains(branchToRebase.get())) {
        presentation.setEnabled(false);
        presentation.setVisible(false);
      }
    }
  }

  private Optional<BaseGitMacheteBranch> getSelectedMacheteBranch(AnActionEvent anActionEvent) {
    IGitMacheteRepository gitMacheteRepository = getMacheteRepository(anActionEvent);
    String selectedBranchName = anActionEvent.getData(DataKeys.KEY_SELECTED_BRANCH_NAME);
    assert selectedBranchName != null : "Can't get selected branch";
    return gitMacheteRepository.getBranchByName(selectedBranchName);
  }
}
