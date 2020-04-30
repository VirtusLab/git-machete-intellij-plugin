package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import git4idea.fetch.GitFetchResult;
import git4idea.fetch.GitFetchSupport;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.actions.common.ActionUtils;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_SELECTED_VCS_REPOSITORY}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public class FetchAllRemotesAction extends AnAction implements DumbAware {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    Project project = ActionUtils.getProject(anActionEvent);
    if (GitFetchSupport.fetchSupport(project).isFetchRunning()) {
      anActionEvent.getPresentation().setEnabled(false);
      anActionEvent.getPresentation().setDescription("Update is already running...");
    }
  }

  @Override
  public void actionPerformed(AnActionEvent anActionEvent) {
    LOG.debug("Performing");

    Project project = ActionUtils.getProject(anActionEvent);
    Option<GitRepository> selectedVcsRepository = ActionUtils.getSelectedVcsRepository(anActionEvent);

    new Task.Backgroundable(project, "Fetching...", /* canBeCancelled */ true) {

      @Nullable
      GitFetchResult result = null;

      @Override
      public void run(ProgressIndicator indicator) {
        result = GitFetchSupport.fetchSupport(project).fetchAllRemotes(selectedVcsRepository.toJavaList());
      }

      @Override
      public void onFinished() {
        if (result != null) {
          onFetchFinished(result);
        }
      }
    }.queue();
  }

  protected void onFetchFinished(GitFetchResult result) {
    result.showNotification();
  }
}
