package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.AheadOfRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.DivergedFromAndNewerThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.DivergedFromAndOlderThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Untracked;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import git4idea.GitLocalBranch;
import git4idea.config.GitSharedSettings;
import git4idea.push.GitPushSource;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.dialogs.GitPushDialog;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;

@ExtensionMethod(GitMacheteBundle.class)
@CustomLog
public abstract class BasePushAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      ISyncToRemoteStatusDependentAction {

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  @Override
  public @I18nFormat({}) String getActionName() {
    return getString("action.GitMachete.BasePushAction.action-name");
  }

  @Override
  public @I18nFormat({}) String getActionNameForDescription() {
    return getString("action.GitMachete.BasePushAction.description-action-name");
  }

  @Override
  public List<SyncToRemoteStatus> getEligibleStatuses() {
    return List.of(
        AheadOfRemote,
        DivergedFromAndNewerThanRemote,
        DivergedFromAndOlderThanRemote,
        Untracked);
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);

    syncToRemoteStatusDependentActionUpdate(anActionEvent);

    val branchName = getNameOfBranchUnderAction(anActionEvent);
    val managedBranchByName = getManagedBranchByName(anActionEvent, branchName);
    val relation = managedBranchByName != null
        ? managedBranchByName.getRelationToRemote().getSyncToRemoteStatus()
        : null;
    val project = getProject(anActionEvent);

    if (branchName != null && relation != null && isForcePushRequired(relation)) {
      if (GitSharedSettings.getInstance(project).isBranchProtected(branchName)) {
        val presentation = anActionEvent.getPresentation();
        presentation.setDescription(
            getNonHtmlString("action.GitMachete.BasePushAction.force-push-disabled-for-protected-branch")
                .format(branchName));
        presentation.setEnabled(false);
      }
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {

    val project = getProject(anActionEvent);
    val gitRepository = getSelectedGitRepository(anActionEvent);
    val branchName = getNameOfBranchUnderAction(anActionEvent);
    val managedBranchByName = getManagedBranchByName(anActionEvent, branchName);
    val relation = managedBranchByName != null ? managedBranchByName.getRelationToRemote().getSyncToRemoteStatus() : null;

    if (branchName != null && gitRepository != null && relation != null) {
      val isForcePushRequired = isForcePushRequired(relation);
      doPush(project, gitRepository, branchName, isForcePushRequired);
    }
  }

  private boolean isForcePushRequired(SyncToRemoteStatus syncToRemoteStatus) {
    return List.of(DivergedFromAndNewerThanRemote, DivergedFromAndOlderThanRemote).contains(syncToRemoteStatus);
  }

  @UIEffect
  private void doPush(Project project,
      GitRepository preselectedRepository,
      String branchName,
      boolean isForcePushRequired) {
    @Nullable GitLocalBranch localBranch = preselectedRepository.getBranches().findLocalBranch(branchName);

    if (localBranch != null) {
      new GitPushDialog(project, List.of(preselectedRepository), GitPushSource.create(localBranch), isForcePushRequired).show();
    } else {
      log().warn("Skipping the action because provided branch ${branchName} was not found in repository");
    }
  }
}
