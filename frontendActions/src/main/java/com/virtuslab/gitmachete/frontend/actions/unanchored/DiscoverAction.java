package com.virtuslab.gitmachete.frontend.actions.unanchored;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.nio.file.Path;
import java.util.function.Consumer;

import com.intellij.ide.actions.OpenFileAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.ui.GuiUtils;
import git4idea.repo.GitRepository;
import io.vavr.control.Try;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryCache;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.frontend.actions.base.BaseProjectDependentAction;
import com.virtuslab.gitmachete.frontend.actions.dialogs.GraphTableDialog;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.gitmachete.frontend.ui.providerservice.SelectedGitRepositoryProvider;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public class DiscoverAction extends BaseProjectDependentAction {

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    var project = getProject(anActionEvent);
    var selectedRepoProvider = project.getService(SelectedGitRepositoryProvider.class).getGitRepositorySelectionProvider();
    var gitRepository = selectedRepoProvider.getSelectedGitRepository().getOrNull();
    if (gitRepository == null) {
      VcsNotifier.getInstance(project).notifyError(
          /* title */ getString("action.GitMachete.DiscoverAction.notification.title.cant-get-current-repository-error"),
          /* message */ "");
      return;
    }
    var mainDirPath = GitVfsUtils.getMainDirectoryPath(gitRepository).toAbsolutePath();
    var gitDirPath = GitVfsUtils.getGitDirectoryPath(gitRepository).toAbsolutePath();

    Consumer<IGitMacheteRepositorySnapshot> saveAction = repositorySnapshot -> saveDiscoveredLayout(repositorySnapshot,
        GitVfsUtils.getMacheteFilePath(gitRepository), project, getGraphTable(anActionEvent),
        getBranchLayoutWriter(anActionEvent));
    Consumer<IGitMacheteRepositorySnapshot> saveAndEditAction = repositorySnapshot -> {
      saveAction.accept(repositorySnapshot);
      openMacheteFile(project, gitRepository);
    };

    // Note that we're essentially doing a heavy-ish operation of discoverLayoutAndCreateSnapshot on UI thread here.
    // This is still acceptable since it simplifies the flow (no background task needed)
    // and this action is not going to be invoked frequently (probably just once for a given project).
    Try.of(() -> RuntimeBinding.instantiateSoleImplementingClass(IGitMacheteRepositoryCache.class)
        .getInstance(mainDirPath, gitDirPath).discoverLayoutAndCreateSnapshot())
        .onFailure(e -> GuiUtils.invokeLaterIfNeeded(() -> VcsNotifier.getInstance(project)
            .notifyError(/* title */ getString("action.GitMachete.DiscoverAction.notification.title.repository-discover-error"),
                /* message */ e.getMessage() != null ? e.getMessage() : ""),
            NON_MODAL))
        .onSuccess(repoSnapshot -> GuiUtils.invokeLaterIfNeeded(() -> GraphTableDialog.Companion.of(repoSnapshot,
            /* windowTitle */ getString("action.GitMachete.DiscoverAction.discovered-branch-tree-dialog.title"),
            /* emptyTableText */ getString("action.GitMachete.DiscoverAction.discovered-branch-tree-dialog.empty-table-text"),
            saveAction,
            saveAndEditAction,
            /* okButtonText */ getString("action.GitMachete.DiscoverAction.discovered-branch-tree-dialog.save-button-text"),
            /* cancelButtonVisible */ true,
            /* hasBranchActionToolTips */ false).show(), NON_MODAL));
  }

  private static void openMacheteFile(Project project, GitRepository gitRepository) {
    GuiUtils.invokeLaterIfNeeded(() -> {
      var file = GitVfsUtils.getMacheteFile(gitRepository);
      if (file.isDefined()) {
        OpenFileAction.openFile(file.get(), project);
      } else {
        VcsNotifier.getInstance(project)
            .notifyError(
                /* title */ getString("action.GitMachete.OpenMacheteFileAction.notification.title.machete-file-not-found"),
                /* message */ format(
                    getString("action.GitMachete.OpenMacheteFileAction.notification.message.machete-file-not-found"),
                    gitRepository.getRoot().getPath()));
      }
    }, NON_MODAL);
  }

  private void saveDiscoveredLayout(IGitMacheteRepositorySnapshot repositorySnapshot, Path macheteFilePath, Project project,
      BaseEnhancedGraphTable baseEnhancedGraphTable, IBranchLayoutWriter branchLayoutWriter) {
    var branchLayout = repositorySnapshot.getBranchLayout().getOrNull();
    if (branchLayout == null) {
      VcsNotifier.getInstance(project).notifyError(
          /* title */ getString("action.GitMachete.DiscoverAction.notification.title.cant-discover-layout-error"),
          /* message */ "");
      return;
    }
    new Task.Backgroundable(project, getString("action.GitMachete.DiscoverAction.write-file-task-title")) {
      @Override
      public void run(ProgressIndicator indicator) {
        Try.run(() -> branchLayoutWriter.write(macheteFilePath, branchLayout, /* backupBranchLayout */ true))
            .onFailure(e -> VcsNotifier.getInstance(project).notifyError(
                /* title */ getString("action.GitMachete.DiscoverAction.notification.title.write-file-error"),
                /* message */ e.getMessage() != null ? e.getMessage() : ""))
            .onSuccess(__ -> baseEnhancedGraphTable.queueRepositoryUpdateAndModelRefresh());
      }
    }.queue();
  }
}
