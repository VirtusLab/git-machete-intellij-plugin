package com.virtuslab.gitmachete.frontend.actions.contextmenu;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getSelectedMacheteBranch;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.common.BaseRebaseBranchOntoParentAction;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 *  <li>{@link DataKeys#KEY_SELECTED_BRANCH_NAME}</li>
 *  <li>{@link DataKeys#KEY_SELECTED_VCS_REPOSITORY}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public class RebaseSelectedBranchOntoParentAction extends BaseRebaseBranchOntoParentAction {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    Presentation presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    Option<BaseGitMacheteBranch> selectedBranch = getSelectedMacheteBranch(anActionEvent);

    if (selectedBranch.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription("Rebase disabled due to undefined selected branch");

    } else if (selectedBranch.get().isRootBranch()) {
      // in case of root branch we do not want to show this option at all
      presentation.setEnabledAndVisible(false);

    } else if (selectedBranch.get().asNonRootBranch().getSyncToParentStatus().equals(SyncToParentStatus.Merged)) {
      presentation.setEnabled(false);
      presentation.setDescription("Can't rebase merged branch '${selectedBranch.get().getName()}'");

    } else if (selectedBranch.get().isNonRootBranch()) {
      var nonRootBranch = selectedBranch.get().asNonRootBranch();
      BaseGitMacheteBranch upstream = nonRootBranch.getUpstreamBranch();
      presentation.setDescription("Rebase '${nonRootBranch.getName()}' onto '${upstream.getName()}'");
    }
  }

  @Override
  public void actionPerformed(AnActionEvent anActionEvent) {
    LOG.debug("Performing");

    Option<BaseGitMacheteBranch> selectedBranch = getSelectedMacheteBranch(anActionEvent);
    if (selectedBranch.isDefined() && selectedBranch.get().isNonRootBranch()) {
      doRebase(anActionEvent, selectedBranch.get().asNonRootBranch());
    } else {
      LOG.warn("Skipping the action because selected branch is undefined or is a root branch: " +
          "selectedBranch='${selectedBranch}'");
    }
  }
}
