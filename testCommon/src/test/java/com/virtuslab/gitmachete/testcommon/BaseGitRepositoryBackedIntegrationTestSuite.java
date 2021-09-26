package com.virtuslab.gitmachete.testcommon;

import static com.virtuslab.gitmachete.testcommon.TestFileUtils.copyScriptFromResources;
import static com.virtuslab.gitmachete.testcommon.TestFileUtils.prepareRepoFromScript;

import java.nio.file.Files;
import java.nio.file.Path;

import lombok.SneakyThrows;

public abstract class BaseGitRepositoryBackedIntegrationTestSuite {

  protected final String scriptName;
  protected final Path parentDir;
  protected final Path repositoryMainDir;
  protected final Path repositoryGitDir;

  @SneakyThrows
  protected BaseGitRepositoryBackedIntegrationTestSuite(String scriptName) {
    this.scriptName = scriptName;
    parentDir = Files.createTempDirectory("machete-tests-");
    repositoryMainDir = parentDir.resolve("machete-sandbox");
    repositoryGitDir = repositoryMainDir.resolve(".git");

    copyScriptFromResources("common.sh", parentDir);
    copyScriptFromResources(scriptName, parentDir);
    prepareRepoFromScript(scriptName, parentDir);
  }
}
