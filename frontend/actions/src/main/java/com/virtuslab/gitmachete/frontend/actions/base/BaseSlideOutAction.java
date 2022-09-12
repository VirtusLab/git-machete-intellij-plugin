package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getQuotedStringOrCurrent;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

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
import com.intellij.util.ModalityUiUtil;
import git4idea.branch.GitBrancher;
import git4idea.config.GitConfigUtil;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.frontend.actions.dialogs.DeleteBranchOnSlideOutSuggestionDialog;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod({GitVfsUtils.class, GitMacheteBundle.class})
@CustomLog
public abstract class BaseSlideOutAction extends BaseGitMacheteRepositoryReadyAction
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

    final var branchName = getNameOfBranchUnderAction(anActionEvent);
    final var branch = branchName != null
        ? getManagedBranchByName(anActionEvent, branchName)
        : null;

    if (branch == null) {
      presentation.setEnabled(false);
      presentation.setDescription(getNonHtmlString("action.GitMachete.description.disabled.undefined.machete-branch")
          .format("Slide out", getQuotedStringOrCurrent(branchName)));
    } else {
      presentation
          .setDescription(getNonHtmlString("action.GitMachete.BaseSlideOutAction.description").format(branch.getName()));
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    LOG.debug("Performing");

    final var branchName = getNameOfBranchUnderAction(anActionEvent);
    final var branch = getManagedBranchByName(anActionEvent, branchName);
    if (branch != null) {
      doSlideOut(anActionEvent, branch);
    }
  }

  @UIEffect
  private void doSlideOut(AnActionEvent anActionEvent, IManagedBranchSnapshot branchToSlideOut) {
    LOG.debug(() -> "Entering: branchToSlideOut = ${branchToSlideOut}");
    String branchName = branchToSlideOut.getName();
    final var project = getProject(anActionEvent);

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
    final var gitRepository = getSelectedGitRepository(anActionEvent);
    final var project = getProject(anActionEvent);
    final var currentBranchNameIfManaged = getCurrentBranchNameIfManaged(anActionEvent);
    final var slidOutBranchIsCurrent = currentBranchNameIfManaged != null
        ? currentBranchNameIfManaged.equals(branchName)
        : false;

    if (slidOutBranchIsCurrent) {
      LOG.debug("Skipping (optional) local branch deletion because it is equal to current branch");
      slideOutBranch(anActionEvent, branchName);
      getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
      VcsNotifier.getInstance(project).notifySuccess(/* displayId */ null,
          /* title */ "",
          getString("action.GitMachete.BaseSlideOutAction.notification.title.slide-out-success.of-current.HTML")
              .format(branchName));

    } else if (gitRepository != null) {
      final var root = gitRepository.getRoot();
      final var configValueOption = getDeleteLocalBranchOnSlideOutGitConfigValue(project, root);
      if (configValueOption.isEmpty()) {
        ModalityUiUtil.invokeLaterIfNeeded(ModalityState.NON_MODAL,
            () -> suggestBranchDeletion(anActionEvent, branchName, gitRepository, project));
      } else if (configValueOption.isDefined()) {
        final var shouldDelete = configValueOption.get();
        handleBranchDeletionDecision(project, branchName, gitRepository, anActionEvent, shouldDelete);
      }

    } else {
      getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
    }
  }

  @UIEffect
  private void suggestBranchDeletion(AnActionEvent anActionEvent, String branchName, GitRepository gitRepository,
      Project project) {
    final var slideOutOptions = new DeleteBranchOnSlideOutSuggestionDialog(project, branchName).showAndGetSlideOutOptions();

    new Task.Backgroundable(project, "Deleting branch if required...") {
      @Override
      @UIThreadUnsafe
      public void run(ProgressIndicator indicator) {
        if (slideOutOptions != null) {
          handleBranchDeletionDecision(project, branchName, gitRepository, anActionEvent, slideOutOptions.shouldDelete());

          if (slideOutOptions.shouldRemember()) {
            final var value = String.valueOf(slideOutOptions.shouldDelete());
            setDeleteLocalBranchOnSlideOutGitConfigValue(project, gitRepository.getRoot(), value);
          }
        } else {
          final var title = getString("action.GitMachete.BaseSlideOutAction.notification.title.slide-out-info.canceled");
          final var message = getString(
              "action.GitMachete.BaseSlideOutAction.notification.message.slide-out-info.canceled.HTML")
                  .format(branchName);
          VcsNotifier.getInstance(project).notifyInfo(/* displayId */ null, title, message);
        }
      }
    }.queue();
  }

  private void slideOutBranch(AnActionEvent anActionEvent, String branchName) {
    final var project = getProject(anActionEvent);
    final var branchLayout = getBranchLayout(anActionEvent);
    final var branchLayoutWriter = getBranchLayoutWriter(anActionEvent);
    final var gitRepository = getSelectedGitRepository(anActionEvent);
    if (branchLayout == null || gitRepository == null) {
      return;
    }

    LOG.info("Sliding out '${branchName}' branch in memory");
    final var newBranchLayout = branchLayout.slideOut(branchName);

    try {
      final var macheteFilePath = gitRepository.getMacheteFilePath();
      LOG.info("Writing new branch layout into ${macheteFilePath}");
      branchLayoutWriter.write(macheteFilePath, newBranchLayout, /* backupOldLayout */ true);

    } catch (BranchLayoutException e) {
      final var exceptionMessage = e.getMessage();
      final var errorMessage = "Error occurred while sliding out '${branchName}' branch" +
          (exceptionMessage == null ? "" : ": " + exceptionMessage);
      LOG.error(errorMessage);
      VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
          getString("action.GitMachete.BaseSlideOutAction.notification.title.slide-out-fail.HTML").format(branchName),
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
          getString("action.GitMachete.BaseSlideOutAction.notification.title.slide-out-success.with-delete.HTML")
              .format(branchName));
      return;
    } else {
      VcsNotifier.getInstance(project).notifySuccess(/* displayId */ null,
          /* title */ "",
          getString("action.GitMachete.BaseSlideOutAction.notification.title.slide-out-success.without-delete.HTML")
              .format(branchName));
    }
    getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
  }

  @UIThreadUnsafe
  private Option<Boolean> getDeleteLocalBranchOnSlideOutGitConfigValue(Project project, VirtualFile root) {
    try {
      final var value = GitConfigUtil.getValue(project, root, DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY);
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
      final var additionalParameters = "--local";
      GitConfigUtil.setValue(project, root, DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY, value, additionalParameters);
    } catch (VcsException e) {
      LOG.error("Attempt to set '${DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY}' git config value failed");
    }
  }
}
