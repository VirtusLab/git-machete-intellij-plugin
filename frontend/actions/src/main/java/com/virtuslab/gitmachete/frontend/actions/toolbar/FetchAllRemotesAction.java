package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import git4idea.fetch.GitFetchResult;
import git4idea.fetch.GitFetchSupport;
import io.vavr.control.Option;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import com.virtuslab.gitmachete.frontend.actions.base.BaseProjectDependentAction;
import com.virtuslab.gitmachete.frontend.actions.common.FetchUpToDateTimeoutStatus;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
public class FetchAllRemotesAction extends BaseProjectDependentAction {

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);

    final var project = getProject(anActionEvent);
    final var presentation = anActionEvent.getPresentation();
    if (GitFetchSupport.fetchSupport(project).isFetchRunning()) {
      presentation.setEnabled(false);
      presentation
          .setDescription(getNonHtmlString("action.GitMachete.FetchAllRemotesAction.description.disabled.already-running"));
    } else {
      final var gitRepository = getSelectedGitRepository(anActionEvent);
      if (gitRepository == null) {
        presentation.setEnabled(false);
        presentation
            .setDescription(getNonHtmlString("action.GitMachete.FetchAllRemotesAction.description.disabled.no-git-repository"));
      } else if (gitRepository.getRemotes().isEmpty()) {
        presentation.setEnabled(false);
        presentation
            .setDescription(getNonHtmlString("action.GitMachete.FetchAllRemotesAction.description.disabled.no-remotes"));
      } else {
        presentation.setEnabled(true);
        presentation.setDescription(getNonHtmlString("action.GitMachete.FetchAllRemotesAction.description"));
      }
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    log().debug("Performing");

    Project project = getProject(anActionEvent);
    final var gitRepository = getSelectedGitRepository(anActionEvent);

    final var title = getString("action.GitMachete.FetchAllRemotesAction.task-title");
    new Task.Backgroundable(project, title, /* canBeCancelled */ true) {
      private @MonotonicNonNull GitFetchResult result = null;

      @Override
      @UIThreadUnsafe
      public void run(ProgressIndicator indicator) {
        result = GitFetchSupport.fetchSupport(project).fetchAllRemotes(Option.of(gitRepository).toJavaList());
        if (gitRepository != null) {
          final var repoName = gitRepository.getRoot().getName();
          FetchUpToDateTimeoutStatus.update(repoName);
        }
      }

      @Override
      @UIEffect
      public void onFinished() {
        final var result = this.result;
        if (result != null) {
          result.showNotification();
        }
      }
    }.queue();
  }
}
