package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.common.SyncToRemoteStatusDescriptionProvider.syncToRemoteStatusRelationToReadableBranchDescription;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.common.FetchBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGraphTable;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedVcsRepository;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

public abstract class BasePullBranchAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyGitMacheteRepository,
      IExpectsKeyGraphTable,
      IExpectsKeyProject,
      IExpectsKeySelectedVcsRepository {

  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");

  private final List<SyncToRemoteStatus.Relation> PULL_ELIGIBLE_STATUSES = List.of(SyncToRemoteStatus.Relation.BehindRemote);

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    Presentation presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    var branchName = getNameOfBranchUnderAction(anActionEvent);

    if (branchName.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription("Pull disabled due to undefined branch name");
      return;
    }

    Option<SyncToRemoteStatus> syncToRemoteStatus = getGitMacheteRepository(anActionEvent)
        .flatMap(repo -> repo.getBranchByName(branchName.get()))
        .map(branch -> branch.getSyncToRemoteStatus());

    if (syncToRemoteStatus.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription("Pull disabled due to undefined sync to remote status");
      return;
    }

    SyncToRemoteStatus.Relation relation = syncToRemoteStatus.get().getRelation();
    boolean isEnabled = PULL_ELIGIBLE_STATUSES.contains(relation);

    if (isEnabled) {

      if (getCurrentBranchNameIfManaged(anActionEvent).equals(branchName)) {
        presentation.setText("Pull Current Branch");
      }

      presentation.setDescription("Pull (fast-forward only) branch '${branchName.get()}'");

    } else {
      presentation.setEnabled(false);
      String descriptionSpec = syncToRemoteStatusRelationToReadableBranchDescription(relation);
      presentation.setDescription("Pull disabled because ${descriptionSpec}");
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    LOG.debug("Performing");

    var project = getProject(anActionEvent);
    var gitRepository = getSelectedVcsRepository(anActionEvent);
    var branchName = getNameOfBranchUnderAction(anActionEvent);

    if (!branchName.isDefined()) {
      LOG.warn("Skipping the action because name of branch to pull is undefined");
    } else if (!gitRepository.isDefined()) {
      LOG.warn("Skipping the action because no VCS repository is selected");
    } else {
      doPull(project, gitRepository.get(), branchName.get());
      getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
    }
  }

  private void doPull(Project project, GitRepository gitRepository, String branchName) {
    var trackingInfo = gitRepository.getBranchTrackInfo(branchName);

    if (trackingInfo == null) {
      LOG.warn("No branch tracking info for branch ${branchName}");
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
