package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.nio.file.Path;
import java.util.function.Consumer;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.ui.GuiUtils;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryCache;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
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

  @UIEffect
  public void doDiscover(Path macheteFilePath) {
    var selectedRepository = gitRepositorySelectionProvider.getSelectedGitRepository().getOrNull();
    if (selectedRepository == null) {
      LOG.error("Can't do automatic discover because of undefined selected repository");
      return;
    }
    Path mainDirPath = GitVfsUtils.getMainDirectoryPath(selectedRepository).toAbsolutePath();
    Path gitDirPath = GitVfsUtils.getGitDirectoryPath(selectedRepository).toAbsolutePath();

    new Task.Backgroundable(project, getString("string.GitMachete.EnhancedGraphTable.automatic-discover.task-title")) {
      @Override
      public void run(ProgressIndicator indicator) {
        var discoverRunResult = Try.of(() -> RuntimeBinding.instantiateSoleImplementingClass(IGitMacheteRepositoryCache.class)
            .getInstance(mainDirPath, gitDirPath).discoverLayoutAndCreateSnapshot());

        if (!discoverRunResult.isSuccess()) {
          var exception = discoverRunResult.getCause();
          GuiUtils.invokeLaterIfNeeded(() -> VcsNotifier.getInstance(project).notifyError(
              getString("string.GitMachete.EnhancedGraphTable.automatic-discover.cant-discover-layout-error-title"),
              exception.getMessage() != null ? exception.getMessage() : ""), NON_MODAL);
          return;
        }

        var repositorySnapshot = discoverRunResult.get();

        if (repositorySnapshot.getRootBranches().size() == 0) {
          onFailurePathConsumer.accept(macheteFilePath);
          return;
        }

        var branchLayoutWriter = project.getService(BranchLayoutWriterProvider.class).getBranchLayoutWriter();
        var branchLayout = repositorySnapshot.getBranchLayout().getOrNull();

        if (branchLayout == null) {
          LOG.error("Can't get branch layout from repository snapshot");
          return;
        }

        try {
          branchLayoutWriter.write(macheteFilePath, branchLayout, /* backupOldLayout */ true);
          onSuccessRepositoryConsumer.accept(repositorySnapshot);
        } catch (BranchLayoutException exception) {
          GuiUtils.invokeLaterIfNeeded(() -> VcsNotifier.getInstance(project).notifyError(
              getString("string.GitMachete.EnhancedGraphTable.automatic-discover.cant-discover-layout-error-title"),
              exception.getMessage() != null ? exception.getMessage() : ""), NON_MODAL);
        }
      }
    }.queue();
  }
}
