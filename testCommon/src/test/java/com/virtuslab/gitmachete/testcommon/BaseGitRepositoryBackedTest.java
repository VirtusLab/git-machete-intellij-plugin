package com.virtuslab.gitmachete.testcommon;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;

public abstract class BaseGitRepositoryBackedTest {

  protected final static String SETUP_WITH_SINGLE_REMOTE = "setup-with-single-remote.sh";
  protected final static String SETUP_WITH_MULTIPLE_REMOTES = "setup-with-multiple-remotes.sh";
  protected final static String SETUP_FOR_DIVERGED_AND_OLDER_THAN = "setup-for-diverged-and-older-than.sh";

  protected final Path parentDir = Files.createTempDirectory("machete-tests-");
  protected final Path repositoryMainDir = parentDir.resolve("machete-sandbox");
  protected final Path repositoryGitDir = repositoryMainDir.resolve(".git");

  protected BaseGitRepositoryBackedTest() throws IOException {}

  protected void init(String scriptName) throws Exception {
    copyScriptsFromResources("common.sh");
    copyScriptsFromResources(scriptName);
    prepareRepoFromScript(scriptName);
  }

  private void copyScriptsFromResources(String scriptName) throws URISyntaxException, IOException {
    URL resourceUrl = getClass().getResource("/" + scriptName);
    assert resourceUrl != null : "Can't get resource";
    Files.copy(Path.of(resourceUrl.toURI()), parentDir.resolve(scriptName), StandardCopyOption.REPLACE_EXISTING);
  }

  private void prepareRepoFromScript(String scriptName) throws IOException, InterruptedException {
    var process = Runtime.getRuntime()
        .exec("/bin/bash ${parentDir.resolve(scriptName).toAbsolutePath()} ${parentDir.toAbsolutePath()}");
    var completed = process.waitFor(5, TimeUnit.SECONDS);

    // In case of non 0 exit code print stdout and stderr
    if (process.exitValue() != 0) {
      System.out.println(new String(process.getInputStream().readAllBytes()));
      System.err.println(new String(process.getErrorStream().readAllBytes()));
    }

    Assert.assertTrue(completed);
    Assert.assertEquals(0, process.exitValue());
  }

  @After
  public void cleanup() throws IOException {
    Files.walk(parentDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
  }

}
