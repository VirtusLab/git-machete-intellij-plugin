package com.virtuslab.gitmachete.frontend.actions.contextmenu;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getSelectedMacheteBranch;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
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

    Presentation presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    Option<BaseGitMacheteBranch> selectedBranch = getSelectedMacheteBranch(anActionEvent);

    if (selectedBranch.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription("Slide out disabled due to undefined selected branch");

    } else if (selectedBranch.get().isNonRootBranch()) {
      var nonRootBranch = selectedBranch.get().asNonRootBranch();
      presentation.setDescription("Slide out '${nonRootBranch.getName()}'");

    } else {
      // in case of root branch we do not want to show this option at all
      presentation.setEnabledAndVisible(false);
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
