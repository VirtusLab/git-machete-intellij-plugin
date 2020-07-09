package com.virtuslab.gitmachete.frontend.actions.common;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static java.text.MessageFormat.format;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.fetch.GitFetchSupport;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;

public class FetchBackgroundable extends Task.Backgroundable {

  private final Project project;
  private final GitRepository gitRepository;
  private final GitRemote remote;
  private final String refspec;

  public FetchBackgroundable(Project project,
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
    var fetchSupport = GitFetchSupport.fetchSupport(project);
    var fetchResult = fetchSupport.fetch(gitRepository, remote, refspec);
    fetchResult.showNotificationIfFailed(format(getString("action.GitMachete.FetchBackgroundable.notification.fail"), refspec));
  }

  @Override
  public void onSuccess() {
    VcsNotifier.getInstance(project)
        .notifySuccess(format(getString("action.GitMachete.FetchBackgroundable.notification.success"), refspec));
  }
}
