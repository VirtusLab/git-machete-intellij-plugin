package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable.LOCAL_REPOSITORY_NAME;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.createRefspec;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory.GENERAL;

import java.util.Collections;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import git4idea.branch.GitBrancher;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import io.vavr.control.Option;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;

import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.common.MergeProps;

@CustomLog
public abstract class BaseMergeParentIntoBranchAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      ISyncToParentStatusDependentAction {

  @Override
  public LambdaLogger log() {
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
    val mergeProps = new MergeProps(
        /* movingBranchName */ nonRootStayingBranch,
        /* stayingBranchName */ nonRootStayingBranch.getParent());
    if (nonRootStayingBranch.getParent().getName().equals(currentBranchName)) {
      doMergeIntoCurrentBranch(project, gitRepository, mergeProps);
    } else {
      doMergeIntoNonCurrentBranch(project, gitRepository, mergeProps);
    }
  }

  public static void doMergeIntoCurrentBranch(Project project,
      GitRepository gitRepository,
      MergeProps branchToCheckoutName) {

    //    log().debug(() -> "Queuing '${selectedBranchName.get()}' branch checkout background task");
    //    new Task.Backgroundable(project, getString("action.GitMachete.CheckoutSelectedBranchAction.task-title")) {
    //      @Override
    //      @UIThreadUnsafe
    //      public void run(ProgressIndicator indicator) {
    // TODO (#772): switch to constructor that does not take git once we no longer support 2021.2
    String movingBranchName = branchToCheckoutName.getMovingBranch().getName();
    GitBrancher.getInstance(project)
        .merge(movingBranchName, GitBrancher.DeleteOnMergeOption.NOTHING, Collections.singletonList(gitRepository));

    //      }
    //    }.queue();

  }

  public static void doMergeIntoNonCurrentBranch(Project project,
      GitRepository gitRepository,
      MergeProps ffmProps) {
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
