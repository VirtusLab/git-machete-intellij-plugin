package com.virtuslab.gitmachete.frontend.actions;

import static com.virtuslab.gitmachete.frontend.actions.ActionUtils.getCurrentBaseMacheteNonRootBranch;
import static com.virtuslab.gitmachete.frontend.actions.ActionUtils.getPresentMacheteRepository;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.keys.DataKeys;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_BRANCH_LAYOUT}</li>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_FILE_PATH}</li>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 *  <li>{@link DataKeys#KEY_IS_GIT_MACHETE_REPOSITORY_READY}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public class SlideOutCurrentBranchAction extends BaseSlideOutBranchAction {
  public static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");

  private static final String ACTION_TEXT = "Slide Out Current Branch";
  private static final String ACTION_DESCRIPTION = "Slide out current branch";

  public SlideOutCurrentBranchAction() {
    super(ACTION_TEXT, ACTION_DESCRIPTION, AllIcons.Actions.GC);
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
        presentation.setDescription("Can't slide out git machete root branch '${currentBranchOption.get().getName()}'");
        presentation.setEnabled(false);

      } else {
        presentation.setDescription("Slide out '${currentBranchOption.get().getName()}'");
      }
    }
  }

  /**
   * Assumption to the following code is that the result of {@link IGitMacheteRepository#getCurrentBranchIfManaged}
   * is present and it is not a root branch because otherwise the user wouldn't be able to perform action in the first place
   */
  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    LOG.debug(() -> "Performing ${getClass().getSimpleName()}");
    BaseGitMacheteNonRootBranch baseGitMacheteBranch = getCurrentBaseMacheteNonRootBranch(anActionEvent);
    doSlideOut(anActionEvent, baseGitMacheteBranch);
  }
}
