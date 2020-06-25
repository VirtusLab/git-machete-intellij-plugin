package com.virtuslab.gitmachete.frontend.actions.base;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.common.FetchBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public abstract class BasePullBranchAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyGitMacheteRepository,
      IExpectsKeyProject,
      ISyncToRemoteStatusDependentAction {

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }

  @Override
  public String getActionName() {
    return "Pull";
  }

  @Override
  public String getDescriptionActionName() {
    return "Pull (fast-forward only)";
  }

  @Override
  public List<SyncToRemoteStatus.Relation> getEligibleStatuses() {
    return List.of(
        SyncToRemoteStatus.Relation.BehindRemote,
        SyncToRemoteStatus.Relation.InSyncToRemote);
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
    log().debug("Performing");

    var project = getProject(anActionEvent);
    var gitRepository = getSelectedGitRepository(anActionEvent);
    var branchName = getNameOfBranchUnderAction(anActionEvent);

    if (branchName.isDefined() && gitRepository.isDefined()) {
      doPull(project, gitRepository.get(), branchName.get());
      getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
    }
  }

  private void doPull(Project project, GitRepository gitRepository, String branchName) {
    var trackingInfo = gitRepository.getBranchTrackInfo(branchName);

    if (trackingInfo == null) {
      log().warn("No branch tracking info for branch ${branchName}");
      return;
    }

    var localFullName = trackingInfo.getLocalBranch().getFullName();
    var remoteFullName = trackingInfo.getRemoteBranch().getFullName();

    // Note the '+' sign preceding the refspec. It permits non-fast-forward updates.
    // This strategy is used to fetch branch from remote repository to local remotes.
    var refspecLocalRemote = "+${localFullName}:${remoteFullName}";

    // On the other hand this refspec has no '+' sign.
    // This is because the fetch from local remotes to local heads must behave fast-forward-like.
    var refspecRemoteLocal = "${remoteFullName}:${localFullName}";

    new FetchBackgroundable(project, gitRepository, refspecLocalRemote, trackingInfo.getRemote(), /* taskTitle */ "Pulling...")
        .queue();

    // Remote set to '.' (dot) is just the local repository.
    new FetchBackgroundable(project, gitRepository, refspecRemoteLocal, GitRemote.DOT, /* taskTitle */ "Pulling...").queue();
  }
}
