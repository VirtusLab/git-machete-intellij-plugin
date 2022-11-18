package com.virtuslab.gitmachete.frontend.actions.common;

import static com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable.LOCAL_REPOSITORY_NAME;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.createRefspec;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.project.Project;
import git4idea.GitReference;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.tainting.qual.Untainted;

import com.virtuslab.gitmachete.frontend.actions.backgroundables.CheckRemoteBranchBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.MergeCurrentBranchFastForwardOnlyBackgroundable;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;

@ExtensionMethod(GitMacheteBundle.class)
public final class FastForwardMerge {

  private FastForwardMerge() {}

  private static void mergeCurrentBranch(Project project,
      GitRepository gitRepository,
      MergeProps mergeProps) {
    new MergeCurrentBranchFastForwardOnlyBackgroundable(project, gitRepository, mergeProps.getStayingBranch()).queue();
  }

  private static void mergeNonCurrentBranch(Project project,
      GitRepository gitRepository,
      MergeProps mergeProps, @Untainted String fetchNotificationTextPrefix) {
    val stayingFullName = mergeProps.getStayingBranch().getFullName();
    val movingFullName = mergeProps.getMovingBranch().getFullName();
    val refspecFromChildToParent = createRefspec(stayingFullName, movingFullName, /* allowNonFastForward */ false);
    val stayingName = mergeProps.getStayingBranch().getName();
    val movingName = mergeProps.getMovingBranch().getName();
    val successFFMergeNotification = getString(
        "action.GitMachete.BaseFastForwardMergeToParentAction.notification.text.ff-success")
            .format(stayingName, movingName);
    val failFFMergeNotification = getNonHtmlString(
        "action.GitMachete.BaseFastForwardMergeToParentAction.notification.text.ff-fail")
            .format(stayingName, movingName);
    new FetchBackgroundable(
        project,
        gitRepository,
        LOCAL_REPOSITORY_NAME,
        refspecFromChildToParent,
        getString("action.GitMachete.BaseFastForwardMergeToParentAction.task-title"),
        getNonHtmlString("action.GitMachete.BaseFastForwardMergeToParentAction.task-subtitle"),
        fetchNotificationTextPrefix + failFFMergeNotification,
        fetchNotificationTextPrefix + successFFMergeNotification).queue();
  }

  public static void perform(Project project,
      GitRepository gitRepository,
      MergeProps mergeProps,
      @Untainted String fetchNotificationTextPrefix) {
    val stayingName = mergeProps.getStayingBranch().getName();
    val currentBranchName = Option.of(gitRepository.getCurrentBranch()).map(GitReference::getName).getOrNull();
    val failFfMergeNotificationTitle = getString(
        "action.GitMachete.BaseFastForwardMergeToParentAction.notification.title.ff-fail");
    new CheckRemoteBranchBackgroundable(project, gitRepository, stayingName, failFfMergeNotificationTitle,
        fetchNotificationTextPrefix) {
      @Override
      @UIEffect
      public void onSuccess() {
        if (mergeProps.getMovingBranch().getName().equals(currentBranchName)) {
          mergeCurrentBranch(project, gitRepository, mergeProps);
        } else {
          mergeNonCurrentBranch(project, gitRepository, mergeProps, fetchNotificationTextPrefix);
        }
      }
    }.queue();
  }
}
