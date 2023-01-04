package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import static com.virtuslab.gitmachete.frontend.common.WriteActionUtils.runWriteActionOnUIThread;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.fmt;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils.getMacheteFilePath;

import java.io.IOException;
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
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.frontend.actions.dialogs.DeleteBranchOnSlideOutSuggestionDialog;
import com.virtuslab.gitmachete.frontend.file.MacheteFileWriter;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.qual.async.ContinuesInBackground;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
public class SlideOutBackgroundable extends Task.Backgroundable {

  private final Project project;
  private final String branchToSlideOutName;
  @Nullable
  private final String currentBranchNameIfManaged;
  private final BranchLayout branchLayout;
  private final GitRepository gitRepository;
  private final BaseEnhancedGraphTable graphTable;

  public static final String DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY = "machete.slideOut.deleteLocalBranch";

  public SlideOutBackgroundable(String title, IManagedBranchSnapshot branchToSlideOut,
      GitRepository gitRepository,
      @Nullable IManagedBranchSnapshot currentBranchNameIfManaged,
      BranchLayout branchLayout,
      BaseEnhancedGraphTable graphTable) {
    super(gitRepository.getProject(), title);
    this.project = gitRepository.getProject();
    this.branchToSlideOutName = branchToSlideOut.getName();
    this.currentBranchNameIfManaged = currentBranchNameIfManaged != null ? currentBranchNameIfManaged.getName() : null;
    this.branchLayout = branchLayout;
    this.gitRepository = gitRepository;
    this.graphTable = graphTable;

    LOG.debug(() -> "Entering: branchToSlideOut = ${branchToSlideOutName}");
    LOG.debug("Refreshing repository state");
  }

  @Override
  @ContinuesInBackground
  @UIThreadUnsafe
  public void run(ProgressIndicator indicator) {
    val slideOutBranchIsCurrent = branchToSlideOutName.equals(currentBranchNameIfManaged);
    if (slideOutBranchIsCurrent) {
      LOG.debug("Skipping (optional) local branch deletion because it is equal to current branch");
      slideOutBranch(branchToSlideOutName);
      graphTable.queueRepositoryUpdateAndModelRefresh();
      VcsNotifier.getInstance(project).notifySuccess(/* displayId */ null,
          /* title */ "",
          fmt(getString("action.GitMachete.BaseSlideOutAction.notification.title.slide-out-success.of-current.HTML"),
              branchToSlideOutName));

    } else {
      val root = gitRepository.getRoot();
      val shouldDelete = getDeleteLocalBranchOnSlideOutGitConfigValue(root);
      if (shouldDelete == null) {
        ModalityUiUtil.invokeLaterIfNeeded(ModalityState.NON_MODAL, () -> suggestBranchDeletion(branchToSlideOutName));
      } else {
        handleBranchDeletionDecision(branchToSlideOutName, shouldDelete);
      }
    }
  }

  @ContinuesInBackground
  @UIEffect
  private void suggestBranchDeletion(String branchName) {
    val slideOutOptions = new DeleteBranchOnSlideOutSuggestionDialog(project, branchName).showAndGetSlideOutOptions();

    new Task.Backgroundable(project, getString("action.GitMachete.BaseSlideOutAction.task.title")) {
      @Override
      @UIThreadUnsafe
      public void run(ProgressIndicator indicator) {
        if (slideOutOptions != null) {
          handleBranchDeletionDecision(branchName, slideOutOptions.shouldDelete());

          if (slideOutOptions.shouldRemember()) {
            val value = String.valueOf(slideOutOptions.shouldDelete());
            setDeleteLocalBranchOnSlideOutGitConfigValue(gitRepository.getRoot(), value);
          }
        } else {
          val title = getString("action.GitMachete.BaseSlideOutAction.notification.title.slide-out-info.canceled");
          val message = fmt(getString(
              "action.GitMachete.BaseSlideOutAction.notification.message.slide-out-info.canceled.HTML"), branchName);
          VcsNotifier.getInstance(project).notifyInfo(/* displayId */ null, title, message);
        }
      }
    }.queue();
  }

  @UIThreadUnsafe
  private void handleBranchDeletionDecision(String branchName, boolean shouldDelete) {
    slideOutBranch(branchName);
    if (shouldDelete) {
      GitBrancher.getInstance(project).deleteBranch(branchName, Collections.singletonList(gitRepository));
      VcsNotifier.getInstance(project).notifySuccess(/* displayId */ null,
          /* title */ "",
          fmt(getString("action.GitMachete.BaseSlideOutAction.notification.title.slide-out-success.with-delete.HTML"),
              branchName));
      return;
    } else {
      VcsNotifier.getInstance(project).notifySuccess(/* displayId */ null,
          /* title */ "",
          fmt(getString("action.GitMachete.BaseSlideOutAction.notification.title.slide-out-success.without-delete.HTML"),
              branchName));
    }
    graphTable.queueRepositoryUpdateAndModelRefresh();
  }

  @UIThreadUnsafe
  private void slideOutBranch(String branchName) {
    LOG.info("Sliding out '${branchName}' branch in memory");
    val newBranchLayout = branchLayout.slideOut(branchName);

    runWriteActionOnUIThread(() -> {
      try {
        val macheteFilePath = getMacheteFilePath(gitRepository);
        val branchLayoutWriter = RuntimeBinding.instantiateSoleImplementingClass(IBranchLayoutWriter.class);
        LOG.info("Writing new branch layout into ${macheteFilePath}");
        MacheteFileWriter.writeBranchLayout(macheteFilePath, branchLayoutWriter,
            newBranchLayout, /* backupOldFile */ true, /* requestor */ this);

      } catch (IOException e) {
        val exceptionMessage = e.getMessage();
        val errorMessage = "Error occurred while sliding out '${branchName}' branch" +
            (exceptionMessage == null ? "" : ": " + exceptionMessage);
        LOG.error(errorMessage);
        VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
            fmt(getString("action.GitMachete.BaseSlideOutAction.notification.title.slide-out-fail.HTML"), branchName),
            exceptionMessage == null ? "" : exceptionMessage);
      }
    });
  }

  @UIThreadUnsafe
  private @Nullable Boolean getDeleteLocalBranchOnSlideOutGitConfigValue(VirtualFile root) {
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
  private void setDeleteLocalBranchOnSlideOutGitConfigValue(VirtualFile root, String value) {
    try {
      val additionalParameters = "--local";
      GitConfigUtil.setValue(project, root, DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY, value, additionalParameters);
    } catch (VcsException e) {
      LOG.error("Attempt to set '${DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY}' git config value failed");
    }
  }
}
