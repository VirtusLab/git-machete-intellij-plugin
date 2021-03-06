package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable.LOCAL_REPOSITORY_NAME;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.createRefspec;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory.GENERAL;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;

import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.MergeCurrentBranchFastForwardOnlyBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.common.FastForwardMergeProps;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public abstract class BaseFastForwardMergeBranchToParentAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      ISyncToParentStatusDependentAction {

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }

  @Override
  public @I18nFormat({}) String getActionNameForDisabledDescription() {
    return getString("action.GitMachete.BaseFastForwardMergeBranchToParentAction.description-action-name");
  }

  @Override
  public @I18nFormat({GENERAL, GENERAL}) String getEnabledDescriptionFormat() {
    return getString("action.GitMachete.BaseFastForwardMergeBranchToParentAction.description");
  }

  @Override
  public List<SyncToParentStatus> getEligibleStatuses() {
    return List.of(SyncToParentStatus.InSync, SyncToParentStatus.InSyncButForkPointOff);
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);
    syncToParentStatusDependentActionUpdate(anActionEvent);
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {

    val project = getProject(anActionEvent);
    val gitRepository = getSelectedGitRepository(anActionEvent).getOrNull();
    val stayingBranchName = getNameOfBranchUnderAction(anActionEvent).getOrNull();
    if (gitRepository == null || stayingBranchName == null) {
      return;
    }

    val stayingBranch = getManagedBranchByName(anActionEvent, stayingBranchName).getOrNull();
    if (stayingBranch == null) {
      return;
    }
    // This is guaranteed by `syncToParentStatusDependentActionUpdate` invoked from `onUpdate`.
    assert stayingBranch.isNonRoot() : "Branch that would be fast-forwarded TO is a root";

    val currentBranchName = Option.of(gitRepository.getCurrentBranch()).map(b -> b.getName()).getOrNull();
    val nonRootStayingBranch = stayingBranch.asNonRoot();
    val ffmProps = new FastForwardMergeProps(
        /* movingBranchName */ nonRootStayingBranch.getParent(),
        /* stayingBranchName */ nonRootStayingBranch);
    if (nonRootStayingBranch.getParent().getName().equals(currentBranchName)) {
      doFastForwardCurrentBranch(project, gitRepository, ffmProps);
    } else {
      doFastForwardNonCurrentBranch(project, gitRepository, ffmProps);
    }
  }

  public static void doFastForwardCurrentBranch(Project project,
      GitRepository gitRepository,
      FastForwardMergeProps ffmProps) {
    new MergeCurrentBranchFastForwardOnlyBackgroundable(project, gitRepository, ffmProps.getStayingBranch()).queue();
  }

  public static void doFastForwardNonCurrentBranch(Project project,
      GitRepository gitRepository,
      FastForwardMergeProps ffmProps) {
    val stayingFullName = ffmProps.getStayingBranch().getFullName();
    val movingFullName = ffmProps.getMovingBranch().getFullName();
    val refspecFromChildToParent = createRefspec(stayingFullName, movingFullName, /* allowNonFastForward */ false);

    val stayingName = ffmProps.getStayingBranch().getName();
    val movingName = ffmProps.getMovingBranch().getName();
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
