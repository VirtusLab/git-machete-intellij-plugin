package com.virtuslab.gitmachete.testcommon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;

public final class TestProcessUtils {
  private TestProcessUtils() {}

  @SneakyThrows
  public static String runProcessAndReturnStdout(Path workingDirectory, int timeoutSeconds, String... command) {
    Process process = new ProcessBuilder()
        .command(command)
        .directory(workingDirectory.toFile())
        .redirectErrorStream(true)
        .start();

    String stdout = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
    boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

    String commandRepr = Arrays.toString(command);
    String NL = System.lineSeparator();
    String stdoutMessage = "Stdout of " + commandRepr + ": " + NL + stdout;
    String joinedMessage = NL + NL + stdoutMessage + NL;

    assertTrue(
        completed, "command " + commandRepr + " has not completed within " + timeoutSeconds + " seconds" + joinedMessage);
    int exitValue = process.exitValue();
    assertEquals(0, exitValue, "command " + commandRepr + " has completed with exit code " + exitValue + joinedMessage);

    return stdout;
  }

  public static String runProcessAndReturnStdout(int timeoutSeconds, String... command) {
    Path currentDir = Paths.get(".").toAbsolutePath().normalize();
    return runProcessAndReturnStdout(currentDir, timeoutSeconds, command);
  }
}
