package com.virtuslab.gitmachete.backend.integration;

import static com.virtuslab.gitmachete.testcommon.SetupScripts.ALL_SETUP_SCRIPTS;
import static com.virtuslab.gitmachete.testcommon.TestFileUtils.cleanUpDir;
import static com.virtuslab.gitmachete.testcommon.TestFileUtils.copyScriptFromResources;
import static com.virtuslab.gitmachete.testcommon.TestFileUtils.prepareRepoFromScript;
import static com.virtuslab.gitmachete.testcommon.TestFileUtils.saveFile;
import static com.virtuslab.gitmachete.testcommon.TestProcessUtils.runProcessAndReturnStdout;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.vavr.collection.List;
import io.vavr.collection.Stream;
import lombok.SneakyThrows;

public class RegenerateCliOutputs {
  @SneakyThrows
  public static void main(String[] args) {
    Path outputDirectory = Paths.get(args[0]);

    String versionOutput = runGitMacheteCommandAndReturnStdout(/* timeoutSeconds */ 5, /* command */ "version");
    saveFile(outputDirectory, "version.txt", versionOutput);

    for (String scriptName : ALL_SETUP_SCRIPTS) {
      Path parentDir = Files.createTempDirectory("machete-tests-");
      Path repositoryMainDir = parentDir.resolve("machete-sandbox");

      copyScriptFromResources("common.sh", parentDir);
      copyScriptFromResources(scriptName, parentDir);
      prepareRepoFromScript(scriptName, parentDir);
      String statusOutput = runGitMacheteCommandAndReturnStdout(/* workingDirectory */ repositoryMainDir,
          /* timeoutSeconds */ 15, /* command */ "status", "--list-commits");

      saveFile(outputDirectory, "${scriptName}-status.txt", statusOutput);

      String rawDiscoverOutput = runGitMacheteCommandAndReturnStdout(/* workingDirectory */ repositoryMainDir,
          /* timeoutSeconds */ 15, /* command */ "discover", "--list-commits", "--yes");

      String discoverOutput = Stream.of(rawDiscoverOutput.split(System.lineSeparator()))
          .drop(2) // Let's skip the informational output at the beginning and at the end.
          .dropRight(2)
          .mkString(System.lineSeparator());

      saveFile(outputDirectory, "${scriptName}-discover.txt", discoverOutput);

      cleanUpDir(parentDir);
    }
  }

  @SneakyThrows
  private static String runGitMacheteCommandAndReturnStdout(Path workingDirectory, int timeoutSeconds, String... arguments) {
    String[] commandAndArgs = List.of("git", "machete").appendAll(List.of(arguments)).toJavaArray(String[]::new);
    return runProcessAndReturnStdout(workingDirectory, timeoutSeconds, commandAndArgs);
  }

  private static String runGitMacheteCommandAndReturnStdout(int timeoutSeconds, String... command) {
    Path currentDir = Paths.get(".").toAbsolutePath().normalize();
    return runGitMacheteCommandAndReturnStdout(currentDir, timeoutSeconds, command);
  }
}
