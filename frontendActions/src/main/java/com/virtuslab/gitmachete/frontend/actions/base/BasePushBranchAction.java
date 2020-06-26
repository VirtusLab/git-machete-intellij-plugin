package com.virtuslab.gitmachete.frontend.actions.base;

import java.util.Collections;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import git4idea.GitLocalBranch;
import git4idea.push.GitPushSource;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.gitmachete.frontend.actions.pushdialog.GitPushDialog;
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
  public String getActionName() {
    return "Push";
  }

  @Override
  public List<SyncToRemoteStatus.Relation> getEligibleRelations() {
    return List.of(
        SyncToRemoteStatus.Relation.AheadOfRemote,
        SyncToRemoteStatus.Relation.DivergedFromAndNewerThanRemote,
        SyncToRemoteStatus.Relation.DivergedFromAndOlderThanRemote,
        SyncToRemoteStatus.Relation.Untracked);
  }

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);
    syncToRemoteStatusDependentActionUpdate(anActionEvent);
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {

    var project = getProject(anActionEvent);
    var gitRepository = getSelectedGitRepository(anActionEvent);
    var branchName = getNameOfBranchUnderAction(anActionEvent);

    if (branchName.isDefined() && gitRepository.isDefined()) {
      doPush(project, gitRepository.get(), branchName.get());
    }
  }

  @UIEffect
  private void doPush(Project project, GitRepository preselectedRepository, String branchName) {
    @Nullable GitLocalBranch localBranch = preselectedRepository.getBranches().findLocalBranch(branchName);

    if (localBranch != null) {
      java.util.List<GitRepository> selectedRepositories = Collections.singletonList(preselectedRepository);
      // Presented dialog shows commits for branches belonging to allRepositories, preselectedRepositories and currentRepo.
      // The second and the third one have higher priority of loading its commits.
      // From our perspective, we always have single (pre-selected) repository so we do not care about the priority.
      new GitPushDialog(project,
          /* allRepositories */ selectedRepositories,
          /* preselectedRepositories */ selectedRepositories,
          /* currentRepo */ null,
          GitPushSource.create(localBranch)).show();
    } else {
      log().warn("Skipping the action because provided branch ${branchName} was not found in repository");
    }
  }
}
