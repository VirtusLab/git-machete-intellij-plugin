package com.virtuslab.gitmachete.frontend.actions.common;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.fetch.GitFetchResult;
import git4idea.fetch.GitFetchSupport;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
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
public abstract class BasePullBranchAction extends GitMacheteRepositoryReadyAction {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");

  protected final List<SyncToRemoteStatus.Relation> PULL_ENABLING_STATUSES = List.of(
      SyncToRemoteStatus.Relation.Behind);

  /**
   * Bear in mind that {@link AnAction#beforeActionPerformedUpdate} is called before each action.
   * (For more details check {@link com.intellij.openapi.actionSystem.ex.ActionUtil} as well.)
   * The {@link AnActionEvent} argument passed to before-called {@link AnAction#update} is the same one that is passed here.
   * This gives us certainty that all checks from actions' update implementations will be performed
   * and all data available via data keys in those {@code update} implementations will still do be available
   * in {@link BasePullBranchAction#actionPerformed} implementations.
   */
  @Override
  @UIEffect
  public abstract void actionPerformed(AnActionEvent anActionEvent);

  protected void doPull(Project project, GitRepository gitRepository, String branchName) {
    var trackingInfo = gitRepository.getBranchTrackInfo(branchName);

    if (trackingInfo == null) {
      LOG.warn("No branch tracking info for branch ${branchName}");
      return;
    }

    new Task.Backgroundable(project, "Updating...", true) {

      @Nullable
      private GitFetchResult fetchResult = null;

      @Override
      public void run(ProgressIndicator indicator) {
        var fetchSupport = GitFetchSupport.fetchSupport(project);
        fetchResult = fetchSupport.fetch(gitRepository, trackingInfo.getRemote(), "${branchName}:${branchName}");
        fetchResult.throwExceptionIfFailed();
      }

      @Override
      public void onThrowable(Throwable error) {
        if (fetchResult != null) {
          fetchResult.showNotificationIfFailed("Update of branch '${branchName}' failed");
        } else {
          LOG.warn("Update failed due to an error but fetchResult is null");
        }
      }

      @Override
      public void onSuccess() {
        VcsNotifier.getInstance(project).notifySuccess("Branch '${branchName}' updated");
      }
    }.queue();
  }

  protected String getRelationBaseDescription(SyncToRemoteStatus.Relation relation) {
    String descriptionSpec = Match(relation).of(
        Case($(SyncToRemoteStatus.Relation.Ahead), "ahead its remote"),
        Case($(SyncToRemoteStatus.Relation.DivergedAndNewerThanRemote), "diverged (& newer) from its remote"),
        Case($(SyncToRemoteStatus.Relation.DivergedAndOlderThanRemote), "diverged (& older) from its remote"),
        Case($(SyncToRemoteStatus.Relation.InSync), "in sync to its remote"),
        Case($(SyncToRemoteStatus.Relation.Untracked), "untracked"),
        Case($(), "in unknown status '${relation.toString()}' to its remote"));
    return "Pull disabled because current branch is ${descriptionSpec}";
  }
}
