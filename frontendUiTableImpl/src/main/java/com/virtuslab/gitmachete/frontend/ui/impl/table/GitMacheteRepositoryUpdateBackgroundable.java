package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils.getGitDirectoryPath;
import static com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils.getMacheteFilePath;
import static com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils.getMainDirectoryPath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.ui.GuiUtils;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.index.qual.Positive;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutReader;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryCache;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.MacheteFileReaderException;

@CustomLog
public final class GitMacheteRepositoryUpdateBackgroundable extends Task.Backgroundable {

  private final GitRepository gitRepository;
  private final IBranchLayoutReader branchLayoutReader;
  private final @UI Consumer<Option<IGitMacheteRepositorySnapshot>> doOnUIThreadWhenDone;

  private final IGitMacheteRepositoryCache gitMacheteRepositoryCache;

  public GitMacheteRepositoryUpdateBackgroundable(
      Project project,
      GitRepository gitRepository,
      IBranchLayoutReader branchLayoutReader,
      @UI Consumer<Option<IGitMacheteRepositorySnapshot>> doOnUIThreadWhenDone) {
    super(project, getString("action.GitMachete.GitMacheteRepositoryUpdateBackgroundable.task-title"));

    this.gitRepository = gitRepository;
    this.branchLayoutReader = branchLayoutReader;
    this.doOnUIThreadWhenDone = doOnUIThreadWhenDone;

    this.gitMacheteRepositoryCache = RuntimeBinding.instantiateSoleImplementingClass(IGitMacheteRepositoryCache.class);
  }

  @Override
  public void run(ProgressIndicator indicator) {
    // We can't queue repository update (onto a non-UI thread) and `doOnUIThreadWhenDone` (onto the UI thread) separately
    // since those two actions happen on two separate threads
    // and `doOnUIThreadWhenDone` can only start once repository update is complete.

    // Thus, we synchronously run repository update first...
    Option<IGitMacheteRepositorySnapshot> gitMacheteRepositorySnapshot = updateRepositorySnapshot();

    // ... and only once it completes, we queue `doOnUIThreadWhenDone` onto the UI thread.
    LOG.debug("Queuing graph table refresh onto the UI thread");
    GuiUtils.invokeLaterIfNeeded(() -> doOnUIThreadWhenDone.accept(gitMacheteRepositorySnapshot), NON_MODAL);
  }

  /**
   * Updates the repository snapshot which is the base of graph table model. The change will be seen after
   * {@link EnhancedGraphTable#refreshModel()} completes.
   *
   * This method is heavyweight and must never be invoked on the UI thread.
   */
  private Option<IGitMacheteRepositorySnapshot> updateRepositorySnapshot() {
    Path mainDirectoryPath = getMainDirectoryPath(gitRepository);
    Path gitDirectoryPath = getGitDirectoryPath(gitRepository);
    Path macheteFilePath = getMacheteFilePath(gitRepository);
    boolean isMacheteFilePresent = Files.isRegularFile(macheteFilePath);

    LOG.debug(() -> "Entering: mainDirectoryPath = ${mainDirectoryPath}, gitDirectoryPath = ${gitDirectoryPath}, " +
        "macheteFilePath = ${macheteFilePath}, isMacheteFilePresent = ${isMacheteFilePresent}");
    if (isMacheteFilePresent) {
      LOG.debug("Machete file is present. Trying to create a repository snapshot");

      return Try.of(() -> {
        IBranchLayout branchLayout = readBranchLayout(macheteFilePath);
        return gitMacheteRepositoryCache.getInstance(mainDirectoryPath, gitDirectoryPath).createSnapshotForLayout(branchLayout);
      }).onFailure(this::handleUpdateRepositoryException).toOption();
    } else {
      LOG.debug("Machete file is absent");
      return Option.none();
    }
  }

  private IBranchLayout readBranchLayout(Path path) throws MacheteFileReaderException {
    try {
      return branchLayoutReader.read(path);
    } catch (BranchLayoutException e) {
      Option<@Positive Integer> errorLine = e.getErrorLine();
      throw new MacheteFileReaderException("Error occurred while parsing machete file" +
          (errorLine.isDefined() ? " in line ${errorLine.get()}" : ""), e);
    }
  }

  private void handleUpdateRepositoryException(Throwable t) {
    LOG.error("Unable to create Git Machete repository", t);

    // Getting the innermost exception since it's usually the primary cause that gives most valuable message
    Throwable cause = t;
    while (cause.getCause() != null) {
      cause = cause.getCause();
    }
    String exceptionMessage = cause.getMessage();

    VcsNotifier.getInstance(getProject()).notifyError(
        getString("action.GitMachete.GitMacheteRepositoryUpdateBackgroundable.notification.title.failed"),
        exceptionMessage != null ? exceptionMessage : "");
  }

}
