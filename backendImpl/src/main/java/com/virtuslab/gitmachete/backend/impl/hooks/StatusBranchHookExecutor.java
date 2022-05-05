package com.virtuslab.gitmachete.backend.impl.hooks;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.jcabi.aspects.Loggable;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.CustomLog;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.FS;

import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.impl.CommitOfManagedBranch;

@CustomLog
public final class StatusBranchHookExecutor extends BaseHookExecutor {
  private static final int EXECUTION_TIMEOUT_SECONDS = 1;

  // We're cheating a bit here: we're assuming that the hook's output is fixed for a given (branch-name, commit-hash) pair.
  // machete-status-branch hook spec doesn't impose any requirements like that, but:
  // 1. it's pretty unlikely that any practically useful hook won't conform to this assumption,
  // 2. this kind of caching is pretty useful wrt. performance.
  private final java.util.Map<Tuple2<String, String>, Option<String>> hookOutputByBranchNameAndCommitHash = new ConcurrentHashMap<>();

  private StatusBranchHookExecutor(File rootDirectory, File mainGitDirectory, File hookFile) {
    super(rootDirectory, mainGitDirectory, hookFile);
  }

  public static StatusBranchHookExecutor of(IGitCoreRepository gitCoreRepository) {
    val hooksDir = gitCoreRepository.deriveConfigValue("core", "hooksPath");
    val hooksDirPath = hooksDir.map(Paths::get).getOrElse(gitCoreRepository.getMainGitDirectoryPath().resolve("hooks"));

    val rootDirectory = gitCoreRepository.getRootDirectoryPath().toFile();
    val mainGitDirectory = gitCoreRepository.getMainGitDirectoryPath().toFile();
    val hookFile = hooksDirPath.resolve("machete-status-branch").toFile();

    return new StatusBranchHookExecutor(rootDirectory, mainGitDirectory, hookFile);
  }

  @Loggable(value = Loggable.DEBUG)
  private Option<String> executeHookFor(String branchName) throws GitMacheteException {
    val hookFilePath = hookFile.getAbsolutePath();
    if (!hookFile.isFile()) {
      LOG.debug(() -> "Skipping machete-status-branch hook execution for ${branchName}: " +
          "${hookFilePath} does not exist");
      return Option.none();
    } else if (!hookFile.canExecute()) {
      LOG.warn("Skipping machete-status-branch hook execution for ${branchName}: " +
          "${hookFilePath} cannot be executed");
      return Option.none();
    }

    LOG.debug(() -> "Executing machete-status-branch hook (${hookFilePath}) " +
        "for ${branchName} in cwd=${rootDirectory}");

    val builderForMainGitDir = new FileRepositoryBuilder();
    builderForMainGitDir.setWorkTree(this.rootDirectory);
    builderForMainGitDir.setGitDir(this.mainGitDirectory);
    Repository jgitRepoForMainGitDir = Try.of(() -> builderForMainGitDir.build()).getOrElseThrow(
        e -> new GitMacheteException("Cannot create a repository object for " +
            "rootDirectoryPath=${this.rootDirectory}, mainGitDirectoryPath=${this.mainGitDirectory}", e));

    OutputStream outRedirect = new ByteArrayOutputStream();
    OutputStream errRedirect = new ByteArrayOutputStream();

    @SuppressWarnings("nullness:argument") val x = FS.DETECTED.runHookIfPresent(jgitRepoForMainGitDir, "machete-status-branch",
        new String[]{branchName}, outRedirect,
        errRedirect, null);

    if (true) {
      return Option.of("Test");
    }

    ProcessBuilder pb = new ProcessBuilder();
    pb.command(hookFilePath, branchName);
    // According to machete-status-branch hook spec (`git machete help hooks`),
    // the hook should receive `ASCII_ONLY=true` in its environment if only ASCII characters are expected in the output
    // (so that the hook knows not to output any ANSI escape codes etc.).
    pb.environment().put("ASCII_ONLY", "true");
    // According to git hooks spec (`git help hooks`):
    //   Before Git invokes a hook, it changes its working directory to either $GIT_DIR in a bare repository
    //   or the root of the working tree in a non-bare repository.
    //   An exception are hooks triggered during a push (...) which are always executed in $GIT_DIR.
    // We obviously assume a non-bare repository here, and machete-status-branch isn't related to push.
    pb.directory(rootDirectory);

    Process process;
    String strippedStdout = null;
    String strippedStderr = null;
    try {
      process = pb.start();
      boolean completed = process.waitFor(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

      if (!completed) {
        LOG.warn("machete-status-branch hook (${hookFilePath}) for ${branchName} " +
            "did not complete within ${EXECUTION_TIMEOUT_SECONDS} seconds; ignoring the output");
        return Option.none();
      }
      if (process.exitValue() != 0) {
        LOG.warn("machete-status-branch hook (${hookFilePath}) for ${branchName} " +
            "returned with non-zero (${process.exitValue()}) exit code; ignoring the output");
        return Option.none();
      }

      // It's quite likely that the hook's output will be terminated with a newline,
      // and we don't want that to be displayed.
      strippedStdout = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8).trim();
      strippedStderr = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8).trim();

      LOG.debug("Output of machete-status-branch hook (${hookFilePath}) " +
          "for ${branchName} is '${strippedStdout}'");
      return Option.some(strippedStdout);
    } catch (IOException | InterruptedException e) {
      val message = "An error occurred while running machete-status-branch hook (${hookFilePath})" +
          "for ${branchName}; ignoring the hook";
      LOG.error(message, e);
      throw new GitMacheteException(message
          + (strippedStdout != null && !strippedStdout.trim().isEmpty() ? NL + "stdout:" + NL + strippedStdout : "")
          + (strippedStderr != null && !strippedStderr.trim().isEmpty() ? NL + "stderr:" + NL + strippedStderr : ""), e);
    }
  }

  public Option<String> deriveHookOutputFor(String branchName, CommitOfManagedBranch pointedCommit) {
    val key = Tuple.of(branchName, pointedCommit.getHash());
    return hookOutputByBranchNameAndCommitHash.computeIfAbsent(key,
        k -> Try.of(() -> executeHookFor(k._1)).getOrElse(Option.none()));
  }
}
