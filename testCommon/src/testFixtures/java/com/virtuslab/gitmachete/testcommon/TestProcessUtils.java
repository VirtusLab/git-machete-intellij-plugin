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

    Assert.assertTrue(
        "Stdout of ${commandRepr}: ${System.lineSeparator()} ${stdout};" +
            "\n Stderr of ${commandRepr}: ${System.lineSeparator() ${stderr};" +
            "\n command ${commandRepr} has not completed within ${timeoutSeconds} seconds;",
        completed);
    int exitValue = process.exitValue();
    Assert.assertEquals("Stdout of ${commandRepr}: ${System.lineSeparator()} ${stdout};" +
        "\n Stderr of ${commandRepr}: ${System.lineSeparator() ${stderr};" +
        "\ncommand ${commandRepr} has completed with exit code ${exitValue};", 0, exitValue);

    return stdout;
  }

  public static String runProcessAndReturnStdout(int timeoutSeconds, String... command) {
    Path currentDir = Paths.get(".").toAbsolutePath().normalize();
    return runProcessAndReturnStdout(currentDir, timeoutSeconds, command);
  }
}
