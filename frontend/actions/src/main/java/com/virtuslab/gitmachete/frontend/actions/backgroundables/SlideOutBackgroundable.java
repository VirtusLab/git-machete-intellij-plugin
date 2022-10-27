package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.util.Collections;

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
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.frontend.actions.dialogs.DeleteBranchOnSlideOutSuggestionDialog;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod({GitVfsUtils.class, GitMacheteBundle.class})
@CustomLog
public class SlideOutBackgroundable extends Task.Backgroundable {

  private final String branchToSlideOutName;
  @Nullable
  private final String currentBranchNameIfManaged;
  private final BranchLayout branchLayout;

  private final IBranchLayoutWriter branchLayoutWriter;

  @Nullable
  private final GitRepository selectedGitRepository;

  @Nullable
  private final BaseEnhancedGraphTable graphTable;

  public static final String DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY = "machete.slideOut.deleteLocalBranch";

  public SlideOutBackgroundable(Project project, String title, String branchToSlideOutName,
      @Nullable GitRepository selectedGitRepository,
      @Nullable String currentBranchNameIfManaged,
      BranchLayout branchLayout,
      IBranchLayoutWriter branchLayoutWriter,
      @Nullable BaseEnhancedGraphTable graphTable) {
    super(project, title);
    this.branchToSlideOutName = branchToSlideOutName;
    this.currentBranchNameIfManaged = currentBranchNameIfManaged;
    this.branchLayout = branchLayout;
    this.branchLayoutWriter = branchLayoutWriter;
    this.selectedGitRepository = selectedGitRepository;
    this.graphTable = graphTable;

    LOG.debug(() -> "Entering: branchToSlideOut = ${branchToSlideOutName}");

    LOG.debug("Refreshing repository state");

  }

  public LambdaLogger log() {
    return LOG;
  }

  @Override
  @UIThreadUnsafe
  public void run(ProgressIndicator indicator) {
    if (myProject != null) {
      deleteBranchIfRequired(branchToSlideOutName);
    }
  }

  @UIThreadUnsafe
  private void deleteBranchIfRequired(String branchName) {
    if (myProject == null)
      return;
    val project = myProject;
    val slidOutBranchIsCurrent = currentBranchNameIfManaged != null && currentBranchNameIfManaged.equals(branchName);

    if (slidOutBranchIsCurrent && graphTable != null) {
      LOG.debug("Skipping (optional) local branch deletion because it is equal to current branch");
      slideOutBranch(branchName);
      graphTable.queueRepositoryUpdateAndModelRefresh();
      VcsNotifier.getInstance(project).notifySuccess(/* displayId */ null,
          /* title */ "",
          getString("action.GitMachete.BaseSlideOutAction.notification.title.slide-out-success.of-current.HTML")
              .format(branchName));

    } else if (selectedGitRepository != null) {
      val gitRepository = selectedGitRepository;
      val root = selectedGitRepository.getRoot();
      val shouldDelete = getDeleteLocalBranchOnSlideOutGitConfigValue(project, root);
      if (shouldDelete == null) {
        ModalityUiUtil.invokeLaterIfNeeded(ModalityState.NON_MODAL,
            () -> suggestBranchDeletion(branchName, gitRepository, project));
      } else {
        handleBranchDeletionDecision(project, branchName, selectedGitRepository, shouldDelete);
      }

    } else {
      if (graphTable != null) {
        graphTable.queueRepositoryUpdateAndModelRefresh();
      }
    }
  }

  @UIEffect
  private void suggestBranchDeletion(String branchName, GitRepository gitRepository,
      Project project) {
    val slideOutOptions = new DeleteBranchOnSlideOutSuggestionDialog(project, branchName).showAndGetSlideOutOptions();

    new Task.Backgroundable(project, "Deleting branch if required...") {
      @Override
      @UIThreadUnsafe
      public void run(ProgressIndicator indicator) {
        if (slideOutOptions != null) {
          handleBranchDeletionDecision(project, branchName, gitRepository, slideOutOptions.shouldDelete());

          if (slideOutOptions.shouldRemember()) {
            val value = String.valueOf(slideOutOptions.shouldDelete());
            setDeleteLocalBranchOnSlideOutGitConfigValue(project, gitRepository.getRoot(), value);
          }
        } else {
          val title = getString("action.GitMachete.BaseSlideOutAction.notification.title.slide-out-info.canceled");
          val message = getString(
              "action.GitMachete.BaseSlideOutAction.notification.message.slide-out-info.canceled.HTML")
                  .format(branchName);
          VcsNotifier.getInstance(project).notifyInfo(/* displayId */ null, title, message);
        }
      }
    }.queue();
  }

  @UIThreadUnsafe
  private void slideOutBranch(String branchName) {
    if (branchLayout == null || selectedGitRepository == null || myProject == null) {
      return;
    }

    LOG.info("Sliding out '${branchName}' branch in memory");
    val newBranchLayout = branchLayout.slideOut(branchName);

    try {
      val macheteFilePath = selectedGitRepository.getMacheteFilePath();
      LOG.info("Writing new branch layout into ${macheteFilePath}");
      branchLayoutWriter.write(macheteFilePath, newBranchLayout, /* backupOldLayout */ true);

    } catch (BranchLayoutException e) {
      val exceptionMessage = e.getMessage();
      val errorMessage = "Error occurred while sliding out '${branchName}' branch" +
          (exceptionMessage == null ? "" : ": " + exceptionMessage);
      LOG.error(errorMessage);
      VcsNotifier.getInstance(myProject).notifyError(/* displayId */ null,
          getString("action.GitMachete.BaseSlideOutAction.notification.title.slide-out-fail.HTML").format(branchName),
          exceptionMessage == null ? "" : exceptionMessage);
    }
  }

  @UIThreadUnsafe
  private void handleBranchDeletionDecision(Project project, String branchName, GitRepository gitRepository,
      boolean shouldDelete) {
    slideOutBranch(branchName);
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
    if (graphTable != null) {
      graphTable.queueRepositoryUpdateAndModelRefresh();
    }
  }

  @UIThreadUnsafe
  private @Nullable Boolean getDeleteLocalBranchOnSlideOutGitConfigValue(Project project, VirtualFile root) {
    try {
      val value = GitConfigUtil.getValue(project, root, DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY);
      if (value != null) {
        Boolean booleanValue = GitConfigUtil.getBooleanValue(value);
        return booleanValue != null && booleanValue;
      }
    } catch (VcsException e) {
      LOG.info(
          "Attempt to get '${DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY}' git config value failed: key may not exist");
    }

    return null;
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
