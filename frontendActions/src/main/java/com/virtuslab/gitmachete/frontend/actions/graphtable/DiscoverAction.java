package com.virtuslab.gitmachete.frontend.actions.graphtable;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.nio.file.Path;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.ui.GuiUtils;
import io.vavr.control.Try;
import lombok.CustomLog;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryCache;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.frontend.actions.dialogs.GraphTableDialog;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.gitmachete.frontend.ui.providerservice.SelectedGitRepositoryProvider;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public class DiscoverAction extends DumbAwareAction implements IExpectsKeyProject {
  @Override
  public void actionPerformed(AnActionEvent anActionEvent) {
    var project = getProject(anActionEvent);
    var selectedRepoProvider = project.getService(SelectedGitRepositoryProvider.class).getGitRepositorySelectionProvider();
    var selectedRepository = selectedRepoProvider.getSelectedGitRepository().getOrNull();
    if (selectedRepository == null) {
      VcsNotifier.getInstance(project).notifyError(
          /* title */ getString("action.GitMachete.DiscoverAction.cant-get-current-repository-error-title"), /* message */ "");
      return;
    }
    var mainDirPath = GitVfsUtils.getMainDirectoryPath(selectedRepository).toAbsolutePath();
    var gitDirPath = GitVfsUtils.getGitDirectoryPath(selectedRepository).toAbsolutePath();
    // Note that we're essentially doing a heavy-ish operation of discoverLayoutAndCreateSnapshot on UI thread here.
    // This is still acceptable since it simplifies the flow (no background task needed)
    // and this action is not going to be invoked frequently (probably just once for a given project).
    Try.of(() -> RuntimeBinding.instantiateSoleImplementingClass(IGitMacheteRepositoryCache.class)
        .getInstance(mainDirPath, gitDirPath).discoverLayoutAndCreateSnapshot())
        .onFailure(e -> GuiUtils.invokeLaterIfNeeded(() -> VcsNotifier.getInstance(project)
            .notifyError(/* title */ getString("action.GitMachete.DiscoverAction.repository-discover-error-title"),
                /* message */ e.getMessage() != null ? e.getMessage() : ""),
            NON_MODAL))
        .onSuccess(repoSnapshot -> GuiUtils.invokeLaterIfNeeded(() -> GraphTableDialog.of(repoSnapshot,
            /* windowTitle */ getString("action.GitMachete.DiscoverAction.discovered-branch-tree-dialog.title"),
            /* emptyTableText */ getString("action.GitMachete.DiscoverAction.discovered-branch-tree-dialog.empty-table-text"),
            /* okAction */ repositorySnapshot -> saveDiscoveredLayout(repositorySnapshot,
                GitVfsUtils.getMacheteFilePath(selectedRepository), project, getGraphTable(anActionEvent),
                getBranchLayoutWriter(anActionEvent)),
            /* okButtonText */ getString("action.GitMachete.DiscoverAction.discovered-branch-tree-dialog.save-button-text"),
            /* cancelButtonVisible */ true).show(), NON_MODAL));
  }

  private void saveDiscoveredLayout(IGitMacheteRepositorySnapshot repositorySnapshot, Path macheteFilePath, Project project,
      BaseEnhancedGraphTable baseEnhancedGraphTable, IBranchLayoutWriter branchLayoutWriter) {
    var branchLayout = repositorySnapshot.getBranchLayout().getOrNull();
    if (branchLayout == null) {
      VcsNotifier.getInstance(project).notifyError(
          /* title */ getString("action.GitMachete.DiscoverAction.cant-discover-layout-error-title"), /* message */ "");
      return;
    }
    new Task.Backgroundable(project, getString("action.GitMachete.DiscoverAction.write-file-task-title")) {
      @Override
      public void run(ProgressIndicator indicator) {
        Try.run(() -> branchLayoutWriter.write(macheteFilePath, branchLayout, /* backupBranchLayout */ true))
            .onFailure(e -> VcsNotifier.getInstance(project).notifyError(
                /* title */ getString("action.GitMachete.DiscoverAction.write-file-error-title"),
                /* message */ e.getMessage() != null ? e.getMessage() : ""))
            .onSuccess(__ -> baseEnhancedGraphTable.queueRepositoryUpdateAndModelRefresh());
      }
    }.queue();
  }

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }
}
