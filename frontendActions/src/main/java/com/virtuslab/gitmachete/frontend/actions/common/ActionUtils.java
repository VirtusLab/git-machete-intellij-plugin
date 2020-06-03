package com.virtuslab.gitmachete.frontend.actions.common;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;

public final class ActionUtils implements IExpectsKeyGitMacheteRepository {

  private ActionUtils() {}

  public static String syncToRemoteStatusRelationToReadableBranchDescription(SyncToRemoteStatus.Relation relation) {
    var desc = Match(relation).of(
        Case($(SyncToRemoteStatus.Relation.AheadOfRemote), "ahead of its remote"),
        Case($(SyncToRemoteStatus.Relation.BehindRemote), "behind its remote"),
        Case($(SyncToRemoteStatus.Relation.DivergedFromAndNewerThanRemote), "diverged from (& newer than) its remote"),
        Case($(SyncToRemoteStatus.Relation.DivergedFromAndOlderThanRemote), "diverged from (& older than) its remote"),
        Case($(SyncToRemoteStatus.Relation.InSyncToRemote), "in sync to its remote"),
        Case($(SyncToRemoteStatus.Relation.Untracked), "untracked"),
        Case($(), "in unknown status '${relation.toString()}' to its remote"));
    return "the branch is ${desc}";
  }

  static class FetchBackgroundable extends Task.Backgroundable {

    private final Project project;
    private final GitRepository gitRepository;
    private final GitRemote remote;
    private final String refspec;

    FetchBackgroundable(Project project,
        GitRepository gitRepository,
        String refspec,
        GitRemote remote,
        final String taskTitle) {
      super(project, taskTitle, /* canBeCancelled */ true);
      this.project = project;
      this.gitRepository = gitRepository;
      this.remote = remote;
      this.refspec = refspec;
    }

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
    }
  }

}
