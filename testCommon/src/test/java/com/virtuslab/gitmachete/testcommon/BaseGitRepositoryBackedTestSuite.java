package com.virtuslab.gitmachete.testcommon;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;
import org.junit.Assert;

public abstract class BaseGitRepositoryBackedTestSuite {

  protected final static String SETUP_FOR_NO_REMOTES = "setup-with-no-remotes.sh";
  protected final static String SETUP_WITH_SINGLE_REMOTE = "setup-with-single-remote.sh";
  protected final static String SETUP_WITH_MULTIPLE_REMOTES = "setup-with-multiple-remotes.sh";
  protected final static String SETUP_FOR_DIVERGED_AND_OLDER_THAN = "setup-for-diverged-and-older-than.sh";
  protected final static String SETUP_FOR_YELLOW_EDGES = "setup-for-yellow-edges.sh";
  protected final static String SETUP_FOR_OVERRIDDEN_FORK_POINT = "setup-for-overridden-fork-point.sh";

  protected final Path parentDir;
  protected final Path repositoryMainDir;
  protected final Path repositoryGitDir;

  @SneakyThrows
  protected BaseGitRepositoryBackedTestSuite() {
    parentDir = Files.createTempDirectory("machete-tests-");
    repositoryMainDir = parentDir.resolve("machete-sandbox");
    repositoryGitDir = repositoryMainDir.resolve(".git");
  }

  protected void init(String scriptName) {
    copyScriptsFromResources("common.sh");
    copyScriptsFromResources(scriptName);
    prepareRepoFromScript(scriptName);
  }

  @SneakyThrows
  private void copyScriptsFromResources(String scriptName) {
    URL resourceUrl = getClass().getResource("/" + scriptName);
    assert resourceUrl != null : "Can't get resource";
    Files.copy(Path.of(resourceUrl.toURI()), parentDir.resolve(scriptName), StandardCopyOption.REPLACE_EXISTING);
  }

  @SneakyThrows
  private void prepareRepoFromScript(String scriptName) {
    var process = Runtime.getRuntime()
        .exec("/bin/bash " + parentDir.resolve(scriptName).toAbsolutePath() + " " + parentDir.toAbsolutePath());
    var completed = process.waitFor(5, TimeUnit.SECONDS);

    // In case of non 0 exit code print stdout and stderr
    if (process.exitValue() != 0) {
      System.out.println(new String(process.getInputStream().readAllBytes()));
      System.err.println(new String(process.getErrorStream().readAllBytes()));
    }

    Assert.assertTrue(completed);
    Assert.assertEquals(0, process.exitValue());
  }
}
