package com.virtuslab.gitmachete.frontend.actions.common;

import static com.virtuslab.gitmachete.frontend.actionids.ActionIds.ACTION_REFRESH;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getBranchLayout;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getBranchLayoutWriter;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getProject;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.ui.GuiUtils;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
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
public abstract class BaseSlideOutBranchAction extends GitMacheteRepositoryReadyAction {
  public static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");

  @UIEffect
  public void doSlideOut(AnActionEvent anActionEvent, BaseGitMacheteNonRootBranch branchToSlideOut) {
    LOG.debug(() -> "Entering: branchToSlideOut = ${branchToSlideOut}");
    String branchName = branchToSlideOut.getName();
    Project project = getProject(anActionEvent);
    var branchLayout = getBranchLayout(anActionEvent);
    var branchLayoutWriter = getBranchLayoutWriter(anActionEvent);
    if (branchLayout.isEmpty() || branchLayoutWriter.isEmpty()) {
      LOG.warn("Skipping the action because branch layout and/or branch layout writer is undefined");
      return;
    }

    try {
      LOG.info("Sliding out '${branchName}' branch in memory");
      var newBranchLayout = branchLayout.get().slideOut(branchName);

      LOG.info("Writing new branch layout into file");
      branchLayoutWriter.get().write(newBranchLayout, /* backupOldLayout */ true);

      LOG.debug("Refreshing repository state");
      ActionManager.getInstance().getAction(ACTION_REFRESH).actionPerformed(anActionEvent);
      VcsNotifier.getInstance(project).notifySuccess("Branch <b>${branchName}</b> slid out");
    } catch (BranchLayoutException e) {
      String exceptionMessage = e.getMessage();
      String errorMessage = "Error occurred while sliding out '${branchName}' branch" +
          (exceptionMessage == null ? "" : ": " + exceptionMessage);
      LOG.error(errorMessage);
      VcsNotifier.getInstance(project).notifyError("Slide out of <b>${branchName}</b> failed",
          exceptionMessage == null ? "" : exceptionMessage);
      GuiUtils.invokeLaterIfNeeded(() -> Messages.showErrorDialog(errorMessage, "Something Went Wrong..."),
          ModalityState.NON_MODAL);
    }
  }
}
