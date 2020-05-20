package com.virtuslab.gitmachete.backend.impl.hooks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.CustomLog;

import com.virtuslab.gitmachete.backend.impl.GitMacheteCommit;

@CustomLog
public final class StatusBranchHookExecutor {
  // sun.nio.fs.UnixPath has a reasonable equals/hashCode.
  private static final java.util.Map<Tuple2<Path, Path>, StatusBranchHookExecutor> instanceByMainDirAndGitDirCache = new ConcurrentHashMap<>();

  private final File mainDirectory;
  private final File hookFile;

  // We're cheating a bit here: we're assuming that the hook's output is fixed for a given (branch-name, commit-hash) pair.
  // machete-status-branch hook spec doesn't impose any requirements like that, but:
  // 1. it's pretty unlikely that any practically useful hook won't conform to this assumption
  // 2. this kind of caching is pretty useful wrt. performance, esp. given that executors themselves are also cached
  //    for a given (main-dir, git-dir) pair.
  private final java.util.Map<Tuple2<String, String>, Option<String>> hookOutputByBranchNameAndCommitHash = new ConcurrentHashMap<>();

  public static StatusBranchHookExecutor of(Path mainDirectoryPath, Path gitDirectoryPath) {
    return instanceByMainDirAndGitDirCache.computeIfAbsent(
        /* key */ Tuple.of(mainDirectoryPath, gitDirectoryPath),
        /* mappingFunction */ k -> new StatusBranchHookExecutor(k._1, k._2));
  }

  private StatusBranchHookExecutor(Path mainDirectoryPath, Path gitDirectoryPath) {
    this.mainDirectory = mainDirectoryPath.toFile();
    // TODO (#289): first take `git config core.hooksPath` into account; possibly JGit has a helper for that
    this.hookFile = gitDirectoryPath.resolve("hooks").resolve("machete-status-branch").toFile();
  }

  private Option<String> executeHookFor(String branchName) throws IOException, InterruptedException {
    if (!hookFile.exists()) {
      LOG.debug("Skipping machete-status-branch hook execution for ${branchName}: " +
          "${hookFile.getAbsolutePath()} does not exist");
      return Option.none();
    } else if (!hookFile.canExecute()) {
      LOG.debug("Skipping machete-status-branch hook execution for ${branchName}: " +
          "${hookFile.getAbsolutePath()} cannot be executed");
      return Option.none();
    }

    LOG.startTimer().debug("Executing machete-status-branch hook (${hookFile.getAbsolutePath()}) " +
        "for ${branchName} in cwd=${mainDirectory}");
    ProcessBuilder pb = new ProcessBuilder();
    pb.command(hookFile.getAbsolutePath(), branchName);
    // According to machete-status-branch hook spec (`git machete help hooks`),
    // the hook should receive `ASCII_ONLY=true` in its environment if only ASCII characters are expected in the output
    // (so that the hook knows not to output any ANSI escape codes etc.).
    pb.environment().put("ASCII_ONLY", "true");
    // According to git hooks spec (`git help hooks`):
    //   Before Git invokes a hook, it changes its working directory to either $GIT_DIR in a bare repository
    //   or the root of the working tree in a non-bare repository.
    //   An exception are hooks triggered during a push (...) which are always executed in $GIT_DIR.
    // We obviously assume a non-bare repository here, and machete-status-branch isn't related to push.
    pb.directory(mainDirectory);

    Process process = pb.start();
    int timeoutSeconds = 1;
    boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
    if (!completed) {
      LOG.withTimeElapsed().warn("machete-status-branch hook (${hookFile.getAbsolutePath()}) for ${branchName} " +
          "did not complete within ${timeoutSeconds} seconds; ignoring the output");
      return Option.none();
    } else if (process.exitValue() != 0) {
      LOG.withTimeElapsed().warn("machete-status-branch hook (${hookFile.getAbsolutePath()}) for ${branchName} " +
          "returned with non-zero (${process.exitValue()}) exit code; ignoring the output");
      return Option.none();
    }

    // It's quite likely that the hook's output will be terminated with a newline,
    // and we don't want that to be displayed.
    String strippedOutput = new String(process.getInputStream().readAllBytes()).stripTrailing();
    LOG.withTimeElapsed().debug("Output of machete-status-branch hook (${hookFile.getAbsolutePath()}) " +
        "for ${branchName} is '${strippedOutput}'");
    return Option.some(strippedOutput);
  }

  public Option<String> deriveHookOutputFor(String branchName, GitMacheteCommit pointedCommit) {
    var key = Tuple.of(branchName, pointedCommit.getHash());
    return hookOutputByBranchNameAndCommitHash.computeIfAbsent(key,
        k -> Try.of(() -> executeHookFor(k._1)).getOrElse(Option.none()));
  }
}
