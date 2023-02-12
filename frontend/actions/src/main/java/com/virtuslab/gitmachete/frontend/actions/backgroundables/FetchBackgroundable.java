package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.GitUtil;
import git4idea.fetch.GitFetchSupport;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.tainting.qual.Untainted;

import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
public class FetchBackgroundable extends Task.Backgroundable {

  private final GitRepository gitRepository;
  private final String remoteName;
  private final @Nullable String refspec;
  private final @Untainted String failureNotificationText;
  private final String successNotificationText;

  /** Use as {@code remoteName} when referring to the local repository. */
  public static final String LOCAL_REPOSITORY_NAME = ".";

  public FetchBackgroundable(
      GitRepository gitRepository,
      String remoteName,
      @Nullable String refspec,
      String taskTitle,
      @Untainted String failureNotificationText,
      String successNotificationText) {
    super(gitRepository.getProject(), taskTitle);
    this.gitRepository = gitRepository;
    this.remoteName = remoteName;
    this.refspec = refspec;
    this.failureNotificationText = failureNotificationText;
    this.successNotificationText = successNotificationText;
  }

  @Override
  @UIThreadUnsafe
  public void run(ProgressIndicator indicator) {
    val fetchSupport = GitFetchSupport.fetchSupport(gitRepository.getProject());
    GitRemote remote = remoteName.equals(LOCAL_REPOSITORY_NAME)
        ? GitRemote.DOT
        : GitUtil.findRemoteByName(gitRepository, remoteName);
    if (remote == null) {
      // This is generally NOT expected, the task should never be triggered
      // for an invalid remote in the first place.
      LOG.warn("Remote '${remoteName}' does not exist");
      return;
    }

    val fetchResult = refspec != null
        ? fetchSupport.fetch(gitRepository, remote, refspec)
        : fetchSupport.fetch(gitRepository, remote);
    fetchResult.showNotificationIfFailed(failureNotificationText);
    fetchResult.throwExceptionIfFailed();
  }

  @UIEffect
  @Override
  public void onSuccess() {
    VcsNotifier.getInstance(gitRepository.getProject()).notifySuccess(/* displayId */ null, /* title */ "",
        successNotificationText);
  }

  @Override
  public void onThrowable(Throwable error) {
    // ignore - notification already shown from `run` implementation
  }
}
