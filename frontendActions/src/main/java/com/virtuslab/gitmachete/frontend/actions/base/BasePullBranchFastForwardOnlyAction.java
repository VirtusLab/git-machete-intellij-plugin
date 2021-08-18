package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.createRefspec;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;

import com.virtuslab.gitmachete.backend.api.ILocalBranchReference;
import com.virtuslab.gitmachete.backend.api.IRemoteTrackingBranchReference;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.PullCurrentBranchFastForwardOnlyBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.common.FastForwardMergeProps;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.actions.toolbar.FetchAllRemotesAction;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public abstract class BasePullBranchFastForwardOnlyAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyGitMacheteRepository,
      ISyncToRemoteStatusDependentAction {

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }

  @Override
  public @I18nFormat({}) String getActionName() {
    return getString("action.GitMachete.BasePullBranchFastForwardOnlyAction.action-name");
  }

  @Override
  public @I18nFormat({}) String getActionNameForDescription() {
    return getString("action.GitMachete.BasePullBranchFastForwardOnlyAction.description-action-name");
  }

  @Override
  public List<SyncToRemoteStatus> getEligibleStatuses() {
    return List.of(
        SyncToRemoteStatus.BehindRemote,
        SyncToRemoteStatus.InSyncToRemote);
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);
    syncToRemoteStatusDependentActionUpdate(anActionEvent);
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    log().debug("Performing");

    val project = getProject(anActionEvent);
    val gitRepository = getSelectedGitRepository(anActionEvent).getOrNull();
    val localBranchName = getNameOfBranchUnderAction(anActionEvent).getOrNull();
    val gitMacheteRepositorySnapshot = getGitMacheteRepositorySnapshot(anActionEvent).getOrNull();

    if (localBranchName != null && gitRepository != null && gitMacheteRepositorySnapshot != null) {
      val localBranch = gitMacheteRepositorySnapshot.getManagedBranchByName(localBranchName).getOrNull();
      if (localBranch == null) {
        // This is generally NOT expected, the action should never be triggered
        // for an unmanaged branch in the first place.
        log().warn("Branch '${localBranchName}' not found or not managed by Git Machete");
        return;
      }
      val remoteBranch = localBranch.getRemoteTrackingBranch().getOrNull();
      if (remoteBranch == null) {
        // This is generally NOT expected, the action should never be triggered
        // for an untracked branch in the first place (see `getEligibleRelations`)
        log().warn("Branch '${localBranchName}' does not have a remote tracking branch");
        return;
      }
      val currentBranchName = Option.of(gitRepository.getCurrentBranch()).map(b -> b.getName()).getOrNull();

      if (FetchAllRemotesAction.isUpToDate(gitRepository)) {
        val ffmProps = new FastForwardMergeProps(
            /* movingBranch */ localBranch,
            /* stayingBranch */ remoteBranch);

        if (localBranchName.equals(currentBranchName)) {
          BaseFastForwardMergeBranchToParentAction.doFastForwardCurrentBranch(project, gitRepository, ffmProps);
        } else {
          BaseFastForwardMergeBranchToParentAction.doFastForwardNonCurrentBranch(project, gitRepository, ffmProps);
        }

      } else {
        if (localBranchName.equals(currentBranchName)) {
          doPullCurrentBranchFastForwardOnly(project, gitRepository, remoteBranch);
        } else {
          doPullNonCurrentBranchFastForwardOnly(project, gitRepository, localBranch, remoteBranch);
        }
      }
    }
  }

  private void doPullCurrentBranchFastForwardOnly(Project project,
      GitRepository gitRepository,
      IRemoteTrackingBranchReference remoteBranch) {
    val taskTitle = getString("action.GitMachete.BasePullBranchFastForwardOnlyAction.task-title");
    new PullCurrentBranchFastForwardOnlyBackgroundable(project, gitRepository, taskTitle, remoteBranch).queue();
  }

  private void doPullNonCurrentBranchFastForwardOnly(Project project,
      GitRepository gitRepository,
      ILocalBranchReference localBranch,
      IRemoteTrackingBranchReference remoteBranch) {

    val remoteName = remoteBranch.getRemoteName();
    val remoteBranchFullNameAsLocalBranchOnRemote = remoteBranch.getFullNameAsLocalBranchOnRemote();
    val remoteBranchFullName = remoteBranch.getFullName();
    val localBranchFullName = localBranch.getFullName();

    // This strategy is used to fetch branch from remote repository to remote branch in our repository.
    val refspecFromRemoteRepoToOurRemoteBranch = createRefspec(remoteBranchFullNameAsLocalBranchOnRemote,
        remoteBranchFullName, /* allowNonFastForward */ true);

    // We want a fetch from remote branch in our repository
    // to local branch in our repository to only ever be fast-forward.
    val refspecFromOurRemoteBranchToOurLocalBranch = createRefspec(remoteBranchFullName,
        localBranchFullName, /* allowNonFastForward */ false);

    String taskTitle = getString("action.GitMachete.BasePullBranchFastForwardOnlyAction.task-title");

    val ourRemoteToOurLocalBranchFetchBackgroundable = new FetchBackgroundable(
        project,
        gitRepository,
        FetchBackgroundable.LOCAL_REPOSITORY_NAME,
        refspecFromOurRemoteBranchToOurLocalBranch,
        taskTitle,
        format(getString("action.GitMachete.BasePullBranchFastForwardOnlyAction.notification.title.pull-fail"),
            localBranch.getName()),
        format(getString("action.GitMachete.BasePullBranchFastForwardOnlyAction.notification.title.pull-success"),
            localBranch.getName()));

    new FetchBackgroundable(
        project,
        gitRepository,
        remoteName,
        refspecFromRemoteRepoToOurRemoteBranch,
        taskTitle,
        format(getString("action.GitMachete.BasePullBranchFastForwardOnlyAction.notification.title.pull-fail"),
            remoteBranch.getName()),
        format(getString("action.GitMachete.BasePullBranchFastForwardOnlyAction.notification.title.pull-success"),
            remoteBranch.getName())) {
      // We can only enqueue the update of local branch once the update of remote branch is completed.
      @Override
      @UIEffect
      public void onSuccess() {
        ourRemoteToOurLocalBranchFetchBackgroundable.queue();
      }
    }.queue();
  }
}
