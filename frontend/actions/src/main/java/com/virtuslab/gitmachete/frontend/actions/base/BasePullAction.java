package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.common.FetchUpToDateTimeoutStatus.FETCH_ALL_UP_TO_DATE_TIMEOUT_AS_STRING;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.actionSystem.AnActionEvent;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;

import com.virtuslab.gitmachete.backend.api.IRemoteTrackingBranchReference;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.FastForwardMergeBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.common.FetchUpToDateTimeoutStatus;
import com.virtuslab.gitmachete.frontend.actions.common.MergeProps;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.qual.async.ContinuesInBackground;

@ExtensionMethod(GitMacheteBundle.class)
public abstract class BasePullAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyGitMacheteRepository,
      ISyncToRemoteStatusDependentAction {

  @Override
  protected boolean isSideEffecting() {
    return true;
  }

  @Override
  public @I18nFormat({}) String getActionName() {
    return getString("action.GitMachete.BasePullAction.action-name");
  }

  @Override
  public @I18nFormat({}) String getActionNameForDescription() {
    return getString("action.GitMachete.BasePullAction.description-action-name");
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
  @ContinuesInBackground
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    log().debug("Performing");

    val gitRepository = getSelectedGitRepository(anActionEvent);
    val localBranchName = getNameOfBranchUnderAction(anActionEvent);
    val gitMacheteRepositorySnapshot = getGitMacheteRepositorySnapshot(anActionEvent);

    if (localBranchName != null && gitRepository != null && gitMacheteRepositorySnapshot != null) {
      val localBranch = gitMacheteRepositorySnapshot.getManagedBranchByName(localBranchName);
      if (localBranch == null) {
        // This is generally NOT expected, the action should never be triggered
        // for an unmanaged branch in the first place.
        log().warn("Branch '${localBranchName}' not found or not managed by Git Machete");
        return;
      }
      val remoteBranch = localBranch.getRemoteTrackingBranch();
      if (remoteBranch == null) {
        // This is generally NOT expected, the action should never be triggered
        // for an untracked branch in the first place (see `getEligibleRelations`)
        log().warn("Branch '${localBranchName}' does not have a remote tracking branch");
        return;
      }

      val mergeProps = new MergeProps(
          /* movingBranchName */ localBranch,
          /* stayingBranchName */ remoteBranch);

      val isUpToDate = FetchUpToDateTimeoutStatus.isUpToDate(gitRepository);
      val fetchNotificationPrefix = isUpToDate
          ? getNonHtmlString("action.GitMachete.BasePullAction.notification.prefix.no-fetch-perform")
              .fmt(FETCH_ALL_UP_TO_DATE_TIMEOUT_AS_STRING)
          : getNonHtmlString("action.GitMachete.BasePullAction.notification.prefix.fetch-perform");
      val fetchNotificationTextPrefix = fetchNotificationPrefix + (fetchNotificationPrefix.isEmpty() ? "" : " ");
      val fastForwardBackgroundable = new FastForwardMergeBackgroundable(gitRepository, mergeProps,
          fetchNotificationTextPrefix);

      if (isUpToDate) {
        fastForwardBackgroundable.queue();
      } else {
        updateRepositoryFetchBackgroundable(gitRepository, remoteBranch,
            /* onSuccessRunnable */ () -> fastForwardBackgroundable.queue());
      }
    }
  }

  @ContinuesInBackground
  private void updateRepositoryFetchBackgroundable(
      GitRepository gitRepository,
      IRemoteTrackingBranchReference remoteBranch,
      Runnable onSuccessRunnable) {
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
