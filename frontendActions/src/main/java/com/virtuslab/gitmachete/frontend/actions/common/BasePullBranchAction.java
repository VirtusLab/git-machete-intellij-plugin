package com.virtuslab.gitmachete.frontend.actions.common;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getCurrentBranchNameIfManaged;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getGitMacheteRepository;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getProject;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getSelectedVcsRepository;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.syncToRemoteStatusRelationToReadableBranchDescription;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public abstract class BasePullBranchAction extends GitMacheteRepositoryReadyAction implements IBranchNameProvider {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");

  private final List<SyncToRemoteStatus.Relation> PULL_ELIGIBLE_STATUSES = List.of(
      SyncToRemoteStatus.Relation.BehindRemote);

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

      presentation.setDescription("Pull branch '${branchName.get()}'");

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

    getFetchBackgroundable(project, gitRepository, refspecLocalRemote, trackingInfo.getRemote()).queue();

    // Remote set to '.' (dot) is just the local repository.
    getFetchBackgroundable(project, gitRepository, refspecRemoteLocal, GitRemote.DOT).queue();
  }

  private static Task.Backgroundable getFetchBackgroundable(Project project, GitRepository gitRepository, String refspec,
      GitRemote remote) {
    return new Task.Backgroundable(project, "Pulling...", true) {

      @Override
      public void run(ProgressIndicator indicator) {
        var fetchSupport = GitFetchSupportImpl.fetchSupport(project);
        var fetchResult = fetchSupport.fetch(gitRepository, remote, refspec);
        try {
          fetchResult.ourThrowExceptionIfFailed();
        } catch (VcsException e) {
          fetchResult.showNotificationIfFailed("Fetch of refspec ${refspec} failed");
        }
      }

      @Override
      public void onSuccess() {
        VcsNotifier.getInstance(project).notifySuccess("Fetch of refspec ${refspec} succeeded");
        // For some reason, the call to `BaseGitMacheteGraphTable::queueRepositoryUpdateAndModelRefresh` within `actionPerformed`
        // does not refresh the graph table after the pulls. Therefore, the following solution is to be used.
        project.getMessageBus().syncPublisher(GitRepository.GIT_REPO_CHANGE).repositoryChanged(gitRepository);
      }
    };
  }
}
