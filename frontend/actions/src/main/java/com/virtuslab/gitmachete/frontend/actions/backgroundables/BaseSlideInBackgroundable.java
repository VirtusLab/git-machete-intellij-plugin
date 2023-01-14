package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import static com.virtuslab.gitmachete.frontend.actions.common.BranchCreationUtils.waitForCreationOfLocalBranch;
import static com.virtuslab.gitmachete.frontend.common.WriteActionUtils.blockingRunWriteActionOnUIThread;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.nio.file.Path;
import java.util.Objects;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.BranchLayoutEntry;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.frontend.actions.common.SlideInOptions;
import com.virtuslab.gitmachete.frontend.file.MacheteFileWriter;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod({GitVfsUtils.class, GitMacheteBundle.class, Objects.class})
public abstract class BaseSlideInBackgroundable extends SideEffectingBackgroundable {

  protected final Project project;
  protected final GitRepository gitRepository;
  protected final BranchLayout branchLayout;
  protected final IBranchLayoutWriter branchLayoutWriter;
  protected final Runnable preSlideInRunnable;
  protected final SlideInOptions slideInOptions;

  public BaseSlideInBackgroundable(
      GitRepository gitRepository,
      BranchLayout branchLayout,
      IBranchLayoutWriter branchLayoutWriter,
      Runnable preSlideInRunnable,
      SlideInOptions slideInOptions) {
    super(gitRepository.getProject(), getString("action.GitMachete.BaseSlideInBackgroundable.task-title"), "slide-in");
    this.project = gitRepository.getProject();
    this.gitRepository = gitRepository;
    this.branchLayout = branchLayout;
    this.branchLayoutWriter = branchLayoutWriter;
    this.preSlideInRunnable = preSlideInRunnable;
    this.slideInOptions = slideInOptions;
  }

  @Override
  @UIThreadUnsafe
  public void doRun(ProgressIndicator indicator) {
    preSlideInRunnable.run();

    // `preSlideInRunnable` may perform some sneakily-asynchronous operations (e.g. checkoutRemoteBranch).
    // The high-level method used within the runnable does not allow us to schedule the tasks after them.
    // (Stepping deeper is not an option since we would lose some important logic or become very dependent on the internals of git4idea).
    // Hence, we wait for the creation of the branch (with exponential backoff).
    waitForCreationOfLocalBranch(gitRepository, slideInOptions.getName());

    Path macheteFilePath = gitRepository.getMacheteFilePath();

    val childEntryByName = branchLayout.getEntryByName(slideInOptions.getName());
    BranchLayoutEntry entryToSlideIn;
    BranchLayout targetBranchLayout;
    if (childEntryByName != null) {
      if (slideInOptions.shouldReattach()) {
        entryToSlideIn = childEntryByName.withCustomAnnotation(slideInOptions.getCustomAnnotation());
        targetBranchLayout = branchLayout;
      } else {
        entryToSlideIn = childEntryByName.withChildren(List.empty()).withCustomAnnotation(slideInOptions.getCustomAnnotation());
        targetBranchLayout = branchLayout.slideOut(slideInOptions.getName());
      }

    } else {
      entryToSlideIn = new BranchLayoutEntry(slideInOptions.getName(), slideInOptions.getCustomAnnotation(),
          /* children */ List.empty());
      targetBranchLayout = branchLayout;
    }

    val newBranchLayout = deriveNewBranchLayout(targetBranchLayout, entryToSlideIn);
    if (newBranchLayout == null) {
      return;
    }

    blockingRunWriteActionOnUIThread(() -> {
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
        exceptionMessage.requireNonNullElse(""));
  }

  abstract @Nullable BranchLayout deriveNewBranchLayout(BranchLayout targetBranchLayout, BranchLayoutEntry entryToSlideIn);

}
