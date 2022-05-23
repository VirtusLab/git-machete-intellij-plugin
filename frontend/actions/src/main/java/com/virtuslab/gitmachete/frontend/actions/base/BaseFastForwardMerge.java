package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable.LOCAL_REPOSITORY_NAME;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.createRefspec;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import lombok.val;

import com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.MergeCurrentBranchFastForwardOnlyBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.common.MergeProps;

public abstract class BaseFastForwardMerge {

  public static void doCurrentBranch(Project project,
                                     GitRepository gitRepository,
                                     MergeProps mergeProps) {
    new MergeCurrentBranchFastForwardOnlyBackgroundable(project, gitRepository, mergeProps.getStayingBranch()).queue();
  }

  public static void doNonCurrentBranch(Project project,
                                        GitRepository gitRepository,
                                        MergeProps mergeProps) {
    val stayingFullName = mergeProps.getStayingBranch().getFullName();
    val movingFullName = mergeProps.getMovingBranch().getFullName();
    val refspecFromChildToParent = createRefspec(stayingFullName, movingFullName, /* allowNonFastForward */ false);

    val stayingName = mergeProps.getStayingBranch().getName();
    val movingName = mergeProps.getMovingBranch().getName();
    new FetchBackgroundable(
        project,
        gitRepository,
        LOCAL_REPOSITORY_NAME,
        refspecFromChildToParent,
        getString("action.GitMachete.BaseFastForwardMergeBranchToParentAction.task-title"),
        getString("action.GitMachete.BaseFastForwardMergeBranchToParentAction.task-subtitle"),
        format(getString("action.GitMachete.BaseFastForwardMergeBranchToParentAction.notification.title.ff-fail"),
            stayingName, movingName),
        format(getString("action.GitMachete.BaseFastForwardMergeBranchToParentAction.notification.title.ff-success"),
            stayingName, movingName))
                .queue();
  }
}
