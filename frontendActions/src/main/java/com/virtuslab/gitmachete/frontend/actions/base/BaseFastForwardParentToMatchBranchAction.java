package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable.LOCAL_REPOSITORY_NAME;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.createRefspec;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory.GENERAL;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;

import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.MergeCurrentBranchFastForwardOnlyBackgroundable;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public abstract class BaseFastForwardParentToMatchBranchAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProviderWithLogging,
      ISyncToParentStatusDependentAction {

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }

  @Override
  public @I18nFormat({}) String getActionNameForDisabledDescription() {
    return getString("action.GitMachete.BaseFastForwardParentToMatchBranchAction.description-action-name");
  }

  @Override
  public @I18nFormat({GENERAL, GENERAL}) String getEnabledDescriptionFormat() {
    return getString("action.GitMachete.BaseFastForwardParentToMatchBranchAction.description");
  }

  @Override
  public List<SyncToParentStatus> getEligibleStatuses() {
    return List.of(SyncToParentStatus.InSync);
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

    var project = getProject(anActionEvent);
    var gitRepository = getSelectedGitRepositoryWithLogging(anActionEvent).getOrNull();
    var targetBranchName = getNameOfBranchUnderActionWithLogging(anActionEvent).getOrNull();
    if (gitRepository == null || targetBranchName == null) {
      return;
    }

    var targetBranch = getGitMacheteBranchByNameWithLogging(anActionEvent, targetBranchName).getOrNull();
    if (targetBranch == null) {
      return;
    }
    // This is guaranteed by `syncToParentStatusDependentActionUpdate` invoked from `onUpdate`.
    assert targetBranch.isNonRoot() : "Target branch provided to fast forward is a root";

    var currentBranchName = Option.of(gitRepository.getCurrentBranch()).map(b -> b.getName()).getOrNull();
    if (targetBranch.asNonRoot().getParent().getName().equals(currentBranchName)) {
      doFastForwardCurrentBranch(project, gitRepository, targetBranch.asNonRoot());
    } else {
      doFastForwardNonCurrentBranch(project, gitRepository, targetBranch.asNonRoot());
    }
  }

  private void doFastForwardCurrentBranch(Project project,
      GitRepository gitRepository,
      INonRootManagedBranchSnapshot targetBranch) {

    var taskTitle = getString("action.GitMachete.BaseFastForwardParentToMatchBranchAction.task-title");

    new MergeCurrentBranchFastForwardOnlyBackgroundable(project, gitRepository, taskTitle, targetBranch.getName()).queue();
  }

  private void doFastForwardNonCurrentBranch(Project project,
      GitRepository gitRepository,
      INonRootManagedBranchSnapshot targetBranch) {
    var localFullName = targetBranch.getFullName();
    var parentLocalFullName = targetBranch.getParent().getFullName();
    var refspecFromChildToParent = createRefspec(localFullName, parentLocalFullName, /* allowNonFastForward */ false);

    new FetchBackgroundable(project, gitRepository, LOCAL_REPOSITORY_NAME, refspecFromChildToParent,
        getString("action.GitMachete.BaseFastForwardParentToMatchBranchAction.task-title")).queue();
  }
}
