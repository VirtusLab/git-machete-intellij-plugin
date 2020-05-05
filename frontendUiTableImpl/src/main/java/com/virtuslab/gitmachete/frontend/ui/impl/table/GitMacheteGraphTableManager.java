package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;

import com.intellij.openapi.project.Project;
import com.intellij.ui.GuiUtils;
import com.intellij.util.messages.Topic;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import io.vavr.control.Option;

import com.virtuslab.gitmachete.frontend.ui.api.root.IGitRepositorySelectionProvider;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseGraphTable;
import com.virtuslab.gitmachete.frontend.ui.api.table.IGraphTableManager;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

public final class GitMacheteGraphTableManager implements IGraphTableManager {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendUiTable");

  private final Project project;
  private final IGitRepositorySelectionProvider gitRepositorySelectionProvider;
  private final BaseGraphTable graphTable;

  public GitMacheteGraphTableManager(BaseGraphTable graphTable, Project project,
      IGitRepositorySelectionProvider gitRepositorySelectionProvider) {
    this.graphTable = graphTable;
    this.project = project;
    this.gitRepositorySelectionProvider = gitRepositorySelectionProvider;

    // InitializationChecker allows us to invoke instance methods below because the class is final
    // and all fields are already initialized. Hence, `this` is already `@Initialized` (and not just
    // `@UnderInitialization(GitMacheteGraphTableManager.class)`, as would be with a non-final class) at this point.

    subscribeToVcsRootChanges();
    subscribeToGitRepositoryChanges();
  }

  private void subscribeToVcsRootChanges() {
    // The method reference is invoked when user changes repository in combo box menu
    gitRepositorySelectionProvider.addSelectionChangeObserver(() -> queueRepositoryUpdateAndGraphTableRefresh());
  }

  private void subscribeToGitRepositoryChanges() {
    Topic<GitRepositoryChangeListener> topic = GitRepository.GIT_REPO_CHANGE;
    GitRepositoryChangeListener listener = repository -> queueRepositoryUpdateAndGraphTableRefresh();
    project.getMessageBus().connect().subscribe(topic, listener);
  }

  /**
   * Repository update is queued as a background task, which in turn itself queues graph table refresh onto the UI thread.
   */
  @Override
  public void queueRepositoryUpdateAndGraphTableRefresh() {
    LOG.debug("Entering");

    if (project != null && !project.isDisposed()) {
      GuiUtils.invokeLaterIfNeeded(() -> {
        Option<GitRepository> gitRepository = gitRepositorySelectionProvider.getSelectedRepository();
        if (gitRepository.isDefined()) {
          LOG.debug("Queuing repository update onto a non-UI thread");

          GitMacheteRepositoryUpdateTask.of(project, gitRepository.get(), graphTable).queue();
        } else {
          LOG.warn("Selected repository is null");
        }
      }, NON_MODAL);
    } else {
      LOG.debug("project == null or is disposed");
    }
  }

}
