package com.virtuslab.gitmachete.frontend.actions.common;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.manager.IBranchLayoutWriter;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseGraphTable;

public final class ActionUtils {

  private ActionUtils() {}

  public static Option<IBranchLayout> getBranchLayout(AnActionEvent anActionEvent) {
    return getGitMacheteRepository(anActionEvent).flatMap(repository -> repository.getBranchLayout());
  }

  public static IBranchLayoutWriter getBranchLayoutWriter(AnActionEvent anActionEvent) {
    return anActionEvent.getData(DataKeys.KEY_BRANCH_LAYOUT_WRITER);
  }

  public static Option<String> getCurrentBranchNameIfManaged(AnActionEvent anActionEvent) {
    return getCurrentMacheteBranchIfManaged(anActionEvent).map(branch -> branch.getName());
  }

  public static Option<BaseGitMacheteBranch> getCurrentMacheteBranchIfManaged(AnActionEvent anActionEvent) {
    return getGitMacheteRepository(anActionEvent).flatMap(repository -> repository.getCurrentBranchIfManaged());
  }

  public static Option<BaseGitMacheteBranch> getGitMacheteBranchByName(AnActionEvent anActionEvent, String branchName) {
    return getGitMacheteRepository(anActionEvent).flatMap(r -> r.getBranchByName(branchName));
  }

  public static Option<IGitMacheteRepository> getGitMacheteRepository(AnActionEvent anActionEvent) {
    return Option.of(anActionEvent.getData(DataKeys.KEY_GIT_MACHETE_REPOSITORY));
  }

  public static BaseGraphTable getGraphTable(AnActionEvent anActionEvent) {
    return anActionEvent.getData(DataKeys.KEY_GRAPH_TABLE);
  }

  public static Project getProject(AnActionEvent anActionEvent) {
    var project = anActionEvent.getProject();
    assert project != null : "Can't get project from action event";
    return project;
  }

  public static Option<String> getSelectedBranchName(AnActionEvent anActionEvent) {
    return Option.of(anActionEvent.getData(DataKeys.KEY_SELECTED_BRANCH_NAME));
  }

  public static Option<GitRepository> getSelectedVcsRepository(AnActionEvent anActionEvent) {
    return Option.of(anActionEvent.getData(DataKeys.KEY_SELECTED_VCS_REPOSITORY));
  }

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
