package com.virtuslab.gitmachete.frontend.actions.base;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.vcs.VcsNotifier;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyBranchLayoutWriter;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.gitmachete.frontend.ui.providerservice.BranchLayoutWriterProvider;
import com.virtuslab.gitmachete.frontend.ui.providerservice.GraphTableProvider;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;

@CustomLog
public abstract class BaseSlideOutBranchAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyGitMacheteRepository,
      IExpectsKeyProject {

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
      presentation.setDescription("Slide out '${branch.get().getName()}'");
    } else {
      if (anActionEvent.getPlace().equals(ActionPlaces.ACTION_PLACE_TOOLBAR)) {
        presentation.setEnabled(false);
        presentation.setDescription("Root branch '${branch.get().getName()}' cannot be slid out");
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
    var branchLayoutWriter = project.getService(BranchLayoutWriterProvider.class).getBranchLayoutWriter();
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
      project.getService(GraphTableProvider.class).getGraphTable().queueRepositoryUpdateAndModelRefresh();
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
