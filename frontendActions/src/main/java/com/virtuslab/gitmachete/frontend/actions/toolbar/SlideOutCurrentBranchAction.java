package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getCurrentMacheteNonRootBranch;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getGitMacheteRepository;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.frontend.actions.common.BaseSlideOutBranchAction;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_BRANCH_LAYOUT}</li>
 *  <li>{@link DataKeys#KEY_BRANCH_LAYOUT_WRITER}</li>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public class SlideOutCurrentBranchAction extends BaseSlideOutBranchAction {
  public static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    Presentation presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    var currentBranch = getGitMacheteRepository(anActionEvent).flatMap(repository -> repository.getCurrentBranchIfManaged());

    if (currentBranch.isEmpty()) {
      presentation.setDescription("Current revision is not a branch managed by Git Machete");
      presentation.setEnabled(false);

    } else if (currentBranch.get().isRootBranch()) {
      presentation.setDescription("Can't slide out root branch '${currentBranch.get().getName()}'");
      presentation.setEnabled(false);

    } else {
      presentation.setDescription("Slide out '${currentBranch.get().getName()}'");
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    LOG.debug("Performing");
    Option<BaseGitMacheteNonRootBranch> currentNonRootBranch = getCurrentMacheteNonRootBranch(anActionEvent);
    if (currentNonRootBranch.isDefined()) {
      doSlideOut(anActionEvent, currentNonRootBranch.get());
    } else {
      LOG.warn("Skipping the action because current non-root branch is undefined");
    }
  }
}
