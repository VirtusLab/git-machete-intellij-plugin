package com.virtuslab.gitmachete.frontend.ui.impl.backgroundables;

import static com.virtuslab.gitmachete.frontend.common.WriteActionUtils.blockingRunWriteActionOnUIThread;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.nio.file.Path;
import java.util.Objects;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.repo.GitRepository;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryCache;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.frontend.file.MacheteFileWriter;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.async.ContinuesInBackground;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod({GitVfsUtils.class, Objects.class})
public abstract class AutodiscoverBackgroundable extends Task.Backgroundable {
  private final Project project;
  private final GitRepository gitRepository;

  protected abstract void onDiscoverFailure();

  @ContinuesInBackground
  protected abstract void onDiscoverSuccess(IGitMacheteRepository repository, IGitMacheteRepositorySnapshot repositorySnapshot);

  private final Path macheteFilePath;

  public AutodiscoverBackgroundable(GitRepository gitRepository, Path macheteFilePath) {
    super(gitRepository.getProject(),
        getNonHtmlString("string.GitMachete.AutodiscoverBackgroundable.automatic-discover.task-title"));
    this.project = gitRepository.getProject();
    this.gitRepository = gitRepository;
    this.macheteFilePath = macheteFilePath;
  }

  @Override
  @ContinuesInBackground
  @UIThreadUnsafe
  public void run(ProgressIndicator indicator) {
    if (gitRepository == null) {
      return;
    }
    Path rootDirPath = gitRepository.getRootDirectoryPath().toAbsolutePath();
    Path mainGitDirPath = gitRepository.getMainGitDirectoryPath().toAbsolutePath();
    Path worktreeGitDirPath = gitRepository.getWorktreeGitDirectoryPath().toAbsolutePath();

    IGitMacheteRepository repository;

    try {
      repository = ApplicationManager.getApplication().getService(IGitMacheteRepositoryCache.class)
          .getInstance(rootDirPath, mainGitDirPath, worktreeGitDirPath);
    } catch (GitMacheteException e) {
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
      VcsNotifier.getInstance(project)
          .notifyError(
              /* displayId */ null,
              getString(
                  "string.GitMachete.EnhancedGraphTable.automatic-discover.notification.title.cannot-discover-layout-error"),
              e.getMessage().requireNonNullElse(""));
      return;
    }

    if (repositorySnapshot.getRootBranches().size() == 0) {
      onDiscoverFailure();
      return;
    }

    val branchLayoutWriter = ApplicationManager.getApplication().getService(IBranchLayoutWriter.class);
    val branchLayout = repositorySnapshot.getBranchLayout();

    blockingRunWriteActionOnUIThread(() -> {
      MacheteFileWriter.writeBranchLayout(
          macheteFilePath,
          branchLayoutWriter,
          branchLayout,
          /* backupOldLayout */ true,
          /* requestor */ this);
    });

    onDiscoverSuccess(repository, repositorySnapshot);
  }

  @Override
  @UIEffect
  public void onThrowable(Throwable e) {
    onDiscoverFailure();

    VcsNotifier.getInstance(project)
        .notifyError(
            /* displayId */ null,
            getString(
                "string.GitMachete.EnhancedGraphTable.automatic-discover.notification.title.cannot-discover-layout-error"),
            e.getMessage().requireNonNullElse(""));
  }
}
