package com.virtuslab.gitmachete.frontend.actions.common;

import static com.virtuslab.gitmachete.frontend.common.WriteActionUtils.blockingRunWriteActionOnUIThread;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils.getMacheteFilePath;

import java.io.IOException;
import java.util.Collections;
import java.util.function.Consumer;

import com.intellij.openapi.application.ApplicationManager;
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
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.SideEffectingBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.dialogs.DeleteBranchOnSlideOutSuggestionDialog;
import com.virtuslab.gitmachete.frontend.file.MacheteFileWriter;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.qual.async.ContinuesInBackground;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
@ExtensionMethod({GitMacheteBundle.class})
public class SlideOut {

  private final Project project;
  private final String branchToSlideOutName;
  private final @Nullable String currentBranchNameIfManaged;
  private final BranchLayout branchLayout;
  private final GitRepository gitRepository;
  private final BaseEnhancedGraphTable graphTable;

  public static final String DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY = "machete.slideOut.deleteLocalBranch";

  public SlideOut(IManagedBranchSnapshot branchToSlideOut,
      GitRepository gitRepository,
      @Nullable IManagedBranchSnapshot currentBranchNameIfManaged,
      BranchLayout branchLayout,
      BaseEnhancedGraphTable graphTable) {
    this.project = gitRepository.getProject();
    this.branchToSlideOutName = branchToSlideOut.getName();
    this.currentBranchNameIfManaged = currentBranchNameIfManaged != null ? currentBranchNameIfManaged.getName() : null;
    this.branchLayout = branchLayout;
    this.gitRepository = gitRepository;
    this.graphTable = graphTable;

    LOG.debug(() -> "Entering: branchToSlideOut = ${branchToSlideOutName}");
    LOG.debug("Refreshing repository state");
  }

  @ContinuesInBackground
  public void run() {
    run(() -> {});
  }

  @ContinuesInBackground
  public void run(@UI Runnable doInUIThreadWhenReady) {
    val slideOutBranchIsCurrent = branchToSlideOutName.equals(currentBranchNameIfManaged);
    if (slideOutBranchIsCurrent) {
      LOG.debug("Skipping (optional) local branch deletion because it is equal to current branch");
      slideOutBranch(branchToSlideOutName);
      VcsNotifier.getInstance(project).notifySuccess(/* displayId */ null,
          /* title */ "",
          getString("action.GitMachete.BaseSlideOutAction.notification.title.slide-out-success.of-current.HTML").fmt(
              branchToSlideOutName));

    } else {
      val root = gitRepository.getRoot();
      getDeleteLocalBranchOnSlideOutGitConfigValueAndExecute(root, (@Nullable Boolean shouldDelete) -> {
        if (shouldDelete == null) {
          ModalityUiUtil.invokeLaterIfNeeded(ModalityState.NON_MODAL,
              () -> suggestBranchDeletion(branchToSlideOutName, doInUIThreadWhenReady));
        } else {
          handleBranchDeletionDecision(branchToSlideOutName, shouldDelete, doInUIThreadWhenReady);
        }
      });
    }
  }

  @ContinuesInBackground
  @UIEffect
  private void suggestBranchDeletion(String branchName, @UI Runnable doInUIThreadWhenBranchDeletionReady) {
    val slideOutOptions = new DeleteBranchOnSlideOutSuggestionDialog(project, branchName).showAndGetSlideOutOptions();

    if (slideOutOptions != null) {
      handleBranchDeletionDecision(branchName, slideOutOptions.shouldDelete(), doInUIThreadWhenBranchDeletionReady);

      if (slideOutOptions.shouldRemember()) {
        val value = String.valueOf(slideOutOptions.shouldDelete());
        setDeleteLocalBranchOnSlideOutGitConfigValue(gitRepository.getRoot(), value);
      }
    } else {
      val title = getString("action.GitMachete.BaseSlideOutAction.notification.title.slide-out-info.canceled");
      val message = getString(
          "action.GitMachete.BaseSlideOutAction.notification.message.slide-out-info.canceled.HTML").fmt(branchName);
      VcsNotifier.getInstance(project).notifyInfo(/* displayId */ null, title, message);
      doInUIThreadWhenBranchDeletionReady.run();
    }
  }

