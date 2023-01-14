package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import git4idea.fetch.GitFetchResult;
import git4idea.fetch.GitFetchSupport;
import io.vavr.control.Option;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import com.virtuslab.gitmachete.frontend.actions.backgroundables.SideEffectingBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.base.BaseProjectDependentAction;
import com.virtuslab.gitmachete.frontend.actions.common.FetchUpToDateTimeoutStatus;
import com.virtuslab.qual.async.ContinuesInBackground;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
public class FetchAllRemotesAction extends BaseProjectDependentAction {

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  @Override
  protected boolean isSideEffecting() {
    return true;
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);

    val project = getProject(anActionEvent);
    val presentation = anActionEvent.getPresentation();
    if (GitFetchSupport.fetchSupport(project).isFetchRunning()) {
      presentation.setEnabled(false);
      presentation
          .setDescription(getNonHtmlString("action.GitMachete.FetchAllRemotesAction.description.disabled.already-running"));
    } else {
      val gitRepository = getSelectedGitRepository(anActionEvent);
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
  @ContinuesInBackground
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    LOG.debug("Performing");

    val project = getProject(anActionEvent);
    val gitRepository = getSelectedGitRepository(anActionEvent);
    val title = getString("action.GitMachete.FetchAllRemotesAction.task-title");
    new SideEffectingBackgroundable(project, title, "fetch") {
      private @MonotonicNonNull GitFetchResult result = null;

      @Override
      @UIThreadUnsafe
      public void doRun(ProgressIndicator indicator) {
        result = GitFetchSupport.fetchSupport(project).fetchAllRemotes(Option.of(gitRepository).toJavaList());
        if (gitRepository != null) {
          val repoName = gitRepository.getRoot().getName();
          FetchUpToDateTimeoutStatus.update(repoName);
        }
      }

      @Override
      @UIEffect
      public void onFinished() {
        val result = this.result;
        if (result != null) {
          result.showNotification();
        }
      }
    }.queue();
  }
}
