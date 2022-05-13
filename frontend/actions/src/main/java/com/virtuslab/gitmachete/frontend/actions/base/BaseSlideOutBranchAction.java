package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getQuotedStringOrCurrent;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils.getMacheteFilePath;

import java.nio.file.Path;
import java.util.Collections;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.branch.GitBrancher;
import git4idea.config.GitConfigUtil;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.frontend.actions.dialogs.DeleteBranchOnSlideOutSuggestionDialog;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.compat.UiThreadExecutionCompat;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
public abstract class BaseSlideOutBranchAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyGitMacheteRepository {

  public static final String DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY = "machete.slideOut.deleteLocalBranch";

  @Override
  public LambdaLogger log() {
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

    val branchName = getNameOfBranchUnderAction(anActionEvent).getOrNull();
    val branch = branchName != null
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

    val branchName = getNameOfBranchUnderAction(anActionEvent);
    val branch = branchName.flatMap(bn -> getManagedBranchByName(anActionEvent, bn));
    if (branch.isDefined()) {
      doSlideOut(anActionEvent, branch.get());
    }
  }

  @UIEffect
  private void doSlideOut(AnActionEvent anActionEvent, IManagedBranchSnapshot branchToSlideOut) {
    LOG.debug(() -> "Entering: branchToSlideOut = ${branchToSlideOut}");
    String branchName = branchToSlideOut.getName();
    val project = getProject(anActionEvent);

    LOG.debug("Refreshing repository state");
    new Task.Backgroundable(project, "Deleting branch if required...") {
      @Override
      @UIThreadUnsafe
      public void run(ProgressIndicator indicator) {
        deleteBranchIfRequired(anActionEvent, branchName);
      }
    }.queue();
  }

  @UIThreadUnsafe
  private void deleteBranchIfRequired(AnActionEvent anActionEvent, String branchName) {
    val gitRepository = getSelectedGitRepository(anActionEvent).getOrNull();
    val project = getProject(anActionEvent);
    val slidOutBranchIsCurrent = getCurrentBranchNameIfManaged(anActionEvent)
        .map(b -> b.equals(branchName))
        .getOrElse(false);

    if (slidOutBranchIsCurrent) {
      LOG.debug("Skipping (optional) local branch deletion because it is equal to current branch");
      slideOutBranch(anActionEvent, branchName);
      getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
      VcsNotifier.getInstance(project).notifySuccess(/* displayId */ null,
          /* title */ "",
          format(
              getString("action.GitMachete.BaseSlideOutBranchAction.notification.title.slide-out-success.of-current"),
              branchName));

    } else if (gitRepository != null) {
      val root = gitRepository.getRoot();
      val configValueOption = getDeleteLocalBranchOnSlideOutGitConfigValue(project, root);
      if (configValueOption.isEmpty()) {
        UiThreadExecutionCompat.invokeLaterIfNeeded(
            ModalityState.NON_MODAL,
            () -> suggestBranchDeletion(anActionEvent, branchName, gitRepository, project));
      } else if (configValueOption.isDefined()) {
        val shouldDelete = configValueOption.get();
        handleBranchDeletionDecision(project, branchName, gitRepository, anActionEvent, shouldDelete);
      }

    } else {
      getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
    }
  }

  @UIEffect
  private void suggestBranchDeletion(AnActionEvent anActionEvent, String branchName, GitRepository gitRepository,
      Project project) {
    val slideOutOptions = new DeleteBranchOnSlideOutSuggestionDialog(project, branchName).showAndGetSlideOutOptions();

    new Task.Backgroundable(project, "Deleting branch if required...") {
      @Override
      @UIThreadUnsafe
      public void run(ProgressIndicator indicator) {
        if (slideOutOptions != null) {
          handleBranchDeletionDecision(project, branchName, gitRepository, anActionEvent, slideOutOptions.shouldDelete());

          if (slideOutOptions.shouldRemember()) {
            val value = String.valueOf(slideOutOptions.shouldDelete());
            setDeleteLocalBranchOnSlideOutGitConfigValue(project, gitRepository.getRoot(), value);
          }
        } else {
          val title = getString("action.GitMachete.BaseSlideOutBranchAction.notification.title.slide-out-info.canceled");
          val message = format(
              getString("action.GitMachete.BaseSlideOutBranchAction.notification.message.slide-out-info.canceled"), branchName);
          VcsNotifier.getInstance(project).notifyInfo(/* displayId */ null, title, message);
        }
      }
    }.queue();
  }

  private void slideOutBranch(AnActionEvent anActionEvent, String branchName) {
    val project = getProject(anActionEvent);
    val branchLayout = getBranchLayout(anActionEvent).getOrNull();
    val branchLayoutWriter = getBranchLayoutWriter(anActionEvent);
    val gitRepository = getSelectedGitRepository(anActionEvent).getOrNull();
    if (branchLayout == null || gitRepository == null) {
      return;
    }

    LOG.info("Sliding out '${branchName}' branch in memory");
    val newBranchLayout = branchLayout.slideOut(branchName);

    try {
      Path macheteFilePath = getMacheteFilePath(gitRepository);
      LOG.info("Writing new branch layout into ${macheteFilePath}");
      branchLayoutWriter.write(macheteFilePath, newBranchLayout, /* backupOldLayout */ true);

    } catch (BranchLayoutException e) {
      String exceptionMessage = e.getMessage();
      String errorMessage = "Error occurred while sliding out '${branchName}' branch" +
          (exceptionMessage == null ? "" : ": " + exceptionMessage);
      LOG.error(errorMessage);
      VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
          format(getString("action.GitMachete.BaseSlideOutBranchAction.notification.title.slide-out-fail"), branchName),
          exceptionMessage == null ? "" : exceptionMessage);
    }
  }

  @UIThreadUnsafe
  private void handleBranchDeletionDecision(Project project, String branchName, GitRepository gitRepository,
      AnActionEvent anActionEvent, boolean shouldDelete) {
    slideOutBranch(anActionEvent, branchName);
    if (shouldDelete) {
      GitBrancher.getInstance(project).deleteBranch(branchName, Collections.singletonList(gitRepository));
      VcsNotifier.getInstance(project).notifySuccess(/* displayId */ null,
          /* title */ "",
          format(
              getString("action.GitMachete.BaseSlideOutBranchAction.notification.title.slide-out-success.with-delete"),
              branchName));
      return;
    } else {
      VcsNotifier.getInstance(project).notifySuccess(/* displayId */ null,
          /* title */ "",
          format(
              getString(
                  "action.GitMachete.BaseSlideOutBranchAction.notification.title.slide-out-success.without-delete"),
              branchName));
    }
    getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
  }

  @UIThreadUnsafe
  private Option<Boolean> getDeleteLocalBranchOnSlideOutGitConfigValue(Project project, VirtualFile root) {
    try {
      val value = GitConfigUtil.getValue(project, root, DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY);
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

  @UIThreadUnsafe
  private void setDeleteLocalBranchOnSlideOutGitConfigValue(Project project, VirtualFile root, String value) {
    try {
      val additionalParameters = "--local";
      GitConfigUtil.setValue(project, root, DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY, value, additionalParameters);
    } catch (VcsException e) {
      LOG.error("Attempt to set '${DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY}' git config value failed");
    }
  }
}
