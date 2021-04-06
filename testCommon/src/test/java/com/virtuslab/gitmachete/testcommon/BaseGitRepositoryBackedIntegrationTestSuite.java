package com.virtuslab.gitmachete.testcommon;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;

public abstract class BaseGitRepositoryBackedIntegrationTestSuite {

  public final static String SETUP_FOR_NO_REMOTES = "setup-with-no-remotes.sh";
  public final static String SETUP_WITH_SINGLE_REMOTE = "setup-with-single-remote.sh";
  public final static String SETUP_WITH_MULTIPLE_REMOTES = "setup-with-multiple-remotes.sh";
  public final static String SETUP_FOR_DIVERGED_AND_OLDER_THAN = "setup-for-diverged-and-older-than.sh";
  public final static String SETUP_FOR_YELLOW_EDGES = "setup-for-yellow-edges.sh";
  public final static String SETUP_FOR_OVERRIDDEN_FORK_POINT = "setup-for-overridden-fork-point.sh";

  protected final Path parentDir;
  protected final Path repositoryMainDir;
  protected final Path repositoryGitDir;

  @SneakyThrows
  protected BaseGitRepositoryBackedIntegrationTestSuite(String scriptName) {
    parentDir = Files.createTempDirectory("machete-tests-");
    repositoryMainDir = parentDir.resolve("machete-sandbox");
    repositoryGitDir = repositoryMainDir.resolve(".git");

    copyScriptFromResources("common.sh");
    copyScriptFromResources(scriptName);
    prepareRepoFromScript(scriptName);
  }

  @SneakyThrows
  private void copyScriptFromResources(String scriptName) {
    URL resourceUrl = getClass().getResource("/" + scriptName);
    assert resourceUrl != null : "Can't get resource";
    Files.copy(Paths.get(resourceUrl.toURI()), parentDir.resolve(scriptName), StandardCopyOption.REPLACE_EXISTING);
  }

  @SneakyThrows
  private void prepareRepoFromScript(String scriptName) {
    val process = new ProcessBuilder()
        .command("bash", parentDir.resolve(scriptName).toString())
        .directory(parentDir.toFile())
        .start();
    val completed = process.waitFor(1, TimeUnit.MINUTES);

    if (!completed || process.exitValue() != 0) {

      System.out.println(IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8));
      System.err.println(IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8));
    }

    Assert.assertTrue(completed);
    Assert.assertEquals(0, process.exitValue());
  }

  /**
   * Be careful with this method since it might lead to race conditions when the test is finished but another process
   * (e.g. an IDE in case of UI tests) is still working and modifying the contents of {@link #parentDir}.
   * Hence, this method is not marked as {@link org.junit.Before} by default.
   */
  @SneakyThrows
  protected void cleanUpParentDir() {
    Files.walk(parentDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
  }
}
