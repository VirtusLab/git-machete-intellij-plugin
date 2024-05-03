package com.virtuslab.gitmachete.backend.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;

import io.vavr.Tuple;
import io.vavr.Tuple3;
import io.vavr.collection.HashMap;
import io.vavr.control.Option;
import io.vavr.control.Try;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.hooks.BaseHookExecutor;
import com.virtuslab.gitmachete.backend.hooks.OnExecutionTimeout;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
public final class StatusBranchHookExecutor extends BaseHookExecutor {
  private static final int EXECUTION_TIMEOUT_SECONDS = 5;

  // We're cheating a bit here: we're assuming that the hook's output is fixed
  // for the given (branch-name, commit-hash, hook-script-hash) tuple.
  // machete-status-branch hook spec doesn't impose any requirements like that, but:
  // 1. it's pretty unlikely that any practically useful hook won't conform to this assumption,
  // 2. this kind of caching is pretty useful wrt. performance.
  private final java.util.Map<Tuple3<String, String, String>, Option<String>> hookOutputByBranchNameCommitHashAndHookHash = new ConcurrentHashMap<>();

  @UIThreadUnsafe
  public StatusBranchHookExecutor(IGitCoreRepository gitCoreRepository) {
    super("machete-status-branch",
        gitCoreRepository.getRootDirectoryPath(),
        gitCoreRepository.getMainGitDirectoryPath(),
        gitCoreRepository.deriveConfigValue("core", "hooksPath"));
  }

  /**
   * @param branchName branch name
   * @return stdout of the hook if it has been successfully executed.
   *         Null when the hook has not been executed (because it's absent or non-executable),
   *         or when the hook has been executed but exited with non-zero status code,
   *         or when the hook timed out.
   * @throws GitMacheteException when an I/O exception occurs
   */
  @UIThreadUnsafe
  private @Nullable String executeHookFor(String branchName) throws GitMacheteException {
    // According to machete-status-branch hook spec (`git machete help hooks`),
    // the hook should receive `ASCII_ONLY=true` in its environment if only ASCII characters are expected in the output
    // (so that the hook knows not to output any ANSI escape codes etc.).
    HashMap<String, String> environment = HashMap.of("ASCII_ONLY", "true");

    val result = executeHook(EXECUTION_TIMEOUT_SECONDS, OnExecutionTimeout.RETURN_NULL, environment, branchName);

    if (result == null) {
      return null;
    }
    if (result.getExitCode() != 0) {
      LOG.warn("${name} hook for ${branchName} " +
          "returned with a non-zero (${result.getExitCode()}) exit code; ignoring the output");
      return null;
    }
    return result.getStdout();
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

    Tuple3<String, String, String> key = Tuple.of(branchName, pointedCommit.getHash(), hookContentMD5Hash);
    return hookOutputByBranchNameCommitHashAndHookHash.computeIfAbsent(key,
        k -> Try.of(() -> executeHookFor(k._1)).toOption()).getOrNull();
  }

  @Override
  protected LambdaLogger log() {
    return LOG;
  }
}
