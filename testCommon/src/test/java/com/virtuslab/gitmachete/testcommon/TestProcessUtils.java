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
    String gitMachete[] = {"python", "-m", "git_machete.cmd"};
    int sizeOfNewGitMachete = gitMachete.length + arguments.length;
    String[] newGitMachete = new String[sizeOfNewGitMachete];
    System.arraycopy(gitMachete, 0, newGitMachete, 0, 3);
    System.arraycopy(arguments, 0, newGitMachete, 3, arguments.length);

    return runProcessAndReturnStdout(workingDirectory, timeoutSeconds, newGitMachete);
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
