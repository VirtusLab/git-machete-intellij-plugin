package com.virtuslab.gitmachete.backend.impl.hooks;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.jcabi.aspects.Loggable;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.CustomLog;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.impl.CommitOfManagedBranch;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
public final class StatusBranchHookExecutor extends BaseHookExecutor {
  private static final int EXECUTION_TIMEOUT_SECONDS = 5;

  // We're cheating a bit here: we're assuming that the hook's output is fixed for a given (branch-name, commit-hash) pair.
  // machete-status-branch hook spec doesn't impose any requirements like that, but:
  // 1. it's pretty unlikely that any practically useful hook won't conform to this assumption,
  // 2. this kind of caching is pretty useful wrt. performance.
  private final java.util.Map<Tuple3<String, String, String>, Option<String>> hookOutputByBranchNameCommitHashAndHookHash = new ConcurrentHashMap<>();

  private StatusBranchHookExecutor(File rootDirectory, File hookFile) {
    super(rootDirectory, hookFile);
  }

  public static StatusBranchHookExecutor of(IGitCoreRepository gitCoreRepository) {
    val hooksDir = gitCoreRepository.deriveConfigValue("core", "hooksPath");
    val hooksDirPath = hooksDir != null ? Paths.get(hooksDir) : gitCoreRepository.getMainGitDirectoryPath().resolve("hooks");

    val rootDirectory = gitCoreRepository.getRootDirectoryPath().toFile();
    val hookFile = hooksDirPath.resolve("machete-status-branch").toFile();

    return new StatusBranchHookExecutor(rootDirectory, hookFile);
  }

  @Loggable(value = Loggable.DEBUG)
  private @Nullable String executeHookFor(String branchName) throws GitMacheteException {
    val hookFilePath = hookFile.getAbsolutePath();
    if (!hookFile.isFile()) {
      LOG.debug(() -> "Skipping machete-status-branch hook execution for ${branchName}: " +
          "${hookFilePath} does not exist");
      return null;
    } else if (!hookFile.canExecute()) {
      LOG.warn("Skipping machete-status-branch hook execution for ${branchName}: " +
          "${hookFilePath} cannot be executed");
      return null;
    }

    LOG.debug(() -> "Executing machete-status-branch hook (${hookFilePath}) " +
        "for ${branchName} in cwd=${rootDirectory}");
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
        return null;
      }

      // It's quite likely that the hook's output will be terminated with a newline,
      // and we don't want that to be displayed.
      strippedStdout = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8).trim();
      strippedStderr = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8).trim();

      if (process.exitValue() != 0) {
        LOG.warn("machete-status-branch hook (${hookFilePath}) for ${branchName} " +
            "returned with non-zero (${process.exitValue()}) exit code; " +
            "stdout: '${strippedStdout}'; " +
            "stderr: '${strippedStderr}'");
        return null;
      }

      LOG.debug("machete-status-branch hook (${hookFilePath}) for ${branchName} " +
          "completed successfully; " +
          "stdout: '${strippedStdout}'; " +
          "stderr: '${strippedStderr}'");
      return strippedStdout;
    } catch (IOException | InterruptedException e) {
      val message = "An error occurred while running machete-status-branch hook (${hookFilePath})" +
          "for ${branchName}; ignoring the hook";
      LOG.error(message, e);
      throw new GitMacheteException(message
          + (strippedStdout != null && !strippedStdout.isBlank() ? NL + "stdout:" + NL + strippedStdout : "")
          + (strippedStderr != null && !strippedStderr.isBlank() ? NL + "stderr:" + NL + strippedStderr : ""), e);
    }
  }

  @UIThreadUnsafe
  public static String hashFile(String algorithm, File f) throws IOException, NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance(algorithm);

    try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));
        DigestOutputStream out = new DigestOutputStream(OutputStream.nullOutputStream(), md)) {
      in.transferTo(out);
    }

    return new BigInteger(/* signum */ 1, md.digest()).toString(16);
  }

  @UIThreadUnsafe
  public @Nullable String deriveHookOutputFor(String branchName, CommitOfManagedBranch pointedCommit) {
    var hookContentMD5Hash = "";
    try {
      hookContentMD5Hash = hashFile("MD5", hookFile);
    } catch (IOException | NoSuchAlgorithmException ignored) {
      // We are using the constant empty String as a neutral value, so that the functionality would
      // fall back to not using the contents of the hookFile.
    }

    val key = Tuple.of(branchName, pointedCommit.getHash(), hookContentMD5Hash);
    return hookOutputByBranchNameCommitHashAndHookHash.computeIfAbsent(key,
        k -> Try.of(() -> executeHookFor(k._1)).toOption()).getOrNull();
  }
}
