package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.collection.List;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.common.Pull;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.qual.async.ContinuesInBackground;

@ExtensionMethod(GitMacheteBundle.class)
public abstract class BasePullAction extends BaseGitMacheteRepositoryReadyAction
    implements
      ISyncToRemoteStatusDependentAction,
      IWorktreeGuardedBranchAction {

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

    if (!anActionEvent.getPresentation().isEnabledAndVisible()) {
      return;
    }
    // Pull fast-forwards the target branch via `git fetch . <remote>:<local>`; git refuses to update
    // a local branch ref while it is checked out in another worktree (the same `refusing to fetch
    // into branch ... checked out at ...` failure as a bare `git pull` in that situation).
    disableIfBranchHeldByOtherWorktree(anActionEvent, getNameOfBranchUnderAction(anActionEvent));
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

      new Pull(gitRepository, localBranch, remoteBranch).run();
    }
  }

}
