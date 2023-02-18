package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.virtuslab.gitmachete.frontend.common.WriteActionUtils.blockingRunWriteActionOnUIThread;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryCache;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.frontend.file.MacheteFileWriter;
import com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection.IGitRepositorySelectionProvider;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.async.ContinuesInBackground;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod({GitVfsUtils.class, Objects.class})
@AllArgsConstructor
@CustomLog
public class GitMacheteRepositoryDiscoverer {

  private final Project project;
  private final IGitRepositorySelectionProvider gitRepositorySelectionProvider;
  private final Consumer<Path> onFailurePathConsumer;
  private final Consumer<IGitMacheteRepositorySnapshot> onSuccessRepositorySnapshotConsumer;
  private final Consumer<IGitMacheteRepository> onSuccessRepositoryConsumer;

  @ContinuesInBackground
  public void enqueue(Path macheteFilePath) {
    LOG.info("Enqueuing automatic discover");
    val selectedRepository = gitRepositorySelectionProvider.getSelectedGitRepository();
    if (selectedRepository == null) {
      LOG.error("Can't do automatic discover because of undefined selected repository");
      return;
    }
    Path rootDirPath = selectedRepository.getRootDirectoryPath().toAbsolutePath();
    Path mainGitDirPath = selectedRepository.getMainGitDirectoryPath().toAbsolutePath();
    Path worktreeGitDirPath = selectedRepository.getWorktreeGitDirectoryPath().toAbsolutePath();

    new Task.Backgroundable(project, getString("string.GitMachete.EnhancedGraphTable.automatic-discover.task-title")) {
      @UIThreadUnsafe
      @Override
      public void run(ProgressIndicator indicator) {
        LOG.debug("Running automatic discover task");

        IGitMacheteRepository repository;

        try {
          repository = ApplicationManager.getApplication().getService(IGitMacheteRepositoryCache.class)
              .getInstance(rootDirPath, mainGitDirPath, worktreeGitDirPath);
        } catch (GitMacheteException e) {
          LOG.debug("Instantiation failed");
          VcsNotifier.getInstance(project)
              .notifyError(
                  /* displayId */ null,
                  getString(
                      "string.GitMachete.EnhancedGraphTable.automatic-discover.notification.title.cannot-discover-layout-error"),
                  e.getMessage().requireNonNullElse(""));
          return;
        }

        IGitMacheteRepositorySnapshot repositorySnapshot;
        try {
          repositorySnapshot = repository.discoverLayoutAndCreateSnapshot();
        } catch (GitMacheteException e) {
          LOG.debug("Snapshot creation failed");
          VcsNotifier.getInstance(project)
              .notifyError(
                  /* displayId */ null,
                  getString(
                      "string.GitMachete.EnhancedGraphTable.automatic-discover.notification.title.cannot-discover-layout-error"),
                  e.getMessage().requireNonNullElse(""));
          return;
        }

        if (repositorySnapshot.getRootBranches().size() == 0) {
          LOG.debug("No root branches discovered - executing on-failure consumer");
          onFailurePathConsumer.accept(macheteFilePath);
          return;
        }

        val branchLayoutWriter = ApplicationManager.getApplication().getService(IBranchLayoutWriter.class);
        val branchLayout = repositorySnapshot.getBranchLayout();

        blockingRunWriteActionOnUIThread(() -> {
          LOG.debug("Writing branch layout & executing on-success consumer");
          MacheteFileWriter.writeBranchLayout(
              macheteFilePath,
              branchLayoutWriter,
              branchLayout,
              /* backupOldLayout */ true,
              /* requestor */ this);
          onSuccessRepositorySnapshotConsumer.accept(repositorySnapshot);
          onSuccessRepositoryConsumer.accept(repository);
        });
      }

      @Override
      @UIEffect
      public void onThrowable(Throwable e) {
        LOG.debug("Handling branch layout exception");
        VcsNotifier.getInstance(project)
            .notifyError(
                /* displayId */ null,
                getString(
                    "string.GitMachete.EnhancedGraphTable.automatic-discover.notification.title.cannot-discover-layout-error"),
                e.getMessage().requireNonNullElse(""));
      }

    }.queue();
  }
}
