package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import static com.virtuslab.gitmachete.frontend.actions.common.BranchCreationUtils.waitForCreationOfLocalBranch;
import static com.virtuslab.gitmachete.frontend.common.WriteActionUtils.runWriteActionOnUIThread;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.nio.file.Path;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.repo.GitRepository;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.frontend.file.MacheteFileWriter;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod(GitVfsUtils.class)
public class RenameBackgroundable extends Task.Backgroundable {

  protected final Project project;
  protected final GitRepository gitRepository;
  protected final BranchLayout branchLayout;
  protected final Runnable renameRunnable;
  protected final String currentBranchName;
  protected final String newBranchName;

  public RenameBackgroundable(
      GitRepository gitRepository,
      BranchLayout branchLayout,
      Runnable renameRunnable,
      String currentBranchName,
      String newBranchName) {
    super(gitRepository.getProject(), getString("action.GitMachete.RenameBackgroundable.task-title"));
    this.project = gitRepository.getProject();
    this.gitRepository = gitRepository;
    this.branchLayout = branchLayout;
    this.renameRunnable = renameRunnable;
    this.currentBranchName = currentBranchName;
    this.newBranchName = newBranchName;
  }

  @Override
  @UIThreadUnsafe
  public void run(ProgressIndicator indicator) {
    Path macheteFilePath = gitRepository.getMacheteFilePath();

    val newBranchLayout = branchLayout.rename(currentBranchName, newBranchName);
    val branchLayoutWriter = ApplicationManager.getApplication().getService(IBranchLayoutWriter.class);

    renameRunnable.run();
    // `renameRunnable` may perform some sneakily-asynchronous operations (e.g. renameBranch).
    // The high-level method used within the runnable does not allow us to schedule the tasks after them.
    // (Stepping deeper is not an option since we would lose some important logic or become very dependent on the internals of git4idea).
    // Hence, we wait for the creation of the branch (with exponential backoff).
    waitForCreationOfLocalBranch(gitRepository, newBranchName);

    runWriteActionOnUIThread(() -> {
      MacheteFileWriter.writeBranchLayout(
          macheteFilePath,
          branchLayoutWriter,
          newBranchLayout,
          /* backupOldLayout */ true,
          /* requestor */ this);

    });
  }

  @Override
  @UIEffect
  public void onThrowable(Throwable e) {
    val exceptionMessage = e.getMessage();
    VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
        /* title */ getString(
            "action.GitMachete.BaseSlideInBelowAction.notification.title.branch-layout-write-fail"),
        exceptionMessage == null ? "" : exceptionMessage);
  }

}
