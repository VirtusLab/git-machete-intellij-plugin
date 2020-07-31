package com.virtuslab.gitmachete.frontend.actions.common;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static java.text.MessageFormat.format;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.GitUtil;
import git4idea.fetch.GitFetchSupport;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import lombok.CustomLog;

@CustomLog
public class FetchBackgroundable extends Task.Backgroundable {

  private final Project project;
  private final GitRepository gitRepository;
  private final String remoteName;
  private final String refspec;

  public FetchBackgroundable(Project project,
      GitRepository gitRepository,
      String remoteName,
      String refspec,
      String taskTitle) {
    super(project, taskTitle, /* canBeCancelled */ true);
    this.project = project;
    this.gitRepository = gitRepository;
    this.remoteName = remoteName;
    this.refspec = refspec;
  }

  @Override
  public void run(ProgressIndicator indicator) {
    var fetchSupport = GitFetchSupport.fetchSupport(project);
    var remote = remoteName.equals(".") ? GitRemote.DOT : GitUtil.findRemoteByName(gitRepository, remoteName);
    if (remote == null) {
      // This is generally NOT expected, the task should never be triggered
      // for an invalid remote in the first place.
      LOG.warn("Remote '${remoteName}' does not exist");
      return;
    }
    var fetchResult = fetchSupport.fetch(gitRepository, remote, refspec);
    fetchResult.showNotificationIfFailed(format(getString("action.GitMachete.FetchBackgroundable.notification.fail"), refspec));
  }

  @Override
  public void onSuccess() {
    VcsNotifier.getInstance(project)
        .notifySuccess(format(getString("action.GitMachete.FetchBackgroundable.notification.success"), refspec));
  }
}
