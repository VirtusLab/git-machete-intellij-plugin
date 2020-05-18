package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
import static com.virtuslab.gitmachete.frontend.ui.impl.table.GitPathUtils.getGitDirectoryPath;
import static com.virtuslab.gitmachete.frontend.ui.impl.table.GitPathUtils.getMacheteFilePath;
import static com.virtuslab.gitmachete.frontend.ui.impl.table.GitPathUtils.getMainDirectoryPath;

import java.nio.file.Files;
import java.nio.file.Path;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.ui.GuiUtils;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.checkerframework.checker.index.qual.Positive;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.manager.IBranchLayoutReader;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryFactory;
import com.virtuslab.gitmachete.backend.api.MacheteFileReaderException;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseGraphTable;
import com.virtuslab.logger.EnhancedLambdaLoggerFactory;
import com.virtuslab.logger.IEnhancedLambdaLogger;
import com.virtuslab.logger.IntelliJLoggingUtils;

public final class GitMacheteRepositoryUpdateTask extends Task.Backgroundable {

  private static final IEnhancedLambdaLogger LOG = EnhancedLambdaLoggerFactory.create();

  private final BaseGraphTable graphTable;
  private final Project project;
  private final GitRepository gitRepository;
  private final IBranchLayoutReader branchLayoutReader;

  private final IGitMacheteRepositoryFactory gitMacheteRepositoryFactory;

  private GitMacheteRepositoryUpdateTask(BaseGraphTable graphTable, Project project, GitRepository gitRepository,
      IBranchLayoutReader branchLayoutReader) {
    super(project, "Updating Git Machete repository");

    this.graphTable = graphTable;
    this.project = project;
    this.gitRepository = gitRepository;
    this.branchLayoutReader = branchLayoutReader;

    this.gitMacheteRepositoryFactory = RuntimeBinding.instantiateSoleImplementingClass(IGitMacheteRepositoryFactory.class);
  }

  public static GitMacheteRepositoryUpdateTask of(BaseGraphTable graphTable, Project project, GitRepository gitRepository,
      IBranchLayoutReader branchLayoutReader) {
    return new GitMacheteRepositoryUpdateTask(graphTable, project, gitRepository, branchLayoutReader);
  }

  @Override
  public void run(ProgressIndicator indicator) {
    // We can't queue repository update (onto a non-UI thread) and graph table refresh (onto the UI thread) separately
    // since those two actions happen on two separate threads
    // and graph table refresh can only start once repository update is complete.

    // Thus, we synchronously run repository update first...
    Option<IGitMacheteRepository> gitMacheteRepository = updateRepository();

    // ... and only once it completes, we queue graph table update onto the UI thread.
    LOG.debug("Queuing graph table refresh onto the UI thread");
    // A bit of a shortcut: we're accessing filesystem even though we may be on UI thread here;
    // this shouldn't ever be a heavyweight operation, however.
    Path macheteFilePath = getMacheteFilePath(gitRepository);
    boolean isMacheteFilePresent = Files.isRegularFile(macheteFilePath);

    GuiUtils.invokeLaterIfNeeded(
        () -> graphTable.refreshModel(gitMacheteRepository.getOrNull(), macheteFilePath, isMacheteFilePresent),
        NON_MODAL);
  }

  /**
   * Updates repository which is the base of graph table model. The change will be seen after
   * {@link GitMacheteGraphTable#refreshModel} completes.
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
      }).onFailure(this::handleUpdateRepositoryExceptions).toOption();
    } else {
      LOG.debug("Machete file is absent. Setting repository reference to null");
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

  private void handleUpdateRepositoryExceptions(Throwable t) {
    LOG.error("Unable to create Git Machete repository", t);

    // Getting the innermost exception since it's usually the primary cause that gives most valuable message
    Throwable cause = t;
    while (cause.getCause() != null) {
      cause = cause.getCause();
    }
    String exceptionMessage = cause.getMessage();

    VcsNotifier.getInstance(project).notifyError("Repository instantiation failed",
        exceptionMessage != null ? exceptionMessage : "");

    IntelliJLoggingUtils.showErrorDialog(exceptionMessage != null
        ? exceptionMessage
        : "Repository instantiation failed. For more information, please look at the IntelliJ logs");
  }

}
