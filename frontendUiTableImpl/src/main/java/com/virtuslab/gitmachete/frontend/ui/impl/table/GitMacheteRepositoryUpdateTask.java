package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
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
import com.virtuslab.branchlayout.api.manager.IBranchLayoutReader;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryFactory;
import com.virtuslab.gitmachete.backend.api.MacheteFileReaderException;
import com.virtuslab.logger.IntelliJLoggingUtils;

@CustomLog
public final class GitMacheteRepositoryUpdateTask extends Task.Backgroundable {

  private final GitRepository gitRepository;
  private final IBranchLayoutReader branchLayoutReader;
  private final @UI Consumer<Option<IGitMacheteRepository>> doOnUIThreadWhenDone;

  private final IGitMacheteRepositoryFactory gitMacheteRepositoryFactory;

  public GitMacheteRepositoryUpdateTask(
      Project project,
      GitRepository gitRepository,
      IBranchLayoutReader branchLayoutReader,
      @UI Consumer<Option<IGitMacheteRepository>> doOnUIThreadWhenDone) {
    // Quasi-title capitalization intended since we always write "Git Machete" with initial caps.
    super(project, "Updating Git Machete repository");

    this.gitRepository = gitRepository;
    this.branchLayoutReader = branchLayoutReader;
    this.doOnUIThreadWhenDone = doOnUIThreadWhenDone;

    this.gitMacheteRepositoryFactory = RuntimeBinding.instantiateSoleImplementingClass(IGitMacheteRepositoryFactory.class);
  }

  @Override
  public void run(ProgressIndicator indicator) {
    // We can't queue repository update (onto a non-UI thread) and `doOnUIThreadWhenDone` (onto the UI thread) separately
    // since those two actions happen on two separate threads
    // and `doOnUIThreadWhenDone` can only start once repository update is complete.

    // Thus, we synchronously run repository update first...
    Option<IGitMacheteRepository> gitMacheteRepository = updateRepository();

    // ... and only once it completes, we queue `doOnUIThreadWhenDone` onto the UI thread.
    LOG.debug("Queuing graph table refresh onto the UI thread");
    GuiUtils.invokeLaterIfNeeded(() -> doOnUIThreadWhenDone.accept(gitMacheteRepository), NON_MODAL);
  }

  /**
   * Updates repository which is the base of graph table model. The change will be seen after
   * {@link GitMacheteGraphTable#refreshModel()} completes.
   *
   * This method is heavyweight and must never be invoked on UI thread.
   */
  private Option<IGitMacheteRepository> updateRepository() {
    Path mainDirectoryPath = getMainDirectoryPath(gitRepository);
    Path gitDirectoryPath = getGitDirectoryPath(gitRepository);
    Path macheteFilePath = getMacheteFilePath(gitRepository);
    boolean isMacheteFilePresent = Files.isRegularFile(macheteFilePath);

    LOG.debug(() -> "Entering: mainDirectoryPath = ${mainDirectoryPath}, gitDirectoryPath = ${gitDirectoryPath}" +
        "isMacheteFilePresent = ${isMacheteFilePresent}");
    if (isMacheteFilePresent) {
      LOG.debug("Machete file is present. Trying to create a ${IGitMacheteRepository.class.getSimpleName()} instance");

      return Try.of(() -> {
        IBranchLayout branchLayout = createBranchLayout(macheteFilePath);
        return gitMacheteRepositoryFactory.create(mainDirectoryPath, gitDirectoryPath, branchLayout);
      }).onFailure(this::handleUpdateRepositoryException).toOption();
    } else {
      LOG.debug("Machete file is absent");
      return Option.none();
    }
  }

  private IBranchLayout createBranchLayout(Path path) throws MacheteFileReaderException {
    return Try.of(() -> branchLayoutReader.read(path))
        .getOrElseThrow(e -> {
          Option<@Positive Integer> errorLine = ((BranchLayoutException) e).getErrorLine();
          return new MacheteFileReaderException("Error occurred while parsing machete file" +
              (errorLine.isDefined() ? " in line ${errorLine.get()}" : ""), e);
        });
  }

  private void handleUpdateRepositoryException(Throwable t) {
    LOG.error("Unable to create Git Machete repository", t);

    // Getting the innermost exception since it's usually the primary cause that gives most valuable message
    Throwable cause = t;
    while (cause.getCause() != null) {
      cause = cause.getCause();
    }
    String exceptionMessage = cause.getMessage();

    VcsNotifier.getInstance(getProject()).notifyError("Repository instantiation failed",
        exceptionMessage != null ? exceptionMessage : "");

    IntelliJLoggingUtils.showErrorDialog(exceptionMessage != null
        ? exceptionMessage
        : "Repository instantiation failed. For more information, please look at the IntelliJ logs");
  }

}
