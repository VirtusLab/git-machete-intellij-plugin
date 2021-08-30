package com.virtuslab.gitmachete.testcommon;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;

public final class TestProcessUtils {
  @SneakyThrows
  public static String runProcessAndReturnStdout(Path workingDirectory, int timeoutSeconds, String... command) {
    Process process = new ProcessBuilder()
        .command(command)
        .directory(workingDirectory.toFile())
        .start();
    boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

    String stdout = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);

    String commandRepr = Arrays.toString(command);
    if (!completed || process.exitValue() != 0) {
      System.out.println("Stdout of ${commandRepr}: \n");
      System.out.println(stdout);
      System.err.println("Stderr of ${commandRepr}: \n");
      System.err.println(IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8));
    }

    Assert.assertTrue("command ${commandRepr} has not completed within ${timeoutSeconds} seconds;", completed);
    int exitValue = process.exitValue();
    Assert.assertEquals("command ${commandRepr} has completed with exit code ${exitValue};", 0, exitValue);

    return stdout;
  }

  @SneakyThrows
  public static String runGitMacheteCommandAndReturnStdout(Path workingDirectory, int timeoutSeconds, String... arguments) {
    String[] gitMachete = {"python", "-m", "git_machete.cmd"};
    int sizeOfGitMachete = gitMachete.length;
    int sizeOfArguments = arguments.length;
    int sizeOfNewGitMachete = sizeOfGitMachete + sizeOfArguments;
    gitMachete = Arrays.copyOf(gitMachete, sizeOfNewGitMachete);
    for (int i = 0; i < sizeOfArguments; i++) {
      gitMachete[sizeOfNewGitMachete + (i - sizeOfArguments)] = arguments[i];
    }

    Process process = new ProcessBuilder()
        .command(gitMachete)
        .directory(workingDirectory.toFile())
        .start();
    boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

    String stdout = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);

    String commandRepr = Arrays.toString(gitMachete);
    if (!completed || process.exitValue() != 0) {
      System.out.println("Stdout of ${commandRepr}: \n");
      System.out.println(stdout);
      System.err.println("Stderr of ${commandRepr}: \n");
      System.err.println(IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8));
    }

    Assert.assertTrue("command ${commandRepr} has not completed within ${timeoutSeconds} seconds;", completed);
    int exitValue = process.exitValue();
    Assert.assertEquals("command ${commandRepr} has completed with exit code ${exitValue};", 0, exitValue);

    return stdout;
  }

  public static String runProcessAndReturnStdout(int timeoutSeconds, String... command) {
    Path currentDir = Paths.get(".").toAbsolutePath().normalize();
    return runProcessAndReturnStdout(currentDir, timeoutSeconds, command);
  }

  public static String runGitMacheteCommandAndReturnStdout(int timeoutSeconds, String... command) {
    Path currentDir = Paths.get(".").toAbsolutePath().normalize();
    return runGitMacheteCommandAndReturnStdout(currentDir, timeoutSeconds, command);
  }
}
