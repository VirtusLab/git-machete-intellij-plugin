package com.virtuslab.gitmachete.frontend.actions;

import java.util.Optional;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;

/**
 * Expects DataKeys:
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 */
public class RebaseCurrentBranchOntoParentAction extends BaseRebaseBranchOntoParentAction {
  private static final String ACTION_TEXT = "Rebase Current Branch Onto Parent";
  private static final String ACTION_DESCRIPTION = "Rebase current branch onto parent";

  /**
   * This action "construction" happens here (not like RebaseSelectedBranchOntoParentAction within plugin.xml)
   * because declaration of such a GUI elements (the button in this case) is apparently less obvious than a context menu option.
   */
  public RebaseCurrentBranchOntoParentAction() {
    super(ACTION_TEXT, ACTION_DESCRIPTION, AllIcons.Actions.Menu_cut);
  }

  @Override
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    prohibitRebaseOfNonManagedRevisionOrRootBranch(anActionEvent);
    updateDescriptionIfApplicable(anActionEvent);
  }

  /**
   * Assumption to following code:
   * - the result of {@link com.virtuslab.gitmachete.backend.api.IGitMacheteRepository#getCurrentBranchIfManaged}
   * is present and it is not a root branch because if it was not the user wouldn't be able to perform action in the first place
   */
  @Override
  public void actionPerformed(AnActionEvent anActionEvent) {
    Optional<BaseGitMacheteBranch> currentBranch = getMacheteRepository(anActionEvent).getCurrentBranchIfManaged();
    assert currentBranch.isPresent();

    var branchToRebase = currentBranch.get().asNonRootBranch();
    doRebase(anActionEvent, branchToRebase);
  }

  private void updateDescriptionIfApplicable(AnActionEvent anActionEvent) {
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
