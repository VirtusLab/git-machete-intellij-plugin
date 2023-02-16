package com.virtuslab.gitmachete.frontend.actions.common;

import static com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable.LOCAL_REPOSITORY_NAME;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.createRefspec;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.util.Objects;

import git4idea.GitReference;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.tainting.qual.Untainted;

import com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.MergeCurrentBranchFastForwardOnlyBackgroundable;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.qual.async.ContinuesInBackground;

@ExtensionMethod({GitMacheteBundle.class, Objects.class})
@RequiredArgsConstructor
public class FastForwardMerge {

  private final GitRepository gitRepository;

  private final MergeProps mergeProps;

  private final @Untainted String notificationTextPrefix;

  @ContinuesInBackground
  public void run() {
    run(() -> {});
  }

  @ContinuesInBackground
  public void run(@UI Runnable doInUIThreadWhenReady) {

    val currentBranchName = Option.of(gitRepository.getCurrentBranch()).map(GitReference::getName).getOrNull();
    if (mergeProps.getMovingBranch().getName().equals(currentBranchName)) {
      mergeCurrentBranch(doInUIThreadWhenReady);
    } else {
      mergeNonCurrentBranch(doInUIThreadWhenReady);
    }
  }

  @ContinuesInBackground
  private void mergeCurrentBranch(@UI Runnable doInUIThreadWhenReady) {
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
  private void mergeNonCurrentBranch(@UI Runnable doInUIThreadWhenReady) {
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
        notificationTextPrefix + failFFMergeNotification,
        notificationTextPrefix + successFFMergeNotification) {
      @Override
      @UIEffect
      public void onSuccess() {
        super.onSuccess();

        doInUIThreadWhenReady.run();
      }
    }.queue();
  }
}
