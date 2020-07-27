package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.AheadOfRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedFromAndNewerThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedFromAndOlderThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.Untracked;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import git4idea.GitLocalBranch;
import git4idea.push.GitPushSource;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.dialogs.GitPushDialog;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public abstract class BasePushBranchAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyProject,
      ISyncToRemoteStatusDependentAction {

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }

  @Override
  public @I18nFormat({}) String getActionName() {
    return getString("action.GitMachete.BasePushBranchAction.action-name");
  }

  @Override
  public @I18nFormat({}) String getDescriptionActionName() {
    return getString("action.GitMachete.BasePushBranchAction.description-action-name");
  }

  @Override
  public List<SyncToRemoteStatus.Relation> getEligibleRelations() {
    return List.of(
        AheadOfRemote,
        DivergedFromAndNewerThanRemote,
        DivergedFromAndOlderThanRemote,
        Untracked);
  }

  @Override
  @UIEffect
  public void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);
    syncToRemoteStatusDependentActionUpdate(anActionEvent);
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {

    var project = getProject(anActionEvent);
    var gitRepository = getSelectedGitRepository(anActionEvent);
    var branchName = getNameOfBranchUnderAction(anActionEvent);
    var relation = branchName.flatMap(bn -> getGitMacheteBranchByName(anActionEvent, bn))
        .map(b -> b.getSyncToRemoteStatus())
        .map(strs -> strs.getRelation());

    if (branchName.isDefined() && gitRepository.isDefined() && relation.isDefined()) {
      boolean isForcePushRequired = List.of(DivergedFromAndNewerThanRemote, DivergedFromAndOlderThanRemote)
          .contains(relation.get());
      doPush(project, gitRepository.get(), branchName.get(), isForcePushRequired);
    }
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
