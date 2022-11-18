package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.nio.file.Path;
import java.util.function.Consumer;

import com.intellij.ide.actions.OpenFileAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.util.ModalityUiUtil;
import git4idea.repo.GitRepository;
import io.vavr.control.Try;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.SneakyThrows;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryCache;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.frontend.actions.base.BaseProjectDependentAction;
import com.virtuslab.gitmachete.frontend.actions.dialogs.GraphTableDialog;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.gitmachete.frontend.ui.providerservice.SelectedGitRepositoryProvider;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.guieffect.IgnoreUIThreadUnsafeCalls;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod({GitMacheteBundle.class, GitVfsUtils.class})
@CustomLog
public class DiscoverAction extends BaseProjectDependentAction {

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  @IgnoreUIThreadUnsafeCalls("com.virtuslab.gitmachete.backend.api.IGitMacheteRepository.discoverLayoutAndCreateSnapshot()")
  public void actionPerformed(AnActionEvent anActionEvent) {
    val project = getProject(anActionEvent);
    val selectedRepoProvider = project.getService(SelectedGitRepositoryProvider.class)
        .getGitRepositorySelectionProvider();
    val gitRepository = selectedRepoProvider.getSelectedGitRepository();
    if (gitRepository == null) {
      VcsNotifier.getInstance(project).notifyError(
          /* displayId */ null,
          /* title */ getString("action.GitMachete.DiscoverAction.notification.title.cannot-get-current-repository-error"),
          /* message */ "");
      return;
    }

    val rootDirPath = gitRepository.getRootDirectoryPath().toAbsolutePath();
    val mainGitDirPath = gitRepository.getMainGitDirectoryPath().toAbsolutePath();
    val worktreeGitDirPath = gitRepository.getWorktreeGitDirectoryPath().toAbsolutePath();

    val graphTable = getGraphTable(anActionEvent);
    val branchLayoutWriter = getBranchLayoutWriter();

    // Note that we're essentially doing a heavy-ish operation of discoverLayoutAndCreateSnapshot on UI thread here.
    // This is still acceptable since it simplifies the flow (no background task needed)
    // and this action is not going to be invoked frequently (probably just once for a given project).
    Try.of(() -> RuntimeBinding.instantiateSoleImplementingClass(IGitMacheteRepositoryCache.class)
        .getInstance(rootDirPath, mainGitDirPath, worktreeGitDirPath).discoverLayoutAndCreateSnapshot())
        .onFailure(e -> ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL, () -> VcsNotifier.getInstance(project).notifyError(
            /* displayId */ null,
            /* title */ getString("action.GitMachete.DiscoverAction.notification.title.repository-discover-error"),
            /* message */ e.getMessage() != null ? e.getMessage() : "")))
        .onSuccess(repoSnapshot -> ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL, () -> GraphTableDialog.Companion.of(
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
        gitRepository.getMacheteFilePath(), project, graphTable, branchLayoutWriter, () -> {});
  }

  private Consumer<IGitMacheteRepositorySnapshot> saveAndOpenMacheteFileSnapshotConsumer(GitRepository gitRepository,
      Project project, BaseEnhancedGraphTable graphTable, IBranchLayoutWriter branchLayoutWriter) {
    return repositorySnapshot -> saveDiscoveredLayout(repositorySnapshot,
        gitRepository.getMacheteFilePath(), project, graphTable,
        branchLayoutWriter, () -> openMacheteFile(project, gitRepository));
  }

  @UIEffect
  private static void openMacheteFile(Project project, GitRepository gitRepository) {
    val file = gitRepository.getMacheteFile();
    if (file != null) {
      OpenFileAction.openFile(file, project);
    } else {
      VcsNotifier.getInstance(project).notifyError(
          /* displayId */ null,
          /* title */ getString("action.GitMachete.OpenMacheteFileAction.notification.title.machete-file-not-found"),
          /* message */ getString("action.GitMachete.OpenMacheteFileAction.notification.message.machete-file-not-found")
              .format(gitRepository.getRoot().getPath()));
    }
  }

  private void saveDiscoveredLayout(IGitMacheteRepositorySnapshot repositorySnapshot, Path macheteFilePath, Project project,
      BaseEnhancedGraphTable baseEnhancedGraphTable, IBranchLayoutWriter branchLayoutWriter, @UI Runnable postWriteRunnable) {
    val branchLayout = repositorySnapshot.getBranchLayout();
    new Task.Backgroundable(project, getString("action.GitMachete.DiscoverAction.write-file.task-title")) {
      @UIThreadUnsafe
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
        VcsNotifier.getInstance(project).notifyError(
            /* displayId */ null,
            /* title */ getString("action.GitMachete.DiscoverAction.notification.title.write-file-error"),
            /* message */ e.getMessage() != null ? e.getMessage() : "");
      }

    }.queue();
  }
}
