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
  private TestProcessUtils() {}

  @SneakyThrows
  public static String runProcessAndReturnStdout(Path workingDirectory, int timeoutSeconds, String... command) {
    Process process = new ProcessBuilder()
        .command(command)
        .directory(workingDirectory.toFile())
        .start();
    boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

    String stdout = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
    String stderr = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
    String commandRepr = Arrays.toString(command);
    String NL = System.lineSeparator();
    String stdoutMessage = "Stdout of " + commandRepr + ": " + NL + stdout;
    String stderrMessage = "Stderr of " + commandRepr + ": " + NL + stderr;
    String joinedMessage = NL + NL + stdoutMessage + NL + stderrMessage + NL;

    Assert.assertTrue("command " + commandRepr + " has not completed within " + timeoutSeconds + " seconds" + joinedMessage,
        completed);
    int exitValue = process.exitValue();
    Assert.assertEquals("command " + commandRepr + " has completed with exit code " + exitValue + joinedMessage, 0, exitValue);

    return stdout;
  }

  public static String runProcessAndReturnStdout(int timeoutSeconds, String... command) {
    Path currentDir = Paths.get(".").toAbsolutePath().normalize();
    return runProcessAndReturnStdout(currentDir, timeoutSeconds, command);
  }
}
