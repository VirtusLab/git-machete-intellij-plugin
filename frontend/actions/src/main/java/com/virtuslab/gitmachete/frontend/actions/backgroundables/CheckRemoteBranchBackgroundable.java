package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.GitBranch;
import git4idea.repo.GitRepository;
import lombok.SneakyThrows;
import lombok.experimental.ExtensionMethod;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod(GitMacheteBundle.class)
public class CheckRemoteBranchBackgroundable extends Task.Backgroundable {

  private final Project project;
  private final GitRepository gitRepository;
  private final String remoteBranchName;
  private final String taskFailNotificationTitle;
  private final String taskFailNotificationPrefix;

  public CheckRemoteBranchBackgroundable(Project project, GitRepository gitRepository, String remoteBranchName,
      String taskFailNotificationTitle, String taskFailNotificationPrefix) {
    super(project, getString("action.GitMachete.CheckRemoteBranchBackgroundable.task-title"));
    this.project = project;
    this.gitRepository = gitRepository;
    this.remoteBranchName = remoteBranchName;
    this.taskFailNotificationTitle = taskFailNotificationTitle;
    this.taskFailNotificationPrefix = taskFailNotificationPrefix;
  }

  @Override
  @SneakyThrows
  @UIThreadUnsafe
  public void run(ProgressIndicator indicator) {
    GitBranch targetBranch = gitRepository.getBranches().findBranchByName(remoteBranchName);
    if (targetBranch == null) {
      throw new GitMacheteException(
          getString("action.GitMachete.CheckRemoteBranchBackgroundable.notification.fail.text").format(remoteBranchName));
    }
  }

  @Override
  @UIEffect
  public void onThrowable(Throwable error) {
    String errorMessage = error.getMessage();
    if (errorMessage != null) {
      VcsNotifier.getInstance(project).notifyError(
          /* displayId */ null, taskFailNotificationTitle, taskFailNotificationPrefix + errorMessage);
    }
  }
}
