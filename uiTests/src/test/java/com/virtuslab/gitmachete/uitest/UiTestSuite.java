package com.virtuslab.gitmachete.uitest;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.intellij.remoterobot.RemoteRobot;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedTestSuite;

public class UiTestSuite extends BaseGitRepositoryBackedTestSuite {

  private final static String script = loadScript();

  private RemoteRobot remoteRobot;

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
    init(SETUP_WITH_SINGLE_REMOTE);

    runJs("configureIde()");
    runJs("closeOpenedProjects()");
    runJs("openProject('${repositoryMainDir}')");

    int graphTableRowCount = callJs("openTabAndReturnRowCount(soleOpenedProject())");

    // There should be exactly 6 rows in the graph table, since there are 6 branches in machete file,
    // as set up via `init(SETUP_WITH_SINGLE_REMOTE)`.
    Assert.assertEquals(6, graphTableRowCount);

    runJs("closeIde()");
  }

  private void runJs(String statement) {
    remoteRobot.runJs(script + statement, /* runInEdt */ false);
  }

  private <T> T callJs(String expression) {
    return remoteRobot.callJs(script + expression, /* runInEdt */ false);
  }
}
