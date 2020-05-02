package com.virtuslab.gitmachete.frontend.actions;

import static com.virtuslab.gitmachete.frontend.actions.ActionUtils.getSelectedMacheteBranch;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_BRANCH_LAYOUT}</li>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_FILE_PATH}</li>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 *  <li>{@link DataKeys#KEY_SELECTED_BRANCH_NAME}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public class SlideOutSelectedBranchAction extends BaseSlideOutBranchAction {
  public static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    var presentation = anActionEvent.getPresentation();
    if (presentation.isVisible()) {
      Option<BaseGitMacheteBranch> selectedBranch = getSelectedMacheteBranch(anActionEvent);
      if (selectedBranch.isDefined() && selectedBranch.get().isNonRootBranch()) {
        var nonRootBranch = selectedBranch.get().asNonRootBranch();
        presentation.setDescription("Slide out '${nonRootBranch.getName()}'");
      } else {
        presentation.setEnabled(false);
        presentation.setVisible(false);
      }
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    LOG.debug("Performing");

    var selectedBranch = getSelectedMacheteBranch(anActionEvent);
    if (selectedBranch.isDefined() && selectedBranch.get().isNonRootBranch()) {
      doSlideOut(anActionEvent, selectedBranch.get().asNonRootBranch());
    } else {
      LOG.warn("Skipping the action because selected branch is undefined or is a root branch: " +
          "selectedBranch='${selectedBranch}'");
    }
  }
}
