package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.GitBranch;
import git4idea.repo.GitRepository;
import lombok.SneakyThrows;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

public class CheckRemoteBranchBackgroundable extends Task.Backgroundable {

  private final Project project;
  private final GitRepository gitRepository;
  private final String remoteBranchName;
  private final String taskFailNotification;

  public CheckRemoteBranchBackgroundable(Project project, GitRepository gitRepository, String remoteBranchName,
      String taskFailNotification) {
    super(project, getString("action.GitMachete.CheckRemoteBranchBackgroundable.task-title"));
    this.project = project;
    this.gitRepository = gitRepository;
    this.remoteBranchName = remoteBranchName;
    this.taskFailNotification = taskFailNotification;
  }

  @Override
  @SneakyThrows
  @UIThreadUnsafe
  public void run(ProgressIndicator indicator) {
    GitBranch targetBranch = gitRepository.getBranches().findBranchByName(remoteBranchName);
    if (targetBranch == null) {
      throw new GitMacheteException("Couldn't find the branch on remote repository with name " + remoteBranchName);
    }
  }

  @Override
  @UIEffect
  public void onThrowable(Throwable error) {
    VcsNotifier.getInstance(project).notifyError(
        /* displayId */ null,
        taskFailNotification,
        getString("action.GitMachete.BasePullBranchAction.notification.fail.text"));
  }
}
