package com.virtuslab.gitmachete.frontend.actions.common;

import static com.virtuslab.gitmachete.frontend.actions.common.FetchUpToDateTimeoutStatus.FETCH_ALL_UP_TO_DATE_TIMEOUT_AS_STRING;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import git4idea.repo.GitRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.ILocalBranchReference;
import com.virtuslab.gitmachete.backend.api.IRemoteTrackingBranchReference;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.FastForwardMergeBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.qual.async.ContinuesInBackground;

@ExtensionMethod(GitMacheteBundle.class)
@RequiredArgsConstructor
public final class Pull {

  private final GitRepository gitRepository;
  private final ILocalBranchReference localBranch;
  private final IRemoteTrackingBranchReference remoteBranch;

  @ContinuesInBackground
  public void run() {
    run(() -> {});
  }

  @ContinuesInBackground
  public void run(@UI Runnable doInUIThreadWhenReady) {
    val mergeProps = new MergeProps(
        /* movingBranch */ localBranch,
        /* stayingBranch */ remoteBranch);

    val isUpToDate = FetchUpToDateTimeoutStatus.isUpToDate(gitRepository);
    val fetchNotificationPrefix = isUpToDate
        ? getNonHtmlString("action.GitMachete.Pull.notification.prefix.no-fetch-performed")
            .fmt(FETCH_ALL_UP_TO_DATE_TIMEOUT_AS_STRING)
        : getNonHtmlString("action.GitMachete.Pull.notification.prefix.fetch-performed");
    val fetchNotificationTextPrefix = fetchNotificationPrefix + (fetchNotificationPrefix.isEmpty() ? "" : " ");
    val fastForwardBackgroundable = new FastForwardMergeBackgroundable(gitRepository, mergeProps, fetchNotificationTextPrefix,
        doInUIThreadWhenReady);

    if (isUpToDate) {
      fastForwardBackgroundable.queue();
    } else {
      updateRepositoryFetchBackgroundable(/* onSuccessRunnable */ () -> fastForwardBackgroundable.queue());
    }
  }

  @ContinuesInBackground
  private void updateRepositoryFetchBackgroundable(Runnable onSuccessRunnable) {
    val remoteName = remoteBranch.getRemoteName();

    String taskTitle = getString("action.GitMachete.BasePullAction.task-title");

    new FetchBackgroundable(
        gitRepository,
        remoteName,
        /* refspec */ null, // let's use the default refspec for the given remote, as defined in git config
        taskTitle,
        getNonHtmlString("action.GitMachete.BasePullAction.notification.title.pull-fail").fmt(remoteBranch.getName()),
        getString("action.GitMachete.BasePullAction.notification.title.pull-success.HTML").fmt(remoteBranch.getName())) {

      @Override
      @UIEffect
      public void onSuccess() {
        String repoName = gitRepository.getRoot().getName();
        FetchUpToDateTimeoutStatus.update(repoName);
        onSuccessRunnable.run();
      }
    }.queue();
  }
}
