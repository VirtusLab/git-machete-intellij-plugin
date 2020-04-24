package com.virtuslab.gitmachete.frontend.actions;

import static com.virtuslab.gitmachete.frontend.actions.ActionUtils.getSelectedMacheteBranch;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import io.vavr.control.Option;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 *  <li>{@link DataKeys#KEY_IS_GIT_MACHETE_REPOSITORY_READY}</li>
 *  <li>{@link DataKeys#KEY_SELECTED_BRANCH_NAME}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public class RebaseSelectedBranchOntoParentAction extends BaseRebaseBranchOntoParentAction {
  public static final LambdaLogger LOG = LambdaLoggerFactory.getLogger("frontendActions");

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    var presentation = anActionEvent.getPresentation();
    if (presentation.isVisible()) {
      Option<BaseGitMacheteBranch> selectedBranch = getSelectedMacheteBranch(anActionEvent);
      if (selectedBranch.isDefined()) {
        if (selectedBranch.get().isRootBranch()) {
          presentation.setEnabled(false);
          presentation.setVisible(false);
        } else {
          var nonRootBranch = selectedBranch.get().asNonRootBranch();
          BaseGitMacheteBranch upstream = nonRootBranch.getUpstreamBranch();
          presentation.setDescription("Rebase '${nonRootBranch.getName()}' onto '${upstream.getName()}'");
        }
      } else {
        presentation.setEnabled(false);
        presentation.setVisible(false);
      }
    }
  }

  /**
   * Assumption to the following code is that the result of {@link ActionUtils#getSelectedMacheteBranch}
   * is present and it is not a root branch because if it was not the user wouldn't be able to perform action in the first place
   */
  @Override
  public void actionPerformed(AnActionEvent anActionEvent) {
    LOG.debug(() -> "Performing ${getClass().getSimpleName()}");
    var selectedGitMacheteBranchOption = getSelectedMacheteBranch(anActionEvent);
    assert selectedGitMacheteBranchOption.isDefined() : "Can't get selected branch";
    var baseGitMacheteBranch = selectedGitMacheteBranchOption.get();
    assert !baseGitMacheteBranch.isRootBranch() : "Selected branch is a root branch";

    var branchToRebase = baseGitMacheteBranch.asNonRootBranch();
    doRebase(anActionEvent, branchToRebase);
  }
}
