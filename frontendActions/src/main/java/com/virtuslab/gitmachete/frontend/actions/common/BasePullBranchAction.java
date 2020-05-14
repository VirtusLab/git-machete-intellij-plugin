package com.virtuslab.gitmachete.frontend.actions.common;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;

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
public abstract class BasePullBranchAction extends GitMacheteRepositoryReadyAction {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");

  protected final List<SyncToRemoteStatus.Relation> PULL_ELIGIBLE_STATUSES = List.of(
      SyncToRemoteStatus.Relation.BehindRemote);

  protected void doPull(Project project, GitRepository gitRepository, String branchName) {
    var trackingInfo = gitRepository.getBranchTrackInfo(branchName);

    if (trackingInfo == null) {
      LOG.warn("No branch tracking info for branch ${branchName}");
      return;
    }

    new Task.Backgroundable(project, "Pulling...", true) {

      @Override
      public void run(ProgressIndicator indicator) {
        var fetchSupport = GitFetchSupportImpl.fetchSupport(project);
        var fetchResult = fetchSupport.fetch(gitRepository, trackingInfo.getRemote(), "${branchName}:${branchName}");
        try {
          fetchResult.ourThrowExceptionIfFailed();
        } catch (VcsException e) {
          fetchResult.showNotificationIfFailed("Update of branch '${branchName}' failed");
        }
      }

      @Override
      public void onSuccess() {
        VcsNotifier.getInstance(project).notifySuccess("Branch '${branchName}' updated");
      }
    }.queue();
  }
}
