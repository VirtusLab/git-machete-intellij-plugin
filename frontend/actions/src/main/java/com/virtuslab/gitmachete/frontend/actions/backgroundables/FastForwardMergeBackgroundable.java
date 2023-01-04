package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import static com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable.LOCAL_REPOSITORY_NAME;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.createRefspec;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.fmt;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.progress.ProgressIndicator;
import git4idea.GitReference;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;
import lombok.val;
import org.checkerframework.checker.tainting.qual.Untainted;

import com.virtuslab.gitmachete.frontend.actions.common.MergeProps;
import com.virtuslab.qual.async.ContinuesInBackground;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

public class FastForwardMergeBackgroundable extends CheckRemoteBranchBackgroundable {

  private final GitRepository gitRepository;

  private final MergeProps mergeProps;

  private final @Untainted String fetchNotificationTextPrefix;

  public FastForwardMergeBackgroundable(GitRepository gitRepository, MergeProps mergeProps,
      @Untainted String fetchNotificationTextPrefix) {
    super(gitRepository,
        /* remoteBranchName */ mergeProps.getStayingBranch().getName(),
        /* taskFailNotificationTitle */ getString(
            "action.GitMachete.BaseFastForwardMergeToParentAction.notification.title.ff-fail"),
        /* taskFailNotificationPrefix */ fetchNotificationTextPrefix);
    this.gitRepository = gitRepository;
    this.mergeProps = mergeProps;
    this.fetchNotificationTextPrefix = fetchNotificationTextPrefix;
  }

  @Override
  @ContinuesInBackground
  @UIThreadUnsafe
  public void run(ProgressIndicator indicator) {
    super.run(indicator);

    val currentBranchName = Option.of(gitRepository.getCurrentBranch()).map(GitReference::getName).getOrNull();
    if (mergeProps.getMovingBranch().getName().equals(currentBranchName)) {
      mergeCurrentBranch();
    } else {
      mergeNonCurrentBranch();
    }
  }

  @ContinuesInBackground
  private void mergeCurrentBranch() {
    new MergeCurrentBranchFastForwardOnlyBackgroundable(gitRepository, mergeProps.getStayingBranch()).queue();
  }

  @ContinuesInBackground
  private void mergeNonCurrentBranch() {
    val stayingFullName = mergeProps.getStayingBranch().getFullName();
    val movingFullName = mergeProps.getMovingBranch().getFullName();
    val refspecFromChildToParent = createRefspec(stayingFullName, movingFullName, /* allowNonFastForward */ false);
    val stayingName = mergeProps.getStayingBranch().getName();
    val movingName = mergeProps.getMovingBranch().getName();
    val successFFMergeNotification = fmt(getString(
        "action.GitMachete.BaseFastForwardMergeToParentAction.notification.text.ff-success"), stayingName, movingName);
    val failFFMergeNotification = fmt(getNonHtmlString(
        "action.GitMachete.BaseFastForwardMergeToParentAction.notification.text.ff-fail"), stayingName, movingName);
    new FetchBackgroundable(
        gitRepository,
        LOCAL_REPOSITORY_NAME,
        refspecFromChildToParent,
        getString("action.GitMachete.BaseFastForwardMergeToParentAction.task-title"),
        fetchNotificationTextPrefix + failFFMergeNotification,
        fetchNotificationTextPrefix + successFFMergeNotification).queue();
  }
}
