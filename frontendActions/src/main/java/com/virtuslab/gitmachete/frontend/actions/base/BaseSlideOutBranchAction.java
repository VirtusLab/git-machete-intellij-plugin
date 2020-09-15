package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getQuotedStringOrCurrent;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils.getMacheteFilePath;

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
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.logger.IEnhancedLambdaLogger;
import com.virtuslab.qual.guieffect.NotUIThreadSafe;

@CustomLog
public abstract class BaseSlideOutBranchAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyGitMacheteRepository {

  private static final String DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY = "machete.slideOut.deleteLocalBranch";

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);

    Presentation presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    var branchName = getNameOfBranchUnderAction(anActionEvent).getOrNull();
    var branch = branchName != null
        ? getManagedBranchByName(anActionEvent, branchName).getOrNull()
        : null;

    if (branch == null) {
      presentation.setEnabled(false);
      presentation.setDescription(format(getString("action.GitMachete.description.disabled.undefined.machete-branch"),
          "Slide out", getQuotedStringOrCurrent(branchName)));
    } else {
      presentation.setDescription(
          format(getString("action.GitMachete.BaseSlideOutBranchAction.description"), branch.getName()));
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    LOG.debug("Performing");

    var branchName = getNameOfBranchUnderAction(anActionEvent);
    var branch = branchName.flatMap(bn -> getManagedBranchByName(anActionEvent, bn));
    if (branch.isDefined()) {
      doSlideOut(anActionEvent, branch.get());
    }
  }

  @UIEffect
  private void doSlideOut(AnActionEvent anActionEvent, IManagedBranchSnapshot branchToSlideOut) {
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

    } catch (BranchLayoutException e) {
      String exceptionMessage = e.getMessage();
      String errorMessage = "Error occurred while sliding out '${branchName}' branch" +
          (exceptionMessage == null ? "" : ": " + exceptionMessage);
      LOG.error(errorMessage);
      VcsNotifier.getInstance(project).notifyError(
          format(getString("action.GitMachete.BaseSlideOutBranchAction.notification.title.slide-out-fail"), branchName),
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

  @NotUIThreadSafe
  private void deleteBranchIfRequired(AnActionEvent anActionEvent, String branchName) {
    var gitRepository = getSelectedGitRepository(anActionEvent);

    if (gitRepository.isDefined()) {
      var root = gitRepository.get().getRoot();
      var project = getProject(anActionEvent);
      var shallDeleteLocalBranch = getDeleteLocalBranchOnSlideOutGitConfigValue(project, root);
      if (shallDeleteLocalBranch) {
        var slidOutBranchIsCurrent = getCurrentBranchNameIfManaged(anActionEvent)
            .map(b -> b.equals(branchName))
            .getOrElse(true);
        if (slidOutBranchIsCurrent) {
          LOG.debug("Skipping local branch deletion because it is equal to current branch");
          getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
          VcsNotifier.getInstance(project)
              .notifySuccess(
                  format(
                      getString("action.GitMachete.BaseSlideOutBranchAction.notification.title.slide-out-success.of-current"),
                      branchName));
          return;
        }

        GitBrancher.getInstance(project).deleteBranch(branchName, gitRepository.toJavaList());
        VcsNotifier.getInstance(project)
            .notifySuccess(
                format(getString("action.GitMachete.BaseSlideOutBranchAction.notification.title.slide-out-success.with-delete"),
                    branchName));
        return; // repository update invoked via GIT_REPO_CHANGE topic
      } else {
        VcsNotifier.getInstance(project)
            .notifySuccess(
                format(
                    getString("action.GitMachete.BaseSlideOutBranchAction.notification.title.slide-out-success.without-delete"),
                    branchName));
      }
    }
    getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
  }

  @NotUIThreadSafe
  private boolean getDeleteLocalBranchOnSlideOutGitConfigValue(Project project, VirtualFile root) {
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
