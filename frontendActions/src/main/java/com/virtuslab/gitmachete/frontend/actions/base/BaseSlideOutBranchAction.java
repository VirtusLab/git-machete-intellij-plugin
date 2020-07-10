package com.virtuslab.gitmachete.frontend.actions.base;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.branch.GitBrancher;
import git4idea.config.GitConfigUtil;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public abstract class BaseSlideOutBranchAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyGitMacheteRepository,
      IExpectsKeyProject {

  private static final String DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY = "machete.slideOut.deleteLocalBranch";

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }

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
    if (branch.isDefined()) {
      if (branch.get().isNonRootBranch()) {
        doSlideOut(anActionEvent, branch.get().asNonRootBranch());
      } else {
        LOG.warn("Skipping the action because the branch '${branch.get().getName()}' is a root branch");
      }
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
      return;
    }

    try {
      LOG.info("Sliding out '${branchName}' branch in memory");
      var newBranchLayout = branchLayout.get().slideOut(branchName);

      LOG.info("Writing new branch layout into file");
      branchLayoutWriter.write(newBranchLayout, /* backupOldLayout */ true);
      VcsNotifier.getInstance(project).notifySuccess("Branch <b>${branchName}</b> slid out");

    } catch (BranchLayoutException e) {
      String exceptionMessage = e.getMessage();
      String errorMessage = "Error occurred while sliding out '${branchName}' branch" +
          (exceptionMessage == null ? "" : ": " + exceptionMessage);
      LOG.error(errorMessage);
      VcsNotifier.getInstance(project).notifyError("Slide out of <b>${branchName}</b> failed",
          exceptionMessage == null ? "" : exceptionMessage);
    }

    LOG.debug("Refreshing repository state");
    new Task.Backgroundable(project, "Deleting branch if required...") {
      @Override
      public void run(ProgressIndicator indicator) {
        deleteBranchIfRequired(anActionEvent, branchName);
      }

      @Override
      public void onFinished() {
        getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
      }
    }.queue();
  }

  private void deleteBranchIfRequired(AnActionEvent anActionEvent, String branchName) {
    var selectedVcsRepository = getSelectedGitRepository(anActionEvent);

    if (selectedVcsRepository.isDefined()) {
      var root = selectedVcsRepository.get().getRoot();
      var project = getProject(anActionEvent);
      var shallDeleteLocalBranch = getDeleteLocalBranchOnSlideOutGitConfigKeyValue(root, project);
      if (shallDeleteLocalBranch) {
        var slidOutBranchIsCurrent = getCurrentBranchNameIfManaged(anActionEvent)
            .map(b -> b.equals(branchName))
            .getOrElse(true);
        if (slidOutBranchIsCurrent) {
          LOG.debug("Skipping local branch deletion because it is equal to current branch");
          return;
        }

        GitBrancher.getInstance(project).deleteBranch(branchName, selectedVcsRepository.toJavaList());
      }
    }
  }

  private boolean getDeleteLocalBranchOnSlideOutGitConfigKeyValue(VirtualFile root, Project project) {
    try {
      ThrowableComputable<@Nullable String, VcsException> computable = () -> GitConfigUtil.getValue(project, root,
          DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY);
      var value = computable.compute();
      boolean result = false;
      if (value != null) {
        Boolean booleanValue = GitConfigUtil.getBooleanValue(value);
        result = booleanValue != null && booleanValue;
      }
      return result;
    } catch (VcsException e) {
      LOG.info(
          "Attempt to get '${DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY}' git config value failed: key may not exist");
    }

    return false;
  }
}
