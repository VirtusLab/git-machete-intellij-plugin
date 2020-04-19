package com.virtuslab.gitmachete.frontend.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.keys.DataKeys;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 *  <li>{@link DataKeys#KEY_IS_GIT_MACHETE_REPOSITORY_READY}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public class RebaseCurrentBranchOntoParentAction extends BaseRebaseBranchOntoParentAction {
  private static final String ACTION_TEXT = "Rebase Current Branch Onto Parent";
  private static final String ACTION_DESCRIPTION = "Rebase current branch onto parent";

  /**
   * This action "construction" happens here (not within plugin.xml, as in the case of {@link RebaseSelectedBranchOntoParentAction})
   * because declaration of such GUI elements (the button in this case) is apparently less obvious than a context menu option.
   */
  public RebaseCurrentBranchOntoParentAction() {
    super(ACTION_TEXT, ACTION_DESCRIPTION, AllIcons.Actions.Menu_cut);
  }

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    Presentation presentation = anActionEvent.getPresentation();
    if (presentation.isEnabledAndVisible()) {
      IGitMacheteRepository gitMacheteRepository = getMacheteRepository(anActionEvent);

      var currentBranchOption = gitMacheteRepository.getCurrentBranchIfManaged();

      if (currentBranchOption.isEmpty()) {
        presentation.setDescription("Current revision is not a branch managed by Git Machete");
        presentation.setEnabled(false);

      } else if (currentBranchOption.get().isRootBranch()) {
        presentation.setDescription("Can't rebase git machete root branch '${currentBranchOption.get().getName()}'");
        presentation.setEnabled(false);

      } else {
        var upstreamBranch = currentBranchOption.get().asNonRootBranch().getUpstreamBranch();
        presentation.setDescription("Rebase '${currentBranchOption.get().getName()}' onto ${upstreamBranch.getName()}");
      }
    }
  }

  /**
   * Assumption to the following code is that the result of {@link com.virtuslab.gitmachete.backend.api.IGitMacheteRepository#getCurrentBranchIfManaged}
   * is present and it is not a root branch because if it was not the user wouldn't be able to perform action in the first place
   */
  @Override
  public void actionPerformed(AnActionEvent anActionEvent) {
    Option<BaseGitMacheteBranch> currentBranch = getMacheteRepository(anActionEvent).getCurrentBranchIfManaged();
    assert currentBranch.isDefined();

    var branchToRebase = currentBranch.get().asNonRootBranch();
    doRebase(anActionEvent, branchToRebase);
  }
}
