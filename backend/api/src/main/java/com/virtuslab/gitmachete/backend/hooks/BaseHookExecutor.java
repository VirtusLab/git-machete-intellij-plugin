package com.virtuslab.gitmachete.backend.hooks;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import io.vavr.collection.List;
import io.vavr.collection.Map;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

public abstract class BaseHookExecutor {
  protected static final String NL = System.lineSeparator();

  protected final String name;
  protected final File rootDirectory;
  protected final File hookFile;

  protected BaseHookExecutor(String name, Path rootDirectoryPath, Path mainGitDirectoryPath,
      @Nullable String gitConfigCoreHooksPath) {
    this.name = name;
    this.rootDirectory = rootDirectoryPath.toFile();

    val hooksDirPath = gitConfigCoreHooksPath != null
        ? Paths.get(gitConfigCoreHooksPath)
        : mainGitDirectoryPath.resolve("hooks");
    this.hookFile = hooksDirPath.resolve(name).toFile();
  }

  protected abstract LambdaLogger log();

  @UIThreadUnsafe
  protected @Nullable ExecutionResult executeHook(int timeoutSeconds, OnExecutionTimeout onTimeout,
      Map<String, String> environment, String... args)
      throws GitMacheteException {
    val argsToString = Arrays.toString(args);
    val hookFilePath = hookFile.getAbsolutePath();
    if (!hookFile.isFile()) {
      log().debug(() -> "Skipping ${name} hook execution for ${argsToString}: ${hookFilePath} does not exist");
      return null;
    } else if (!hookFile.canExecute()) {
      log().warn("Skipping ${name} hook execution for ${argsToString}: ${hookFilePath} cannot be executed");
      return null;
    }

    log().debug(() -> "Executing ${name} hook (${hookFilePath}) for ${argsToString} in cwd=${rootDirectory}");
    ProcessBuilder pb = new ProcessBuilder();
    String[] commandAndArgs = List.of(args).prepend(hookFilePath).toJavaArray(String[]::new);
    pb.command(commandAndArgs);
    for (val item : environment) {
      pb.environment().put(item._1(), item._2());
    }

    // According to git hooks spec (`git help hooks`):
    //   Before Git invokes a hook, it changes its working directory to either $GIT_DIR in a bare repository
    //   or the root of the working tree in a non-bare repository.
    //   An exception are hooks triggered during a push (...) which are always executed in $GIT_DIR.
    // We obviously assume a non-bare repository here, and this hook isn't related to push.
    pb.directory(rootDirectory);

    Process process;
    String strippedStdout = null;
    String strippedStderr = null;
    try {
      process = pb.start();
      boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

      // It's quite likely that the hook's output will be terminated with a newline,
      // and we don't want that to be displayed.
      strippedStdout = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8).trim();
      strippedStderr = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8).trim();

      if (!completed) {
        val message = "${name} hook (${hookFilePath}) for ${argsToString} did not complete within ${timeoutSeconds} seconds";
        if (onTimeout == OnExecutionTimeout.RETURN_NULL) {
          log().warn(message);
          return null;
        } else {
          log().error(message);
          throw new GitMacheteException(message
              + (!strippedStdout.isBlank() ? NL + "stdout:" + NL + strippedStdout : "")
              + (!strippedStderr.isBlank() ? NL + "stderr:" + NL + strippedStderr : ""));

        }
      }

      log().debug("Stdout of ${name} hook is '${strippedStdout}'");
      log().debug("Stderr of ${name} hook is '${strippedStderr}'");
    } catch (IOException | InterruptedException e) {
      val message = "An error occurred while running ${name} hook (${hookFilePath}) for ${argsToString}; aborting";
      log().error(message, e);
      throw new GitMacheteException(message
          + (strippedStdout != null && !strippedStdout.isBlank() ? NL + "stdout:" + NL + strippedStdout : "")
          + (strippedStderr != null && !strippedStderr.isBlank() ? NL + "stderr:" + NL + strippedStderr : ""), e);
    }

    log().info(() -> "${name} hook (${hookFilePath}) for ${argsToString} " +
        "returned with ${process.exitValue()} exit code");
    return new ExecutionResult(process.exitValue(), strippedStdout, strippedStderr);
  }
}
