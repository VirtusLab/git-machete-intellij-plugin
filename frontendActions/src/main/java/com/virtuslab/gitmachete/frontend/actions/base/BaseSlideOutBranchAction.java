package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils.getMacheteFilePath;
import static java.text.MessageFormat.format;

import java.nio.file.Path;

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
  public void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);

    Presentation presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    var branchName = getNameOfBranchUnderAction(anActionEvent);
    var branch = branchName.flatMap(bn -> getGitMacheteBranchByName(anActionEvent, bn));

    if (branch.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(
          format(getString("action.GitMachete.description.disabled.undefined.machete-branch"), "Slide out"));
    } else if (branch.get().isNonRootBranch()) {
      presentation.setDescription(
          format(getString("action.GitMachete.BaseSlideOutBranchAction.description"), branch.get().getName()));

      if (getCurrentBranchNameIfManaged(anActionEvent).equals(branchName)) {
        presentation.setText(getString("action.GitMachete.BaseSlideOutBranchBelowAction.text.current-branch"));
      }
    } else {
      if (anActionEvent.getPlace().equals(ActionPlaces.ACTION_PLACE_TOOLBAR)) {
        presentation.setEnabled(false);
        presentation.setDescription(
            format(getString("action.GitMachete.BaseSlideOutBranchAction.description.root.branch"), branch.get().getName()));
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
    var branchLayoutWriter = getBranchLayoutWriter(anActionEvent);
    var gitRepository = getSelectedGitRepository(anActionEvent).getOrNull();
    var branchLayout = getBranchLayout(anActionEvent).getOrNull();
    if (branchLayout == null || gitRepository == null) {
      return;
    }

    try {
      LOG.info("Sliding out '${branchName}' branch in memory");
      var newBranchLayout = branchLayout.slideOut(branchName);

      Path macheteFilePath = getMacheteFilePath(gitRepository);
      LOG.info("Writing new branch layout into ${macheteFilePath}");
      branchLayoutWriter.write(macheteFilePath, newBranchLayout, /* backupOldLayout */ true);
      VcsNotifier.getInstance(project)
          .notifySuccess(format(getString("action.GitMachete.BaseSlideOutBranchAction.notification.success"), branchName));

    } catch (BranchLayoutException e) {
      String exceptionMessage = e.getMessage();
      String errorMessage = "Error occurred while sliding out '${branchName}' branch" +
          (exceptionMessage == null ? "" : ": " + exceptionMessage);
      LOG.error(errorMessage);
      VcsNotifier.getInstance(project).notifyError(
          format(getString("action.GitMachete.BaseSlideOutBranchAction.notification.fail"), branchName),
          exceptionMessage == null ? "" : exceptionMessage);
    }

    LOG.debug("Refreshing repository state");
    new Task.Backgroundable(project, "Deleting branch if required...") {
      @Override
      public void run(ProgressIndicator indicator) {
        deleteBranchIfRequired(anActionEvent, branchName);
      }
    }.queue();
  }

  /**
   * This method must NOT be called on the UI thread.
   */
  private void deleteBranchIfRequired(AnActionEvent anActionEvent, String branchName) {
    var selectedVcsRepository = getSelectedGitRepository(anActionEvent);

    if (selectedVcsRepository.isDefined()) {
      var root = selectedVcsRepository.get().getRoot();
      var project = getProject(anActionEvent);
      var shallDeleteLocalBranch = getDeleteLocalBranchOnSlideOutGitConfigKeyValue(project, root);
      if (shallDeleteLocalBranch) {
        var slidOutBranchIsCurrent = getCurrentBranchNameIfManaged(anActionEvent)
            .map(b -> b.equals(branchName))
            .getOrElse(true);
        if (slidOutBranchIsCurrent) {
          LOG.debug("Skipping local branch deletion because it is equal to current branch");
          getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
          return;
        }

        GitBrancher.getInstance(project).deleteBranch(branchName, selectedVcsRepository.toJavaList());
        return; // repository update invoked via GIT_REPO_CHANGE topic
      }
    }
    getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
  }

  /**
   * This method must NOT be called on the UI thread.
   */
  private boolean getDeleteLocalBranchOnSlideOutGitConfigKeyValue(Project project, VirtualFile root) {
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
