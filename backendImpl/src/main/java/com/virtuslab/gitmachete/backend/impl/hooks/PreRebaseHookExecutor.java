package com.virtuslab.gitmachete.backend.impl.hooks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import io.vavr.control.Option;
import lombok.CustomLog;
import lombok.val;
import org.apache.commons.io.IOUtils;

import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;
import com.virtuslab.gitmachete.backend.api.hooks.IExecutionResult;

@CustomLog
public final class PreRebaseHookExecutor {
  private static final int EXECUTION_TIMEOUT_SECONDS = 10;
  private static final String NL = System.lineSeparator();

  private final File mainDirectory;
  private final File hookFile;

  public PreRebaseHookExecutor(IGitCoreRepository gitCoreRepository, Path mainDirectoryPath, Path gitDirectoryPath) {
    val hooksDir = gitCoreRepository.deriveConfigValue("core", "hooksPath");
    val hooksDirPath = hooksDir.map(Paths::get).getOrElse(gitDirectoryPath.resolve("hooks"));

    this.mainDirectory = mainDirectoryPath.toFile();
    this.hookFile = hooksDirPath.resolve("machete-pre-rebase").toFile();
  }

  /**
   * @param gitRebaseParameters git rebase parameters
   * @return {@link Option.Some} with exit code (possibly non-zero) when the hook has been successfully executed,
   *         or {@link Option.None} when the hook has not been executed (because it's absent or non-executable)
   * @throws GitMacheteException when a timeout or I/O exception occurs
   */
  public Option<IExecutionResult> executeHookFor(IGitRebaseParameters gitRebaseParameters) throws GitMacheteException {
    val hookFilePath = hookFile.getAbsolutePath();
    if (!hookFile.isFile()) {
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
        gitRebaseParameters.getNewBaseBranch().getFullName(),
        gitRebaseParameters.getForkPointCommit().getHash(),
        gitRebaseParameters.getCurrentBranch().getName());
    // According to git hooks spec (`git help hooks`):
    //   Before Git invokes a hook, it changes its working directory to either $GIT_DIR in a bare repository
    //   or the root of the working tree in a non-bare repository.
    //   An exception are hooks triggered during a push (...) which are always executed in $GIT_DIR.
    // We obviously assume a non-bare repository here, and machete-pre-rebase isn't related to push.
    pb.directory(mainDirectory);

    Process process;
    String strippedStdout = null;
    String strippedStderr = null;
    try {
      process = pb.start();
      boolean completed = process.waitFor(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

      strippedStdout = IOUtils.toString(process.getInputStream()).trim();
      strippedStderr = IOUtils.toString(process.getErrorStream()).trim();

      if (!completed) {
        val message = "machete-pre-rebase hook (${hookFilePath}) for ${gitRebaseParameters} " +
            "did not complete within ${EXECUTION_TIMEOUT_SECONDS} seconds; aborting the rebase";
        LOG.withTimeElapsed().error(message);
        throw new GitMacheteException(message
            + (!strippedStdout.trim().isEmpty() ? NL + "stdout:" + NL + strippedStdout : "")
            + (!strippedStderr.trim().isEmpty() ? NL + "stderr:" + NL + strippedStderr : ""));
      }

      // Can't use lambda because `strippedStdout` and `strippedStderr` are not effectively final
      LOG.debug("Stdout of machete-pre-rebase hook is '${strippedStdout}'");
      LOG.debug("Stderr of machete-pre-rebase hook is '${strippedStderr}'");
    } catch (IOException | InterruptedException e) {
      val message = "An error occurred while running machete-pre-rebase hook (${hookFilePath})" +
          "for ${gitRebaseParameters}; aborting the rebase";
      LOG.withTimeElapsed().error(message, e);
      throw new GitMacheteException(message
          + (strippedStdout != null && !strippedStdout.trim().isEmpty() ? NL + "stdout:" + NL + strippedStdout : "")
          + (strippedStderr != null && !strippedStderr.trim().isEmpty() ? NL + "stderr:" + NL + strippedStderr : ""), e);
    }

    LOG.withTimeElapsed().info(() -> "machete-pre-rebase hook (${hookFilePath}) for ${gitRebaseParameters} " +
        "returned with ${process.exitValue()} exit code");
    return Option.some(ExecutionResult.of(process.exitValue(), strippedStdout, strippedStderr));
  }
}
