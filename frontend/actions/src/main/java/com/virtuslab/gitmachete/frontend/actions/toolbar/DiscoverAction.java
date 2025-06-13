package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
import static com.virtuslab.gitmachete.frontend.actions.toolbar.OpenMacheteFileAction.openMacheteFile;
import static com.virtuslab.gitmachete.frontend.common.WriteActionUtils.blockingRunWriteActionOnUIThread;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.util.Objects;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ModalityUiUtil;
import git4idea.repo.GitRepository;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.SneakyThrows;
import lombok.experimental.ExtensionMethod;
import lombok.val;
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
            /* saveAction */ repo -> saveMacheteFile(repo, gitRepository, branchLayoutWriter, /* openAfterSave */ false),
            /* saveAndOpenAction */ repo -> saveMacheteFile(repo, gitRepository, branchLayoutWriter, /* openAfterSave */ true),
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

  private void saveMacheteFile(IGitMacheteRepositorySnapshot repositorySnapshot, GitRepository gitRepository,
      IBranchLayoutWriter branchLayoutWriter, boolean openAfterSave) {
    val branchLayout = repositorySnapshot.getBranchLayout();
    blockingRunWriteActionOnUIThread(() -> MacheteFileWriter.writeBranchLayout(
        gitRepository.getMacheteFilePath(),
        branchLayoutWriter,
        branchLayout,
        /* backupOldLayout */ true,
        /* requestor */ this));
    VirtualFile macheteFile = gitRepository.getMacheteFile();
    if (openAfterSave && macheteFile != null) {
      VfsUtil.markDirtyAndRefresh(/* async */ false, /* recursive */ false, /* reloadChildren */ false, macheteFile);
      ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL, () -> openMacheteFile(gitRepository));
    }
  }
}
