package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils.getMacheteFilePath;

import java.nio.file.Path;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.GitLocalBranch;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import io.vavr.control.Try;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BranchLayoutEntry;
import com.virtuslab.branchlayout.api.EntryDoesNotExistException;
import com.virtuslab.branchlayout.api.EntryIsDescendantOfException;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.IBranchLayoutEntry;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.frontend.actions.dialogs.SlideInOptions;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

public class SlideInBackgroundable extends Task.Backgroundable {

  private final GitRepository gitRepository;
  private final IBranchLayout branchLayout;
  private final IBranchLayoutWriter branchLayoutWriter;
  private final Runnable preSlideInRunnable;
  private final SlideInOptions slideInOptions;
  private final String parentName;
  private final VcsNotifier notifier;

  public SlideInBackgroundable(
      Project project,
      GitRepository gitRepository,
      IBranchLayout branchLayout,
      IBranchLayoutWriter branchLayoutWriter,
      Runnable preSlideInRunnable,
      SlideInOptions slideInOptions,
      String parentName) {
    super(project, getString("action.GitMachete.BaseSlideInBranchBelowAction.task-title"));
    this.gitRepository = gitRepository;
    this.branchLayout = branchLayout;
    this.branchLayoutWriter = branchLayoutWriter;
    this.preSlideInRunnable = preSlideInRunnable;
    this.slideInOptions = slideInOptions;
    this.parentName = parentName;
    this.notifier = VcsNotifier.getInstance(project);
  }

  @Override
  @UIThreadUnsafe
  public void run(ProgressIndicator indicator) {
    preSlideInRunnable.run();

    // `preSlideInRunnable` may perform some sneakily-asynchronous operations (e.g. checkoutRemoteBranch).
    // The high-level method used within the runnable do not allow us to schedule the tasks after them.
    // (Stepping deeper is not an option since we would lose some important logic or become very dependent on the internals of git4idea).
    // Hence we wait for the creation of the branch (with exponential backoff).
    waitForCreationOfLocalBranch();

    Path macheteFilePath = getMacheteFilePath(gitRepository);

    IBranchLayoutEntry childEntryByName = branchLayout.findEntryByName(slideInOptions.getName()).getOrNull();
    IBranchLayoutEntry entryToSlideIn;
    IBranchLayout targetBranchLayout;
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

    IBranchLayout newBranchLayout;
    try {
      newBranchLayout = targetBranchLayout.slideIn(parentName, entryToSlideIn);
    } catch (EntryDoesNotExistException e) {
      notifyError(
          format(getString("action.GitMachete.BaseSlideInBranchBelowAction.notification.message.entry-does-not-exist"),
              parentName),
          e);
      return;
    } catch (EntryIsDescendantOfException e) {
      notifyError(
          format(getString("action.GitMachete.BaseSlideInBranchBelowAction.notification.message.entry-is-descendant-of"),
              entryToSlideIn.getName(), parentName),
          e);
      return;
    }

    final IBranchLayout finalNewBranchLayout = newBranchLayout;
    Try.run(() -> branchLayoutWriter.write(macheteFilePath, finalNewBranchLayout, /* backupOldLayout */ true))
        .onFailure(t -> notifier.notifyError(
            /* title */ getString(
                "action.GitMachete.BaseSlideInBranchBelowAction.notification.title.branch-layout-write-fail"),
            getMessageOrEmpty(t)));
  }

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
      notifier.notifyWeakError(
          format(getString("action.GitMachete.BaseSlideInBranchBelowAction.notification.message.wait-interrupted"),
              slideInOptions.getName()));
    }

    if (findLocalBranch() == null) {
      notifier
          .notifyWeakError(format(getString("action.GitMachete.BaseSlideInBranchBelowAction.notification.message.timeout"),
              slideInOptions.getName()));
    }
  }

  private void notifyError(@Nullable String message, Throwable throwable) {
    notifier.notifyError(
        /* title */ format(getString("action.GitMachete.BaseSlideInBranchBelowAction.notification.title.slide-in-fail"),
            slideInOptions.getName()),
        message != null ? message : getMessageOrEmpty(throwable));
  }

  private static String getMessageOrEmpty(Throwable t) {
    return t.getMessage() != null ? t.getMessage() : "";
  }
}
