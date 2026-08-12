package com.virtuslab.gitmachete.testcommon;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.io.IOUtils;

public final class TestFileUtils {
  private TestFileUtils() {}

  @SneakyThrows
  public static void copyScriptFromResources(String scriptName, Path targetDir) {
    String scriptContents = IOUtils.resourceToString("/" + scriptName, StandardCharsets.UTF_8);
    Files.copy(IOUtils.toInputStream(scriptContents, StandardCharsets.UTF_8), targetDir.resolve(scriptName),
        StandardCopyOption.REPLACE_EXISTING);
  }

  @SneakyThrows
  public static String runProcessAndReturnStdout(int timeoutSeconds, String... command) {
    Path currentDir = Paths.get(".").toAbsolutePath().normalize();
    return TestProcessUtils.runProcessAndReturnStdout(currentDir, timeoutSeconds, command);
  }

  public static String getShellExecutable() {
    String shell = "bash";
    String osName = System.getProperty("os.name").toLowerCase();
    if (osName.contains("windows")) {
      String gitPath = runProcessAndReturnStdout(5, "where", "git").trim().split(System.lineSeparator())[0];
      if (gitPath.endsWith("cmd\\git.exe")) {
        shell = gitPath.replace("cmd\\git.exe", "bin\\sh.exe");
      } else if (gitPath.endsWith("bin\\git.exe")) {
        shell = gitPath.replace("bin\\git.exe", "bin\\sh.exe");
      } else {
        shell = "sh"; // fall back to PATH
      }
    }
    return shell;
  }

  @SneakyThrows
  public static void prepareRepoFromScript(String scriptName, Path workingDir) {
    String shell = getShellExecutable();
    String scriptPath = workingDir.resolve(scriptName).toString();
    TestProcessUtils.runProcessAndReturnStdout(/* workingDirectory */ workingDir, /* timeoutSeconds */ 60, shell, scriptPath);
  }

  @SneakyThrows
  public static void cleanUpDir(Path dir) {
    try (val walkDirs = Files.walk(dir)) {
      walkDirs.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }
  }

  @SneakyThrows
  public static void saveFile(Path directory, String fileName, String contents) {
    Files.write(directory.resolve(fileName), contents.getBytes(StandardCharsets.UTF_8));
  }
}
