package com.virtuslab.gitmachete.frontend.actions.common;

import static com.virtuslab.gitmachete.frontend.defs.GitConfigKeys.DELETE_LOCAL_BRANCH_ON_SLIDE_OUT;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
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
import com.virtuslab.gitmachete.frontend.actions.hooks.PostSlideOutHookExecutor;
import com.virtuslab.gitmachete.frontend.common.WriteActionUtils;
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
  private final BranchLayout branchLayout;
  private final GitRepository gitRepository;
  private final BaseEnhancedGraphTable graphTable;

  public SlideOut(IManagedBranchSnapshot branchToSlideOut,
      GitRepository gitRepository,
      BranchLayout branchLayout,
      BaseEnhancedGraphTable graphTable) {
    this.project = gitRepository.getProject();
    this.branchToSlideOutName = branchToSlideOut.getName();
    this.branchLayout = branchLayout;
    this.gitRepository = gitRepository;
    this.graphTable = graphTable;
  }

  @ContinuesInBackground
  public void run() {
    run(() -> {});
  }

  @ContinuesInBackground
  public void run(@UI Runnable doInUIThreadWhenReady) {
    val slideOutBranchIsCurrent = branchToSlideOutName.equals(gitRepository.getCurrentBranchName());
    if (slideOutBranchIsCurrent) {
      LOG.debug("Skipping (optional) local branch deletion because it is equal to current branch");
      slideOutBranchAndRunPostSlideOutHookIfPresent(() -> {
        VcsNotifier.getInstance(project).notifySuccess(/* displayId */ null,
            /* title */ "",
            getString("action.GitMachete.SlideOut.notification.title.slide-out-success.of-current.HTML").fmt(
                branchToSlideOutName));
        ModalityUiUtil.invokeLaterIfNeeded(ModalityState.NON_MODAL, doInUIThreadWhenReady);
      });
    } else {
      val root = gitRepository.getRoot();
      getDeleteLocalBranchOnSlideOutGitConfigValueAndExecute(root, (@Nullable Boolean shouldDelete) -> {
        if (shouldDelete == null) {
          ModalityUiUtil.invokeLaterIfNeeded(ModalityState.NON_MODAL, () -> suggestBranchDeletion(doInUIThreadWhenReady));
        } else {
          handleBranchDeletionDecision(shouldDelete, doInUIThreadWhenReady);
        }
      });
    }
  }

  @ContinuesInBackground
  @UIEffect
  private void suggestBranchDeletion(@UI Runnable doInUIThreadWhenBranchDeletionReady) {
    val slideOutOptions = new DeleteBranchOnSlideOutSuggestionDialog(project, branchToSlideOutName).showAndGetSlideOutOptions();

    if (slideOutOptions != null) {
      handleBranchDeletionDecision(slideOutOptions.shouldDelete(), doInUIThreadWhenBranchDeletionReady);

      if (slideOutOptions.shouldRemember()) {
        val value = String.valueOf(slideOutOptions.shouldDelete());
        setDeleteLocalBranchOnSlideOutGitConfigValue(gitRepository.getRoot(), value);
      }
    } else {
      val title = getString("action.GitMachete.SlideOut.notification.title.slide-out-info.canceled");
      val message = getString("action.GitMachete.SlideOut.notification.message.slide-out-info.canceled.HTML")
          .fmt(branchToSlideOutName);
      VcsNotifier.getInstance(project).notifyInfo(/* displayId */ null, title, message);
      doInUIThreadWhenBranchDeletionReady.run();
    }
  }

  @ContinuesInBackground
  private void handleBranchDeletionDecision(boolean shouldDelete, @UI Runnable doInUIThreadWhenReady) {
    slideOutBranchAndRunPostSlideOutHookIfPresent(() -> {
      if (shouldDelete) {
        graphTable.queueRepositoryUpdateAndModelRefresh(
            () -> GitBrancher.getInstance(project).deleteBranches(Collections.singletonMap(branchToSlideOutName,
                Collections.singletonList(gitRepository)), () -> {
                  VcsNotifier.getInstance(project).notifySuccess(/* displayId */ null,
                      /* title */ "",
                      getString("action.GitMachete.SlideOut.notification.title.slide-out-success.with-delete.HTML").fmt(
                          branchToSlideOutName));
                  doInUIThreadWhenReady.run();
                }));
      } else {
        VcsNotifier.getInstance(project).notifySuccess(/* displayId */ null,
            /* title */ "",
            getString("action.GitMachete.SlideOut.notification.title.slide-out-success.without-delete.HTML").fmt(
                branchToSlideOutName));
        ModalityUiUtil.invokeLaterIfNeeded(ModalityState.NON_MODAL, doInUIThreadWhenReady);
      }
    });
  }

  @ContinuesInBackground
  private void slideOutBranchAndRunPostSlideOutHookIfPresent(Runnable doWhenReady) {
    LOG.info("Sliding out '${branchToSlideOutName}' branch in memory");
    val branchToSlideOut = branchLayout.getEntryByName(branchToSlideOutName);
    if (branchToSlideOut == null) {
      // Unlikely, let's handle this case to calm down Checker Framework.
      return;
    }
    val parentBranch = branchToSlideOut.getParent();
    val parentBranchName = parentBranch != null ? parentBranch.getName() : null;
    val childBranchNames = branchToSlideOut.getChildren().map(child -> child.getName());
    val newBranchLayout = branchLayout.slideOut(branchToSlideOutName);

    // Let's execute the write action in a blocking way, in order to prevent branch deletion from running concurrently.
    // If branch deletion completes before the new branch layout is saved, we might end up with an issue like
    // https://github.com/VirtusLab/git-machete-intellij-plugin/issues/971.
    WriteActionUtils.<RuntimeException>blockingRunWriteActionOnUIThread(() -> {
      try {
        val macheteFilePath = getMacheteFilePath(gitRepository);
        val branchLayoutWriter = ApplicationManager.getApplication().getService(IBranchLayoutWriter.class);
        LOG.info("Writing new branch layout into ${macheteFilePath}");
        MacheteFileWriter.writeBranchLayout(macheteFilePath, branchLayoutWriter,
            newBranchLayout, /* backupOldFile */ true, /* requestor */ this);

      } catch (IOException e) {
        val exceptionMessage = e.getMessage();
        val errorMessage = "Error occurred while sliding out '${branchToSlideOutName}' branch" +
            (exceptionMessage == null ? "" : ": " + exceptionMessage);
        LOG.error(errorMessage);
        VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
            getString("action.GitMachete.SlideOut.notification.title.slide-out-fail.HTML").fmt(branchToSlideOutName),
            exceptionMessage == null ? "" : exceptionMessage);
      }
    });

    new SideEffectingBackgroundable(project, getNonHtmlString("string.GitMachete.SlideOut.post-slide-out-hook-task.title"),
        "machete-post-slide-out hook") {
      @Override
      @UIThreadUnsafe
      protected void doRun(ProgressIndicator indicator) {
        val postSlideOutHookExecutor = new PostSlideOutHookExecutor(gitRepository);
        if (postSlideOutHookExecutor.executeHookFor(parentBranchName, branchToSlideOutName, childBranchNames)) {
          doWhenReady.run();
        }
      }
    }.queue();
  }

  @ContinuesInBackground
  private void getDeleteLocalBranchOnSlideOutGitConfigValueAndExecute(VirtualFile root,
      Consumer<@Nullable Boolean> doForConfigValue) {
    new Task.Backgroundable(project, getNonHtmlString("action.GitMachete.get-git-config.task-title")) {

      @Override
      @UIThreadUnsafe
      public void run(ProgressIndicator indicator) {
        Boolean result = null;
        try {
          val value = GitConfigUtil.getValue(project, root, DELETE_LOCAL_BRANCH_ON_SLIDE_OUT);
          if (value != null) {
            Boolean booleanValue = GitConfigUtil.getBooleanValue(value);
            result = booleanValue != null && booleanValue;
          }
        } catch (VcsException e) {
          LOG.warn("Attempt to get '${DELETE_LOCAL_BRANCH_ON_SLIDE_OUT}' git config value failed", e);
        }
        doForConfigValue.accept(result);
      }
    }.queue();
  }

  @ContinuesInBackground
  private void setDeleteLocalBranchOnSlideOutGitConfigValue(VirtualFile root, String value) {
    new SideEffectingBackgroundable(project, getNonHtmlString("action.GitMachete.set-git-config.task-title"),
        "setting git config") {

      @Override
      @UIThreadUnsafe
      public void doRun(ProgressIndicator indicator) {
        try {
          val additionalParameters = "--local";
          GitConfigUtil.setValue(project, root, DELETE_LOCAL_BRANCH_ON_SLIDE_OUT, value, additionalParameters);
        } catch (VcsException e) {
          LOG.error("Attempt to set '${DELETE_LOCAL_BRANCH_ON_SLIDE_OUT}' git config value failed", e);
        }
      }
    }.queue();
  }
}
