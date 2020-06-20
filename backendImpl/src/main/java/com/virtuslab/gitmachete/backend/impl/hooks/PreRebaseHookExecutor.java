package com.virtuslab.gitmachete.backend.impl.hooks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import io.vavr.control.Option;
import lombok.CustomLog;

import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;

@CustomLog
public final class PreRebaseHookExecutor {
  private static final int EXECUTION_TIMEOUT_SECONDS = 10;

  private final File mainDirectory;
  private final File hookFile;

  public static PreRebaseHookExecutor of(Path mainDirectoryPath, Path gitDirectoryPath) {
    return new PreRebaseHookExecutor(mainDirectoryPath, gitDirectoryPath);
  }

  private PreRebaseHookExecutor(Path mainDirectoryPath, Path gitDirectoryPath) {
    this.mainDirectory = mainDirectoryPath.toFile();
    // TODO (#289): first take `git config core.hooksPath` into account; possibly JGit has a helper for that
    this.hookFile = gitDirectoryPath.resolve("hooks").resolve("machete-pre-rebase").toFile();
  }

  /**
   * @param gitRebaseParameters git rebase parameters
   * @return {@link Option.Some} with exit code (possibly non-zero) when the hook has been successfully executed,
   *         or {@link Option.None} when the hook has not been executed (because it's absent or non-executable)
   * @throws GitMacheteException when a timeout or I/O exception occurs
   */
  public Option<Integer> executeHookFor(IGitRebaseParameters gitRebaseParameters) throws GitMacheteException {
    var hookFilePath = hookFile.getAbsolutePath();
    if (!hookFile.exists()) {
      LOG.debug(() -> "Skipping machete-pre-rebase hook execution for ${gitRebaseParameters}: " +
          "${hookFilePath} does not exist");
      return Option.none();
    } else if (!hookFile.canExecute()) {
      LOG.warn("Skipping machete-status-branch hook execution for ${gitRebaseParameters}: " +
          "${hookFilePath} cannot be executed");
      return Option.none();
    }

    LOG.startTimer().debug(() -> "Executing machete-pre-rebase hook (${hookFilePath}) " +
        "for ${gitRebaseParameters} in cwd=${mainDirectory}");
    ProcessBuilder pb = new ProcessBuilder();
    pb.command(
        hookFilePath,
        gitRebaseParameters.getNewBaseCommit().getHash(),
        gitRebaseParameters.getForkPointCommit().getHash(),
        gitRebaseParameters.getCurrentBranch().getName());
    // According to git hooks spec (`git help hooks`):
    //   Before Git invokes a hook, it changes its working directory to either $GIT_DIR in a bare repository
    //   or the root of the working tree in a non-bare repository.
    //   An exception are hooks triggered during a push (...) which are always executed in $GIT_DIR.
    // We obviously assume a non-bare repository here, and machete-pre-rebase isn't related to push.
    pb.directory(mainDirectory);

    Process process;
    try {
      process = pb.start();
      boolean completed = process.waitFor(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

      if (!completed) {
        var message = "machete-pre-rebase hook (${hookFilePath}) for ${gitRebaseParameters} " +
            "did not complete within ${EXECUTION_TIMEOUT_SECONDS} seconds; aborting the rebase";
        LOG.withTimeElapsed().error(message);
        throw new GitMacheteException(message);
      }

      String strippedStdout = new String(process.getInputStream().readAllBytes()).stripTrailing();
      String strippedStderr = new String(process.getErrorStream().readAllBytes()).stripTrailing();
      LOG.debug(() -> "Stdout of machete-pre-rebase hook is '${strippedStdout}'");
      LOG.debug(() -> "Stderr of machete-pre-rebase hook is '${strippedStderr}'");
    } catch (IOException | InterruptedException e) {
      LOG.withTimeElapsed().error("An error occurred while running machete-pre-rebase hook (${hookFilePath})" +
          "for ${gitRebaseParameters}; aborting the rebase", e);
      throw new GitMacheteException(e);
    }

    LOG.withTimeElapsed().info(() -> "machete-pre-rebase hook (${hookFilePath}) for ${gitRebaseParameters} " +
        "returned with ${process.exitValue()} exit code");
    return Option.some(process.exitValue());
  }
}
