package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import git4idea.fetch.GitFetchResult;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.actions.common.GitFetchSupportImpl;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public class FetchAllRemotesAction extends DumbAwareAction implements IExpectsKeyProject {

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    var project = getProject(anActionEvent);
    var presentation = anActionEvent.getPresentation();
    if (GitFetchSupportImpl.fetchSupport(project).isFetchRunning()) {
      presentation.setEnabled(false);
      presentation.setDescription("Fetch is already running...");
    } else {
      presentation.setDescription("Fetch all remotes");
    }
  }

  @Override
  public void actionPerformed(AnActionEvent anActionEvent) {
    log().debug("Performing");

    var project = getProject(anActionEvent);
    var gitRepository = getSelectedGitRepository(anActionEvent);

    new Task.Backgroundable(project, "Fetching...", /* canBeCancelled */ true) {

      @Nullable
      GitFetchResult result = null;

      @Override
      public void run(ProgressIndicator indicator) {
        result = GitFetchSupportImpl.fetchSupport(project).fetchAllRemotes(gitRepository.toJavaList());
      }

      @Override
      public void onFinished() {
        var result = this.result;
        if (result != null) {
          result.showNotification();
        }
      }
    }.queue();
  }

}
