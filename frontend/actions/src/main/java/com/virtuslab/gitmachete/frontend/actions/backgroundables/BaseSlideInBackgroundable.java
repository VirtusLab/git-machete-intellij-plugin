package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.nio.file.Path;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.GitLocalBranch;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import io.vavr.control.Try;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.BranchLayoutEntry;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.frontend.actions.common.SlideInOptions;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod({GitVfsUtils.class, GitMacheteBundle.class})
public abstract class BaseSlideInBackgroundable extends Task.Backgroundable {

  protected final Project project;
  protected final GitRepository gitRepository;
  protected final BranchLayout branchLayout;
  protected final IBranchLayoutWriter branchLayoutWriter;
  protected final Runnable preSlideInRunnable;
  protected final SlideInOptions slideInOptions;

  public BaseSlideInBackgroundable(
      Project project,
      GitRepository gitRepository,
      BranchLayout branchLayout,
      IBranchLayoutWriter branchLayoutWriter,
      Runnable preSlideInRunnable,
      SlideInOptions slideInOptions) {
    super(project, getString("action.GitMachete.BaseSlideInBackgroundable.task-title"));
    this.project = project;
    this.gitRepository = gitRepository;
    this.branchLayout = branchLayout;
    this.branchLayoutWriter = branchLayoutWriter;
    this.preSlideInRunnable = preSlideInRunnable;
    this.slideInOptions = slideInOptions;
  }

  @Override
  @UIThreadUnsafe
  public void run(ProgressIndicator indicator) {
    preSlideInRunnable.run();

    // `preSlideInRunnable` may perform some sneakily-asynchronous operations (e.g. checkoutRemoteBranch).
    // The high-level method used within the runnable does not allow us to schedule the tasks after them.
    // (Stepping deeper is not an option since we would lose some important logic or become very dependent on the internals of git4idea).
    // Hence, we wait for the creation of the branch (with exponential backoff).
    waitForCreationOfLocalBranch();

    Path macheteFilePath = gitRepository.getMacheteFilePath();

    val childEntryByName = branchLayout.getEntryByName(slideInOptions.getName());
    BranchLayoutEntry entryToSlideIn;
    BranchLayout targetBranchLayout;
    if (childEntryByName != null) {
      if (slideInOptions.shouldReattach()) {
        entryToSlideIn = childEntryByName;
        targetBranchLayout = branchLayout;
      } else {
        entryToSlideIn = childEntryByName.withChildren(List.empty());
        targetBranchLayout = branchLayout.slideOut(slideInOptions.getName());
      }

    } else {
      entryToSlideIn = new BranchLayoutEntry(slideInOptions.getName(), /* customAnnotation */ null,
          /* children */ List.empty());
      targetBranchLayout = branchLayout;
    }

    val newBranchLayout = deriveNewBranchLayout(targetBranchLayout, entryToSlideIn);
    if (newBranchLayout == null) {
      return;
    }

    Try.run(() -> branchLayoutWriter.write(macheteFilePath, newBranchLayout, /* backupOldLayout */ true))
        .onFailure(t -> VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
            /* title */ getString(
                "action.GitMachete.BaseSlideInBelowAction.notification.title.branch-layout-write-fail"),
            getMessageOrEmpty(t)));
  }

  abstract @Nullable BranchLayout deriveNewBranchLayout(BranchLayout targetBranchLayout, BranchLayoutEntry entryToSlideIn);

  @UIThreadUnsafe
  private @Nullable GitLocalBranch findLocalBranch() {
    return gitRepository.getBranches().findLocalBranch(slideInOptions.getName());
  }

  @UIThreadUnsafe
  private void waitForCreationOfLocalBranch() {
    try {
      //  6 attempts, usually 3 are enough
      final int TIMEOUT = 2048;
      long SLEEP_DURATION = 64;
      while (findLocalBranch() == null && SLEEP_DURATION <= TIMEOUT) {
        Thread.sleep(SLEEP_DURATION);
        SLEEP_DURATION *= 2;
      }
    } catch (InterruptedException e) {
      VcsNotifier.getInstance(project).notifyWeakError(/* displayId */ null,
          /* title */ "",
          getString("action.GitMachete.BaseSlideInBackgroundable.notification.message.wait-interrupted")
              .format(slideInOptions.getName()));
    }

    if (findLocalBranch() == null) {
      VcsNotifier.getInstance(project).notifyWeakError(/* displayId */ null,
          /* title */ "",
          getString("action.GitMachete.BaseSlideInBackgroundable.notification.message.timeout")
              .format(slideInOptions.getName()));
    }
  }

  protected static String getMessageOrEmpty(Throwable t) {
    return t.getMessage() != null ? t.getMessage() : "";
  }
}
