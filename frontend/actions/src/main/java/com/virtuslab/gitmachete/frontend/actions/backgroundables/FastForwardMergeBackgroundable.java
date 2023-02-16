package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import static com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable.LOCAL_REPOSITORY_NAME;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.createRefspec;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.GitBranch;
import git4idea.GitReference;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;
import lombok.SneakyThrows;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.tainting.qual.Untainted;

import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.frontend.actions.common.MergeProps;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.qual.async.ContinuesInBackground;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod({GitMacheteBundle.class})
public class FastForwardMergeBackgroundable extends SideEffectingBackgroundable {

  private final GitRepository gitRepository;

  private final MergeProps mergeProps;

  private final @Untainted String fetchNotificationTextPrefix;

  private final @UI Runnable doInUIThreadWhenReady;

  public FastForwardMergeBackgroundable(GitRepository gitRepository, MergeProps mergeProps,
      @Untainted String fetchNotificationTextPrefix) {
    this(gitRepository, mergeProps, fetchNotificationTextPrefix, () -> {});
  }

  public FastForwardMergeBackgroundable(GitRepository gitRepository, MergeProps mergeProps,
      @Untainted String fetchNotificationTextPrefix, @UI Runnable doInUIThreadWhenReady) {
    super(gitRepository.getProject(), getString("action.GitMachete.FastForwardMergeBackgroundable.task-title"),
        "fast-forward merge");
    this.gitRepository = gitRepository;
    this.mergeProps = mergeProps;
    this.fetchNotificationTextPrefix = fetchNotificationTextPrefix;
    this.doInUIThreadWhenReady = doInUIThreadWhenReady;
  }

  @Override
  @ContinuesInBackground
  @SneakyThrows
  @UIThreadUnsafe
  public void doRun(ProgressIndicator indicator) {
    val remoteBranchName = mergeProps.getStayingBranch().getName();
    GitBranch targetBranch = gitRepository.getBranches().findBranchByName(remoteBranchName);
    if (targetBranch == null) {
      throw new GitMacheteException(
          getString("action.GitMachete.FastForwardMergeBackgroundable.notification.fail.text").fmt(remoteBranchName));
    }

    val currentBranchName = Option.of(gitRepository.getCurrentBranch()).map(GitReference::getName).getOrNull();
    if (mergeProps.getMovingBranch().getName().equals(currentBranchName)) {
      mergeCurrentBranch();
    } else {
      mergeNonCurrentBranch();
    }
  }

  /** Do not override {@code onSuccess}. Instead, pass {@code doInUIThreadWhenReady} callback to the constructor. */
  @Override
  public final void onSuccess() {}

  @ContinuesInBackground
  private void mergeCurrentBranch() {
    new MergeCurrentBranchFastForwardOnlyBackgroundable(gitRepository, mergeProps.getStayingBranch()) {
      @Override
      @UIEffect
      public void onSuccess() {
        super.onSuccess();

        doInUIThreadWhenReady.run();
      }
    }.queue();
  }

  @ContinuesInBackground
  private void mergeNonCurrentBranch() {
    val stayingFullName = mergeProps.getStayingBranch().getFullName();
    val movingFullName = mergeProps.getMovingBranch().getFullName();
    val refspecFromChildToParent = createRefspec(stayingFullName, movingFullName, /* allowNonFastForward */ false);
    val stayingName = mergeProps.getStayingBranch().getName();
    val movingName = mergeProps.getMovingBranch().getName();
    val successFFMergeNotification = getString(
        "action.GitMachete.BaseFastForwardMergeToParentAction.notification.text.ff-success").fmt(stayingName, movingName);
    val failFFMergeNotification = getNonHtmlString(
        "action.GitMachete.BaseFastForwardMergeToParentAction.notification.text.ff-fail").fmt(stayingName, movingName);
    new FetchBackgroundable(
        gitRepository,
        LOCAL_REPOSITORY_NAME,
        refspecFromChildToParent,
        getString("action.GitMachete.BaseFastForwardMergeToParentAction.task-title"),
        fetchNotificationTextPrefix + failFFMergeNotification,
        fetchNotificationTextPrefix + successFFMergeNotification) {
      @Override
      @UIEffect
      public void onSuccess() {
        super.onSuccess();

        doInUIThreadWhenReady.run();
      }
    }.queue();
  }

  @Override
  @UIEffect
  public void onThrowable(Throwable error) {
    String errorMessage = error.getMessage();
    if (errorMessage != null) {
      val taskFailNotificationTitle = getString(
          "action.GitMachete.BaseFastForwardMergeToParentAction.notification.title.ff-fail");
      VcsNotifier.getInstance(gitRepository.getProject()).notifyError(
          /* displayId */ null, taskFailNotificationTitle, fetchNotificationTextPrefix + errorMessage);
    }
  }

}
