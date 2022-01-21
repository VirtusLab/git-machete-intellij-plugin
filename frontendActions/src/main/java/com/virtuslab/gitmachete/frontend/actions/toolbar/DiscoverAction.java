package com.virtuslab.gitmachete.frontend.actions.toolbar;

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
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import git4idea.repo.GitRepository;
import io.vavr.control.Try;
import lombok.CustomLog;
import lombok.SneakyThrows;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryCache;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.frontend.actions.base.BaseProjectDependentAction;
import com.virtuslab.gitmachete.frontend.actions.dialogs.GraphTableDialog;
import com.virtuslab.gitmachete.frontend.compat.IntelliJNotificationCompat;
import com.virtuslab.gitmachete.frontend.compat.UiThreadExecutionCompat;
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
    val project = getProject(anActionEvent);
    val selectedRepoProvider = project.getService(SelectedGitRepositoryProvider.class).getGitRepositorySelectionProvider();
    val gitRepository = selectedRepoProvider.getSelectedGitRepository().getOrNull();
    if (gitRepository == null) {
      IntelliJNotificationCompat.notifyError(project,
          /* title */ getString("action.GitMachete.DiscoverAction.notification.title.cannot-get-current-repository-error"),
          /* message */ "");
      return;
    }

    val mainDirPath = GitVfsUtils.getMainDirectoryPath(gitRepository).toAbsolutePath();
    val gitDirPath = GitVfsUtils.getGitDirectoryPath(gitRepository).toAbsolutePath();

    val graphTable = getGraphTable(anActionEvent);
    val branchLayoutWriter = getBranchLayoutWriter(anActionEvent);

    // Note that we're essentially doing a heavy-ish operation of discoverLayoutAndCreateSnapshot on UI thread here.
    // This is still acceptable since it simplifies the flow (no background task needed)
    // and this action is not going to be invoked frequently (probably just once for a given project).
    Try.of(() -> RuntimeBinding.instantiateSoleImplementingClass(IGitMacheteRepositoryCache.class)
        .getInstance(mainDirPath, gitDirPath).discoverLayoutAndCreateSnapshot())
        .onFailure(e -> UiThreadExecutionCompat.invokeLaterIfNeeded(NON_MODAL,
            () -> IntelliJNotificationCompat.notifyError(project,
                /* title */ getString("action.GitMachete.DiscoverAction.notification.title.repository-discover-error"),
                /* message */ e.getMessage() != null ? e.getMessage() : "")))
        .onSuccess(repoSnapshot -> UiThreadExecutionCompat.invokeLaterIfNeeded(NON_MODAL, () -> GraphTableDialog.Companion.of(
            repoSnapshot,
            /* windowTitle */ getString("action.GitMachete.DiscoverAction.discovered-branch-tree-dialog.title"),
            /* emptyTableText */ getString("action.GitMachete.DiscoverAction.discovered-branch-tree-dialog.empty-table-text"),
            /* saveAction */ saveAndDoNotOpenMacheteFileSnapshotConsumer(gitRepository, project, graphTable,
                branchLayoutWriter),
            /* saveAndEditAction */ saveAndOpenMacheteFileSnapshotConsumer(gitRepository, project, graphTable,
                branchLayoutWriter),
            /* okButtonText */ getString("action.GitMachete.DiscoverAction.discovered-branch-tree-dialog.save-button-text"),
            /* cancelButtonVisible */ true,
            /* shouldDisplayActionToolTips */ false).show()));
  }

  private Consumer<IGitMacheteRepositorySnapshot> saveAndDoNotOpenMacheteFileSnapshotConsumer(GitRepository gitRepository,
      Project project, BaseEnhancedGraphTable graphTable, IBranchLayoutWriter branchLayoutWriter) {
    return repositorySnapshot -> saveDiscoveredLayout(repositorySnapshot,
        GitVfsUtils.getMacheteFilePath(gitRepository), project, graphTable, branchLayoutWriter, () -> {});
  }

  private Consumer<IGitMacheteRepositorySnapshot> saveAndOpenMacheteFileSnapshotConsumer(GitRepository gitRepository,
      Project project, BaseEnhancedGraphTable graphTable, IBranchLayoutWriter branchLayoutWriter) {
    return repositorySnapshot -> saveDiscoveredLayout(repositorySnapshot,
        GitVfsUtils.getMacheteFilePath(gitRepository), project, graphTable,
        branchLayoutWriter, () -> openMacheteFile(project, gitRepository));
  }

  @UIEffect
  private static void openMacheteFile(Project project, GitRepository gitRepository) {
    val file = GitVfsUtils.getMacheteFile(gitRepository);
    if (file.isDefined()) {
      OpenFileAction.openFile(file.get(), project);
    } else {
      IntelliJNotificationCompat.notifyError(project,
          /* title */ getString("action.GitMachete.OpenMacheteFileAction.notification.title.machete-file-not-found"),
          /* message */ format(
              getString("action.GitMachete.OpenMacheteFileAction.notification.message.machete-file-not-found"),
              gitRepository.getRoot().getPath()));
    }
  }

  private void saveDiscoveredLayout(IGitMacheteRepositorySnapshot repositorySnapshot, Path macheteFilePath, Project project,
      BaseEnhancedGraphTable baseEnhancedGraphTable, IBranchLayoutWriter branchLayoutWriter, @UI Runnable postWriteRunnable) {
    val branchLayout = repositorySnapshot.getBranchLayout();
    new Task.Backgroundable(project, getString("action.GitMachete.DiscoverAction.write-file.task-title")) {
      @Override
      @SneakyThrows
      public void run(ProgressIndicator indicator) {
        branchLayoutWriter.write(macheteFilePath, branchLayout, /* backupOldLayout */ true);
      }

      @Override
      @UIEffect
      public void onSuccess() {
        baseEnhancedGraphTable.queueRepositoryUpdateAndModelRefresh();
        VfsUtil.markDirtyAndRefresh(/* async */ false, /* recursive */ true, /* reloadChildren */ false,
            ProjectRootManager.getInstance(project).getContentRoots());
        postWriteRunnable.run();
      }

      @Override
      @UIEffect
      public void onThrowable(Throwable e) {
        IntelliJNotificationCompat.notifyError(project,
            /* title */ getString("action.GitMachete.DiscoverAction.notification.title.write-file-error"),
            /* message */ e.getMessage() != null ? e.getMessage() : "");
      }

    }.queue();
  }
}
