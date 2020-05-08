package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getCurrentMacheteNonRootBranch;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getGitMacheteRepository;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.common.BaseRebaseBranchOntoParentAction;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 *  <li>{@link DataKeys#KEY_SELECTED_VCS_REPOSITORY}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public class RebaseCurrentBranchOntoParentAction extends BaseRebaseBranchOntoParentAction {
  public static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    Presentation presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    var currentBranch = getGitMacheteRepository(anActionEvent)
        .flatMap(repository -> repository.getCurrentBranchIfManaged());

    if (currentBranch.isEmpty()) {
      presentation.setDescription("Current revision is not a branch managed by Git Machete");
      presentation.setEnabled(false);

    } else if (currentBranch.get().isRootBranch()) {
      presentation.setDescription("Can't rebase root branch '${currentBranch.get().getName()}'");
      presentation.setEnabled(false);

    } else if (currentBranch.get().asNonRootBranch().getSyncToParentStatus().equals(SyncToParentStatus.Merged)) {
      presentation.setEnabled(false);
      presentation.setDescription("Can't rebase merged branch '${currentBranch.get().getName()}'");

    } else if (currentBranch.get().isNonRootBranch()) {
      var upstreamBranch = currentBranch.get().asNonRootBranch().getUpstreamBranch();
      presentation.setDescription("Rebase '${currentBranch.get().getName()}' onto '${upstreamBranch.getName()}'");
    }
  }

  @Override
  public void actionPerformed(AnActionEvent anActionEvent) {
    LOG.debug("Performing");
    Option<BaseGitMacheteNonRootBranch> currentNonRootBranch = getCurrentMacheteNonRootBranch(anActionEvent);
    if (currentNonRootBranch.isDefined()) {
      doRebase(anActionEvent, currentNonRootBranch.get());
    } else {
      LOG.warn("Skipping the action because current non root branch is undefined");
    }
  }
}
