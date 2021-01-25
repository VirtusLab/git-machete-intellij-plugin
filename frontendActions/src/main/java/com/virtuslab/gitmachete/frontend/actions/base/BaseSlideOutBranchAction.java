package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getQuotedStringOrCurrent;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils.getMacheteFilePath;

import java.nio.file.Path;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.GuiUtils;
import git4idea.branch.GitBrancher;
import git4idea.config.GitConfigUtil;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.frontend.actions.dialogs.DeleteBranchOnSlideOutSuggestionDialog;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.logger.IEnhancedLambdaLogger;
import com.virtuslab.qual.guieffect.NotUIThreadSafe;

@CustomLog
public abstract class BaseSlideOutBranchAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyGitMacheteRepository {

  public static final String DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY = "machete.slideOut.deleteLocalBranch";

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
    var gitRepository = getSelectedGitRepository(anActionEvent).getOrNull();
    var project = getProject(anActionEvent);
    var slidOutBranchIsCurrent = getCurrentBranchNameIfManaged(anActionEvent)
        .map(b -> b.equals(branchName))
        .getOrElse(true);

    if (slidOutBranchIsCurrent) {
      LOG.debug("Skipping (optional) local branch deletion because it is equal to current branch");
      getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
      VcsNotifier.getInstance(project)
          .notifySuccess(
              format(
                  getString("action.GitMachete.BaseSlideOutBranchAction.notification.title.slide-out-success.of-current"),
                  branchName));

    } else if (gitRepository != null) {
      var root = gitRepository.getRoot();
      var configValueOption = getDeleteLocalBranchOnSlideOutGitConfigValue(project, root);
      if (configValueOption.isEmpty()) {
        GuiUtils.invokeLaterIfNeeded(
            () -> suggestBranchDeletion(anActionEvent, branchName, gitRepository, project),
            ModalityState.NON_MODAL);
      } else if (configValueOption.isDefined()) {
        var decision = configValueOption.get();
        handleBranchDeletionDecision(project, branchName, gitRepository, anActionEvent, decision);
      }

    } else {
      getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
    }
  }

  @UIEffect
  private void suggestBranchDeletion(AnActionEvent anActionEvent, String branchName, GitRepository gitRepository,
      Project project) {
    var slideOutOptions = new DeleteBranchOnSlideOutSuggestionDialog(project).showAndGetSlideOutOptions();

    new Task.Backgroundable(project, "Deleting branch if required...") {
      @Override
      public void run(ProgressIndicator indicator) {
        if (slideOutOptions != null) {
          handleBranchDeletionDecision(project, branchName, gitRepository, anActionEvent, slideOutOptions.shouldDelete());
        } else {
          VcsNotifier.getInstance(project)
              .notifyInfo(
                  format(
                      getString(
                          "action.GitMachete.BaseSlideOutBranchAction.notification.title.slide-out-info.canceled"),
                      branchName));
        }
      }
    }.queue();

    if (slideOutOptions != null && slideOutOptions.shouldRemember()) {
      var value = String.valueOf(slideOutOptions.shouldDelete());
      setDeleteLocalBranchOnSlideOutGitConfigValue(project, gitRepository.getRoot(), value);
    }
  }

  private void slideOutBranch(AnActionEvent anActionEvent, String branchName) {
    var project = getProject(anActionEvent);
    var branchLayout = getBranchLayout(anActionEvent).getOrNull();
    var branchLayoutWriter = getBranchLayoutWriter(anActionEvent);
    var gitRepository = getSelectedGitRepository(anActionEvent).getOrNull();
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
  }

  private void handleBranchDeletionDecision(Project project, String branchName, GitRepository gitRepository,
      AnActionEvent anActionEvent, boolean decision) {
    slideOutBranch(anActionEvent, branchName);
    if (decision) {
      GitBrancher.getInstance(project).deleteBranch(branchName, java.util.List.of(gitRepository));
      VcsNotifier.getInstance(project)
          .notifySuccess(
              format(
                  getString("action.GitMachete.BaseSlideOutBranchAction.notification.title.slide-out-success.with-delete"),
                  branchName));
      return;
    } else {
      VcsNotifier.getInstance(project)
          .notifySuccess(
              format(
                  getString(
                      "action.GitMachete.BaseSlideOutBranchAction.notification.title.slide-out-success.without-delete"),
                  branchName));
    }
    getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
  }

  @NotUIThreadSafe
  private Option<Boolean> getDeleteLocalBranchOnSlideOutGitConfigValue(Project project, VirtualFile root) {
    try {
      ThrowableComputable<@Nullable String, VcsException> computable = () -> GitConfigUtil.getValue(project, root,
          DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY);
      var value = computable.compute();
      if (value != null) {
        Boolean booleanValue = GitConfigUtil.getBooleanValue(value);
        return Option.of(booleanValue != null && booleanValue);
      }
    } catch (VcsException e) {
      LOG.info(
          "Attempt to get '${DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY}' git config value failed: key may not exist");
    }

    return Option.none();
  }

  @NotUIThreadSafe
  private void setDeleteLocalBranchOnSlideOutGitConfigValue(Project project, VirtualFile root, String value) {
    try {
      var additionalParameters = "--local";
      GitConfigUtil.setValue(project, root, DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY, value, additionalParameters);
    } catch (VcsException e) {
      LOG.error("Attempt to set '${DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY}' git config value failed");
    }
  }
}
