package com.virtuslab.gitmachete.frontend.actions.common;

import static com.virtuslab.gitmachete.frontend.actions.common.FetchUpToDateTimeoutStatus.FETCH_ALL_UP_TO_DATE_TIMEOUT_AS_STRING;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.fmt;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.repo.GitRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.ILocalBranchReference;
import com.virtuslab.gitmachete.backend.api.IRemoteTrackingBranchReference;
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
    val fastForwardMerge = new FastForwardMerge(gitRepository, mergeProps, fetchNotificationPrefix);

    if (isUpToDate) {
      fastForwardMerge.run(doInUIThreadWhenReady);
    } else {
      fetchAndThenFFMerge(fetchNotificationPrefix, /* onSuccessRunnable */ () -> fastForwardMerge.run(doInUIThreadWhenReady));
    }
  }

  @ContinuesInBackground
  private void fetchAndThenFFMerge(String fetchNotificationPrefix, Runnable onSuccessRunnable) {
    String remoteName = remoteBranch.getRemoteName();
    String remoteBranchName = remoteBranch.getName();

    String taskTitle = getString("action.GitMachete.BasePullAction.task-title");
    new FetchBackgroundable(
        gitRepository,
        remoteName,
        /* refspec */ null, // let's use the default refspec for the given remote, as defined in git config
        taskTitle,
        getNonHtmlString("action.GitMachete.Pull.notification.title.pull-fail").fmt(remoteBranchName),
        getString("action.GitMachete.Pull.notification.title.pull-success.HTML").fmt(remoteBranchName)) {

      @Override
      @UIEffect
      public void onSuccess() {
        String repoName = gitRepository.getRoot().getName();
        FetchUpToDateTimeoutStatus.update(repoName);

        if (gitRepository.getBranches().findBranchByName(remoteBranchName) == null) {
          val errorMessage = fmt(getString("action.GitMachete.Pull.notification.remote-branch-missing.text"), remoteBranchName);
          val taskFailNotificationTitle = getString(
              "action.GitMachete.BaseFastForwardMergeToParentAction.notification.title.ff-fail");
          VcsNotifier.getInstance(gitRepository.getProject()).notifyError(
              /* displayId */ null, taskFailNotificationTitle, fetchNotificationPrefix + " " + errorMessage);
        } else {
          onSuccessRunnable.run();
        }
      }
    }.queue();
  }
}
