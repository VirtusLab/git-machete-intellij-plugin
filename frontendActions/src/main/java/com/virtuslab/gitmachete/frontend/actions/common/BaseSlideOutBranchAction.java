package com.virtuslab.gitmachete.frontend.actions.common;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getBranchLayout;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getBranchLayoutWriter;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getGitMacheteBranchByName;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getGraphTable;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getProject;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.vcs.VcsNotifier;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.frontend.actionids.ActionPlaces;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.logger.EnhancedLambdaLoggerFactory;
import com.virtuslab.logger.IEnhancedLambdaLogger;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_BRANCH_LAYOUT_WRITER}</li>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public abstract class BaseSlideOutBranchAction extends GitMacheteRepositoryReadyAction implements IBranchNameProvider {
  private static final IEnhancedLambdaLogger LOG = EnhancedLambdaLoggerFactory.create();

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    Presentation presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    var branchName = getNameOfBranchUnderAction(anActionEvent);
    var branch = branchName.flatMap(bn -> getGitMacheteBranchByName(anActionEvent, bn));

    if (branch.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription("Slide out disabled due to undefined branch");

    } else if (branch.get().isNonRootBranch()) {
      var nonRootBranch = branch.get().asNonRootBranch();
      presentation.setDescription("Slide out '${branchName}'");

    } else {

      if (anActionEvent.getPlace().equals(ActionPlaces.ACTION_PLACE_TOOLBAR)) {
        presentation.setEnabled(false);
        presentation.setDescription("Root branch '${branchName}' cannot be slid out");
      } else { //contextmenu
        // in case of root branch we do not want to show this option at all
        presentation.setEnabledAndVisible(false);
      }
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    LOG.debug("Performing");

    var branchName = getNameOfBranchUnderAction(anActionEvent);
    var branch = branchName.flatMap(bn -> getGitMacheteBranchByName(anActionEvent, bn));
    if (branch.isDefined() && branch.get().isNonRootBranch()) {
      doSlideOut(anActionEvent, branch.get().asNonRootBranch());
    } else {
      LOG.warn("Skipping the action because the branch is undefined or is a root branch: branch='${branch}'");
    }
  }

  @UIEffect
  private void doSlideOut(AnActionEvent anActionEvent, IGitMacheteNonRootBranch branchToSlideOut) {
    LOG.debug(() -> "Entering: branchToSlideOut = ${branchToSlideOut}");
    String branchName = branchToSlideOut.getName();
    var project = getProject(anActionEvent);
    var branchLayout = getBranchLayout(anActionEvent);
    var branchLayoutWriter = getBranchLayoutWriter(anActionEvent);
    if (branchLayout.isEmpty()) {
      LOG.warn("Skipping the action because branch layout is undefined");
      return;
    }

    try {
      LOG.info("Sliding out '${branchName}' branch in memory");
      var newBranchLayout = branchLayout.get().slideOut(branchName);

      LOG.info("Writing new branch layout into file");
      branchLayoutWriter.write(newBranchLayout, /* backupOldLayout */ true);

      LOG.debug("Refreshing repository state");
      getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
      VcsNotifier.getInstance(project).notifySuccess("Branch <b>${branchName}</b> slid out");
    } catch (BranchLayoutException e) {
      String exceptionMessage = e.getMessage();
      String errorMessage = "Error occurred while sliding out '${branchName}' branch" +
          (exceptionMessage == null ? "" : ": " + exceptionMessage);
      LOG.error(errorMessage);
      VcsNotifier.getInstance(project).notifyError("Slide out of <b>${branchName}</b> failed",
          exceptionMessage == null ? "" : exceptionMessage);
    }
  }
}
