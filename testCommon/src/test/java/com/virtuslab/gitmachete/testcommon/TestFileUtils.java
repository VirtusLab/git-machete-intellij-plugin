package com.virtuslab.gitmachete.testcommon;

import static com.virtuslab.gitmachete.testcommon.TestProcessUtils.runProcessAndReturnStdout;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

import lombok.SneakyThrows;

public final class TestFileUtils {
  private TestFileUtils() {}

  @SneakyThrows
  public static void copyScriptFromResources(String scriptName, Path targetDir) {
    URL resourceUrl = TestFileUtils.class.getResource("/" + scriptName);
    assert resourceUrl != null : "Can't get resource";
    Files.copy(Paths.get(resourceUrl.toURI()), targetDir.resolve(scriptName), StandardCopyOption.REPLACE_EXISTING);
  }

  public static void prepareRepoFromScript(String scriptName, Path workingDir) {
    runProcessAndReturnStdout(/* workingDirectory */ workingDir, /* timeoutSeconds */ 60,
        /* command */ "bash", workingDir.resolve(scriptName).toString());
  }

  @SneakyThrows
  public static void cleanUpDir(Path dir) {
    Files.walk(dir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
  }

  @SneakyThrows
  public static void saveFile(Path directory, String fileName, String contents) {
    Files.write(directory.resolve(fileName), contents.getBytes(StandardCharsets.UTF_8));
  }
}
