package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.util.ModalityUiUtil;
import git4idea.repo.GitRepository;
import io.vavr.control.Try;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutReader;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryCache;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.MacheteFileReaderException;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod(GitVfsUtils.class)
@CustomLog
public final class GitMacheteRepositoryUpdateBackgroundable extends Task.Backgroundable {

  private final GitRepository gitRepository;
  private final IBranchLayoutReader branchLayoutReader;
  private final @UI Consumer<@Nullable IGitMacheteRepositorySnapshot> doOnUIThreadWhenDone;
  private final Consumer<@Nullable IGitMacheteRepository> gitMacheteRepositoryConsumer;

  private final IGitMacheteRepositoryCache gitMacheteRepositoryCache;

  public GitMacheteRepositoryUpdateBackgroundable(
      Project project,
      GitRepository gitRepository,
      IBranchLayoutReader branchLayoutReader,
      @UI Consumer<@Nullable IGitMacheteRepositorySnapshot> doOnUIThreadWhenDone,
      Consumer<@Nullable IGitMacheteRepository> gitMacheteRepositoryConsumer) {
    super(project, getString("action.GitMachete.GitMacheteRepositoryUpdateBackgroundable.task-title"));

    this.gitRepository = gitRepository;
    this.branchLayoutReader = branchLayoutReader;
    this.doOnUIThreadWhenDone = doOnUIThreadWhenDone;
    this.gitMacheteRepositoryConsumer = gitMacheteRepositoryConsumer;

    this.gitMacheteRepositoryCache = RuntimeBinding.instantiateSoleImplementingClass(IGitMacheteRepositoryCache.class);
  }

  @UIThreadUnsafe
  @Override
  public void run(ProgressIndicator indicator) {
    // We can't queue repository update (onto a non-UI thread) and `doOnUIThreadWhenDone` (onto the UI thread) separately
    // since those two actions happen on two separate threads
    // and `doOnUIThreadWhenDone` can only start once repository update is complete.

    // Thus, we synchronously run repository update first...
    IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot = updateRepositorySnapshot();

    // ... and only once it completes, we queue `doOnUIThreadWhenDone` onto the UI thread.
    LOG.debug("Queuing graph table refresh onto the UI thread");
    ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL, () -> doOnUIThreadWhenDone.accept(gitMacheteRepositorySnapshot));
  }

  /**
   * Updates the repository snapshot which is the base of graph table model. The change will be seen after
   * {@link EnhancedGraphTable#refreshModel()} completes.
   *
   * This method is heavyweight and must never be invoked on the UI thread.
   */
  @UIThreadUnsafe
  private @Nullable IGitMacheteRepositorySnapshot updateRepositorySnapshot() {
    Path rootDirectoryPath = gitRepository.getRootDirectoryPath();
    Path mainGitDirectoryPath = gitRepository.getMainGitDirectoryPath();
    Path worktreeGitDirectoryPath = gitRepository.getWorktreeGitDirectoryPath();
    Path macheteFilePath = gitRepository.getMacheteFilePath();
    boolean isMacheteFilePresent = Files.isRegularFile(macheteFilePath);

    LOG.debug(() -> "Entering: rootDirectoryPath = ${rootDirectoryPath}, mainGitDirectoryPath = ${mainGitDirectoryPath}, " +
        "macheteFilePath = ${macheteFilePath}, isMacheteFilePresent = ${isMacheteFilePresent}");
    if (isMacheteFilePresent) {
      LOG.debug("Machete file is present. Trying to create a repository snapshot");

      return Try.of(() -> {
        BranchLayout branchLayout = readBranchLayout(macheteFilePath);
        IGitMacheteRepository gitMacheteRepository = gitMacheteRepositoryCache.getInstance(rootDirectoryPath,
            mainGitDirectoryPath, worktreeGitDirectoryPath);
        gitMacheteRepositoryConsumer.accept(gitMacheteRepository);
        return gitMacheteRepository.createSnapshotForLayout(branchLayout);
      }).onFailure(this::handleUpdateRepositoryException).getOrNull();
    } else {
      LOG.debug("Machete file is absent");
      return null;
    }
  }

  @UIThreadUnsafe
  private BranchLayout readBranchLayout(Path path) throws MacheteFileReaderException {
    try {
      return branchLayoutReader.read(path);
    } catch (BranchLayoutException e) {
      @Positive Integer errorLine = e.getErrorLine();
      throw new MacheteFileReaderException("Error occurred while parsing machete file" +
          (errorLine != null ? " in line ${errorLine}" : ""), e);
    }
  }

  private void handleUpdateRepositoryException(Throwable t) {
    LOG.warn("Unable to create Git Machete repository", t);

    // Getting the innermost exception since it's usually the primary cause that gives most valuable message
    Throwable cause = t;
    while (cause.getCause() != null) {
      cause = cause.getCause();
    }
    String exceptionMessage = cause.getMessage();

    VcsNotifier.getInstance(getProject()).notifyError(/* displayId */ null,
        getString("action.GitMachete.GitMacheteRepositoryUpdateBackgroundable.notification.title.failed"),
        exceptionMessage != null ? exceptionMessage : "");
  }

}
