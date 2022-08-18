package com.virtuslab.gitmachete.frontend.actions.common;

import static com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable.LOCAL_REPOSITORY_NAME;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.createRefspec;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.project.Project;
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
      MergeProps mergeProps, @Untainted String fetchNotificationPrefix, boolean insertNewlineAfterPrefix) {
    val stayingFullName = mergeProps.getStayingBranch().getFullName();
    val movingFullName = mergeProps.getMovingBranch().getFullName();
    val refspecFromChildToParent = createRefspec(stayingFullName, movingFullName, /* allowNonFastForward */ false);
    val stayingName = mergeProps.getStayingBranch().getName();
    val movingName = mergeProps.getMovingBranch().getName();
    val newLineSymbolHtml = insertNewlineAfterPrefix ? "<br/>" : "";
    @SuppressWarnings("regexp") val newLineSymbolNonHtml = insertNewlineAfterPrefix ? "\n" : "";
    val successFFMergeNotification = fetchNotificationPrefix + newLineSymbolHtml
        + getString("action.GitMachete.BaseFastForwardMergeToParentAction.notification.title.ff-success.HTML")
            .format(stayingName, movingName);
    String failFFMergeNotification = fetchNotificationPrefix + newLineSymbolNonHtml
        + getNonHtmlString("action.GitMachete.BaseFastForwardMergeToParentAction.notification.title.ff-fail").format(
            stayingName,
            movingName);
    new FetchBackgroundable(
        project,
        gitRepository,
        LOCAL_REPOSITORY_NAME,
        refspecFromChildToParent,
        getString("action.GitMachete.BaseFastForwardMergeToParentAction.task-title"),
        getNonHtmlString("action.GitMachete.BaseFastForwardMergeToParentAction.task-subtitle"),
        failFFMergeNotification,
        successFFMergeNotification).queue();
  }

  public static void perform(Project project,
      GitRepository gitRepository,
      MergeProps mergeProps, @Untainted String fetchNotificationPrefix, boolean insertNewlineAfterPrefix) {
    val stayingName = mergeProps.getStayingBranch().getName();
    val movingName = mergeProps.getMovingBranch().getName();
    val currentBranchName = Option.of(gitRepository.getCurrentBranch()).map(b -> b.getName()).getOrNull();
    val newLineSymbolHtml = insertNewlineAfterPrefix ? "<br/>" : "";
    val failFFMergeNotification = fetchNotificationPrefix + newLineSymbolHtml
        + getString("action.GitMachete.BaseFastForwardMergeToParentAction.notification.title.ff-fail").format(stayingName,
            movingName);
    new CheckRemoteBranchBackgroundable(project, gitRepository, stayingName, failFFMergeNotification) {
      @Override
      @UIEffect
      public void onSuccess() {
        if (mergeProps.getMovingBranch().getName().equals(currentBranchName)) {
          mergeCurrentBranch(project, gitRepository, mergeProps);
        } else {
          mergeNonCurrentBranch(project, gitRepository, mergeProps, fetchNotificationPrefix, insertNewlineAfterPrefix);
        }
      }
    }.queue();
  }
}
