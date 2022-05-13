package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.nio.file.Path;
import java.util.function.Consumer;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.val;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryCache;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.frontend.compat.UiThreadExecutionCompat;
import com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection.IGitRepositorySelectionProvider;
import com.virtuslab.gitmachete.frontend.ui.providerservice.BranchLayoutWriterProvider;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;

@AllArgsConstructor
@CustomLog
public class GitMacheteRepositoryDiscoverer {

  private final Project project;
  private final IGitRepositorySelectionProvider gitRepositorySelectionProvider;
  private final Consumer<Path> onFailurePathConsumer;
  private final Consumer<IGitMacheteRepositorySnapshot> onSuccessRepositoryConsumer;

  public void enqueue(Path macheteFilePath) {
    val selectedRepository = gitRepositorySelectionProvider.getSelectedGitRepository().getOrNull();
    if (selectedRepository == null) {
      LOG.error("Can't do automatic discover because of undefined selected repository");
      return;
    }
    Path rootDirPath = GitVfsUtils.getRootDirectoryPath(selectedRepository).toAbsolutePath();
    Path mainGitDirPath = GitVfsUtils.getMainGitDirectoryPath(selectedRepository).toAbsolutePath();
    Path worktreeGitDirPath = GitVfsUtils.getWorktreeGitDirectoryPath(selectedRepository).toAbsolutePath();

    new Task.Backgroundable(project, getString("string.GitMachete.EnhancedGraphTable.automatic-discover.task-title")) {
      @Override
      public void run(ProgressIndicator indicator) {
        val discoverRunResult = Try.of(() -> RuntimeBinding.instantiateSoleImplementingClass(IGitMacheteRepositoryCache.class)
            .getInstance(rootDirPath, mainGitDirPath, worktreeGitDirPath).discoverLayoutAndCreateSnapshot());

        if (discoverRunResult.isFailure()) {
          val exception = discoverRunResult.getCause();
          UiThreadExecutionCompat.invokeLaterIfNeeded(NON_MODAL, () -> VcsNotifier.getInstance(project)
              .notifyError(
                  /* displayId */ null,
                  getString(
                      "string.GitMachete.EnhancedGraphTable.automatic-discover.notification.title.cannot-discover-layout-error"),
                  exception.getMessage() != null ? exception.getMessage() : ""));
          return;
        }

        val repositorySnapshot = discoverRunResult.get();

        if (repositorySnapshot.getRootBranches().size() == 0) {
          onFailurePathConsumer.accept(macheteFilePath);
          return;
        }

        val branchLayoutWriter = project.getService(BranchLayoutWriterProvider.class).getBranchLayoutWriter();
        val branchLayout = repositorySnapshot.getBranchLayout();

        try {
          branchLayoutWriter.write(macheteFilePath, branchLayout, /* backupOldLayout */ true);
          onSuccessRepositoryConsumer.accept(repositorySnapshot);
        } catch (BranchLayoutException exception) {
          UiThreadExecutionCompat.invokeLaterIfNeeded(NON_MODAL, () -> VcsNotifier.getInstance(project)
              .notifyError(
                  /* displayId */ null,
                  getString(
                      "string.GitMachete.EnhancedGraphTable.automatic-discover.notification.title.cannot-discover-layout-error"),
                  exception.getMessage() != null ? exception.getMessage() : ""));
        }
      }
    }.queue();
  }
}
