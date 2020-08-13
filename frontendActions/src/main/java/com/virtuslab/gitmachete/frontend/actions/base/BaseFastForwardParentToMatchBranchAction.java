package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable.LOCAL_REPOSITORY_NAME;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory.GENERAL;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;

import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public abstract class BaseFastForwardParentToMatchBranchAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyProject,
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
  public void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);
    syncToParentStatusDependentActionUpdate(anActionEvent);
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {

    var project = getProject(anActionEvent);
    var gitRepository = getSelectedGitRepository(anActionEvent);
    var gitMacheteBranch = getNameOfBranchUnderAction(anActionEvent).flatMap(b -> getGitMacheteBranchByName(anActionEvent, b));

    if (gitMacheteBranch.isDefined() && gitRepository.isDefined()) {
      assert gitMacheteBranch.get().isNonRoot() : "Provided machete branch to fast forward is a root";
      doFastForward(project, gitRepository.get(), gitMacheteBranch.get().asNonRoot());
    }
  }

  private void doFastForward(Project project,
      GitRepository gitRepository,
      INonRootManagedBranchSnapshot gitMacheteNonRootBranch) {
    var localFullName = gitMacheteNonRootBranch.getFullName();
    var parentLocalFullName = gitMacheteNonRootBranch.getParent().getFullName();
    // There is no leading '+' in the refspec since we only ever want a fast-forward update.
    var refspecFromChildToParent = "${localFullName}:${parentLocalFullName}";

    new FetchBackgroundable(project, gitRepository, LOCAL_REPOSITORY_NAME, refspecFromChildToParent,
        getString("action.GitMachete.BaseFastForwardParentToMatchBranchAction.task-title")).queue();
  }
}
