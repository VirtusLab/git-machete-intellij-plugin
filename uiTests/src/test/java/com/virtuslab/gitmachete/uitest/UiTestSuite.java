package com.virtuslab.gitmachete.uitest;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.intellij.remoterobot.RemoteRobot;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedIntegrationTestSuite;

public class UiTestSuite extends BaseGitRepositoryBackedIntegrationTestSuite {

  private final static String script = loadScript();

  private RemoteRobot remoteRobot;

  public UiTestSuite() {
    super(SETUP_WITH_SINGLE_REMOTE);
  }

  @SneakyThrows
  private static String loadScript() {
    return new String(
        Files.readAllBytes(
            Paths.get(
                UiTestSuite.class.getResource("/scenarios-and-utils.rhino.js").toURI())));
  }

  @Before
  public void connectToIde() {
    remoteRobot = new RemoteRobot("http://127.0.0.1:8080");
  }

  @Test
  @SneakyThrows
  public void openTabAndCountRows() {
    runJs("configureIde()");
    runJs("closeOpenedProjects()");
    runJs("openProject('" + repositoryMainDir + "')");
    runJs("openTab(soleOpenedProject())");

    int branchRowsCount = callJs("refreshModelAndGetRowCount(soleOpenedProject())");
    // There should be exactly 6 rows in the graph table, since there are 6 branches in machete file,
    // as set up via `super(SETUP_WITH_SINGLE_REMOTE)`.
    Assert.assertEquals(6, branchRowsCount);

    runJs("toggleListingCommits(soleOpenedProject())");
    int branchAndCommitRowsCount = callJs("refreshModelAndGetRowCount(soleOpenedProject())");
    // 6 branch rows + 7 commit rows
    Assert.assertEquals(13, branchAndCommitRowsCount);
  }

  @After
  public void closeIde() {
    runJs("closeIde()");
  }

  private void runJs(String statement) {
    remoteRobot.runJs(script + statement, /* runInEdt */ false);
  }

  private <T extends Serializable> T callJs(String expression) {
    return remoteRobot.callJs(script + expression, /* runInEdt */ false);
  }
}
