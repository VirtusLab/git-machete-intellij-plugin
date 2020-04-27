package com.virtuslab.gitmachete.frontend.actions;

import static com.virtuslab.gitmachete.frontend.actions.ActionUtils.getCurrentBaseMacheteNonRootBranch;
import static com.virtuslab.gitmachete.frontend.actions.ActionUtils.getPresentMacheteRepository;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
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
  public static final LambdaLogger LOG = LambdaLoggerFactory.getLogger("frontendActions");

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
      IGitMacheteRepository gitMacheteRepository = getPresentMacheteRepository(anActionEvent);

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
   * is present and it is not a root branch because otherwise the user wouldn't be able to perform action in the first place
   */
  @Override
  public void actionPerformed(AnActionEvent anActionEvent) {
    LOG.debug(() -> "Performing ${getClass().getSimpleName()}");
    BaseGitMacheteNonRootBranch baseGitMacheteBranch = getCurrentBaseMacheteNonRootBranch(anActionEvent);
    doRebase(anActionEvent, baseGitMacheteBranch);
  }
}