  @ContinuesInBackground
  private void handleBranchDeletionDecision(String branchName, boolean shouldDelete, @UI Runnable doInUIThreadWhenReady) {
    slideOutBranch(branchName);
    if (shouldDelete) {
      graphTable.queueRepositoryUpdateAndModelRefresh(
          () -> GitBrancher.getInstance(project).deleteBranches(Collections.singletonMap(branchName,
              Collections.singletonList(gitRepository)), () -> {
                VcsNotifier.getInstance(project).notifySuccess(/* displayId */ null,
                    /* title */ "",
                    getString("action.GitMachete.BaseSlideOutAction.notification.title.slide-out-success.with-delete.HTML").fmt(
                        branchName));
                doInUIThreadWhenReady.run();
              }));
    } else {
      VcsNotifier.getInstance(project).notifySuccess(/* displayId */ null,
          /* title */ "",
          getString("action.GitMachete.BaseSlideOutAction.notification.title.slide-out-success.without-delete.HTML").fmt(
              branchName));
      ModalityUiUtil.invokeLaterIfNeeded(ModalityState.NON_MODAL, doInUIThreadWhenReady);
    }
  }

  private void slideOutBranch(String branchName) {
    LOG.info("Sliding out '${branchName}' branch in memory");
    val newBranchLayout = branchLayout.slideOut(branchName);

    // Let's execute the write action in a blocking way, in order to prevent branch deletion from running concurrently.
    // If branch deletion completes before the new branch layout is saved, we might end up with an issue like
    // https://github.com/VirtusLab/git-machete-intellij-plugin/issues/971.
    blockingRunWriteActionOnUIThread(() -> {
      try {
        val macheteFilePath = getMacheteFilePath(gitRepository);
        val branchLayoutWriter = ApplicationManager.getApplication().getService(IBranchLayoutWriter.class);
        LOG.info("Writing new branch layout into ${macheteFilePath}");
        MacheteFileWriter.writeBranchLayout(macheteFilePath, branchLayoutWriter,
            newBranchLayout, /* backupOldFile */ true, /* requestor */ this);

      } catch (IOException e) {
        val exceptionMessage = e.getMessage();
        val errorMessage = "Error occurred while sliding out '${branchName}' branch" +
            (exceptionMessage == null ? "" : ": " + exceptionMessage);
        LOG.error(errorMessage);
        VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
            getString("action.GitMachete.BaseSlideOutAction.notification.title.slide-out-fail.HTML").fmt(branchName),
            exceptionMessage == null ? "" : exceptionMessage);
      }
    });
  }

  @ContinuesInBackground
  private void getDeleteLocalBranchOnSlideOutGitConfigValueAndExecute(VirtualFile root,
      Consumer<@Nullable Boolean> doForConfigValue) {
    new Task.Backgroundable(project, getString("action.GitMachete.get-git-config.task-title")) {

      @Override
      @UIThreadUnsafe
      public void run(ProgressIndicator indicator) {
        Boolean result = null;
        try {
          val value = GitConfigUtil.getValue(project, root, DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY);
          if (value != null) {
            Boolean booleanValue = GitConfigUtil.getBooleanValue(value);
            result = booleanValue != null && booleanValue;
          }
        } catch (VcsException e) {
          LOG.warn("Attempt to get '${DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY}' git config value failed", e);
        }
        doForConfigValue.accept(result);
      }
    }.queue();
  }

  @ContinuesInBackground
  private void setDeleteLocalBranchOnSlideOutGitConfigValue(VirtualFile root, String value) {
    new SideEffectingBackgroundable(project, getString("action.GitMachete.set-git-config.task-title"), "setting git config") {

      @Override
      @UIThreadUnsafe
      public void doRun(ProgressIndicator indicator) {
        try {
          val additionalParameters = "--local";
          GitConfigUtil.setValue(project, root, DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY, value, additionalParameters);
        } catch (VcsException e) {
          LOG.error("Attempt to set '${DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY}' git config value failed", e);
        }
      }
    }.queue();
  }
}
