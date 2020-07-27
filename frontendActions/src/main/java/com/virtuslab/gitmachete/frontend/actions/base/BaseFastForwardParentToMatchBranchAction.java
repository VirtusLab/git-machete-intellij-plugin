package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory.GENERAL;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;

import com.virtuslab.gitmachete.backend.api.IGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.common.FetchBackgroundable;
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
  public @I18nFormat({}) String getActionName() {
    return getString("action.GitMachete.BaseFastForwardParentToMatchBranchAction.action-name");
  }

  @Override
  public @I18nFormat({}) String getDescriptionActionName() {
    return getString("action.GitMachete.BaseFastForwardParentToMatchBranchAction.description-action-name");
  }

  @Override
  public @I18nFormat({GENERAL, GENERAL}) String getEnabledDescriptionFormat() {
    return getString("action.GitMachete.BaseFastForwardParentToMatchBranchAction.description");
  }

  @Override
  public String getCurrentBranchText() {
    return getString("action.GitMachete.BaseFastForwardParentToMatchBranchAction.text.current-branch");
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
      assert gitMacheteBranch.get().isNonRootBranch() : "Provided machete branch to fast forward is a root";
      doFastForward(project, gitRepository.get(), gitMacheteBranch.get().asNonRootBranch());
    }
  }

  private void doFastForward(Project project,
      GitRepository gitRepository,
      IGitMacheteNonRootBranch gitMacheteNonRootBranch) {
    var localFullName = gitMacheteNonRootBranch.getFullName();
    var parentLocalFullName = gitMacheteNonRootBranch.getParentBranch().getFullName();
    var refspecChildParent = "${localFullName}:${parentLocalFullName}";

    // Remote set to '.' (dot) is just the local repository.
    new FetchBackgroundable(project, gitRepository, refspecChildParent, GitRemote.DOT,
        getString("action.GitMachete.BaseFastForwardParentToMatchBranchAction.task-title")).queue();
  }
}
