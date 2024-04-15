package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
import static com.virtuslab.gitmachete.frontend.actions.toolbar.OpenMacheteFileAction.openMacheteFile;
import static com.virtuslab.gitmachete.frontend.common.WriteActionUtils.blockingRunWriteActionOnUIThread;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.util.ModalityUiUtil;
import git4idea.repo.GitRepository;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.SneakyThrows;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryCache;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.SideEffectingBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.base.BaseProjectDependentAction;
import com.virtuslab.gitmachete.frontend.actions.dialogs.GraphTableDialog;
import com.virtuslab.gitmachete.frontend.file.MacheteFileWriter;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection.IGitRepositorySelectionProvider;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.async.ContinuesInBackground;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod({GitMacheteBundle.class, GitVfsUtils.class, Objects.class})
@CustomLog
public class DiscoverAction extends BaseProjectDependentAction {

  @Override
  protected boolean isSideEffecting() {
    return true;
  }

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  public void onUpdate(AnActionEvent actionEvent) {
    super.onUpdate(actionEvent);
    actionEvent.getPresentation().setDescription(getNonHtmlString("action.GitMachete.DiscoverAction.description"));
  }

  @Override
  @ContinuesInBackground
  public void actionPerformed(AnActionEvent anActionEvent) {
    val project = getProject(anActionEvent);
    val gitRepository = project.getService(IGitRepositorySelectionProvider.class).getSelectedGitRepository();
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
    val branchLayoutWriter = ApplicationManager.getApplication().getService(IBranchLayoutWriter.class);

    new SideEffectingBackgroundable(project, getNonHtmlString("action.GitMachete.DiscoverAction.task.title"), "discovery") {
      @Override
      @SneakyThrows
      @UIThreadUnsafe
      protected void doRun(ProgressIndicator indicator) {
        val repoSnapshot = ApplicationManager.getApplication().getService(IGitMacheteRepositoryCache.class)
            .getInstance(rootDirPath, mainGitDirPath, worktreeGitDirPath, ApplicationManager.getApplication()::getService)
            .discoverLayoutAndCreateSnapshot();

        ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL, () -> GraphTableDialog.Companion.of(
            repoSnapshot,
            /* windowTitle */ getString("action.GitMachete.DiscoverAction.discovered-branch-tree-dialog.title"),
            /* emptyTableText */ getString("action.GitMachete.DiscoverAction.discovered-branch-tree-dialog.empty-table-text"),
            /* saveAction */ saveAndDoNotOpenMacheteFileSnapshotConsumer(gitRepository, branchLayoutWriter),
            /* saveAndEditAction */ saveAndOpenMacheteFileSnapshotConsumer(gitRepository, branchLayoutWriter),
            /* okButtonText */ getString("action.GitMachete.DiscoverAction.discovered-branch-tree-dialog.save-button-text"),
            /* cancelButtonVisible */ true,
            /* shouldDisplayActionToolTips */ false).show());
      }

      @Override
      public void onThrowable(Throwable error) {
        VcsNotifier.getInstance(project).notifyError(
            /* displayId */ null,
            /* title */ getString("action.GitMachete.DiscoverAction.notification.title.repository-discover-error"),
            /* message */ error.getMessage().requireNonNullElse(""));
      }
    }.queue();
  }

  private Consumer<IGitMacheteRepositorySnapshot> saveAndDoNotOpenMacheteFileSnapshotConsumer(GitRepository gitRepository,
      IBranchLayoutWriter branchLayoutWriter) {
    return repositorySnapshot -> saveDiscoveredLayout(repositorySnapshot,
        gitRepository.getMacheteFilePath(), gitRepository.getProject(), branchLayoutWriter, () -> {});
  }

  private Consumer<IGitMacheteRepositorySnapshot> saveAndOpenMacheteFileSnapshotConsumer(GitRepository gitRepository,
      IBranchLayoutWriter branchLayoutWriter) {
    return repositorySnapshot -> saveDiscoveredLayout(repositorySnapshot,
        gitRepository.getMacheteFilePath(), gitRepository.getProject(),
        branchLayoutWriter, () -> openMacheteFile(gitRepository));
  }

  private void saveDiscoveredLayout(IGitMacheteRepositorySnapshot repositorySnapshot,
      Path macheteFilePath,
      Project project,
      IBranchLayoutWriter branchLayoutWriter,
      @UI Runnable postWriteRunnable) {
    val branchLayout = repositorySnapshot.getBranchLayout();
    blockingRunWriteActionOnUIThread(() -> MacheteFileWriter.writeBranchLayout(
        macheteFilePath,
        branchLayoutWriter,
        branchLayout,
        /* backupOldLayout */ true,
        /* requestor */ this));
    VfsUtil.markDirtyAndRefresh(/* async */ false, /* recursive */ true, /* reloadChildren */ false,
        ProjectRootManager.getInstance(project).getContentRoots());
    ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL, postWriteRunnable);
  }
}
