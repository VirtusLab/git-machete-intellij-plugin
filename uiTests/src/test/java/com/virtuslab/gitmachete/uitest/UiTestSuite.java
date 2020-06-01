package com.virtuslab.gitmachete.uitest;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.intellij.remoterobot.RemoteRobot;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;

import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedTestSuite;

public class UiTestSuite extends BaseGitRepositoryBackedTestSuite {

  @Test
  @SneakyThrows
  public void openTabAndCountRows() {
    init(SETUP_WITH_SINGLE_REMOTE);

    var script = new String(
        Files.readAllBytes(
            Paths.get(
                getClass().getResource("/open-tab-and-count-rows.rhino.js").toURI())));

    RemoteRobot remoteRobot = new RemoteRobot("http://127.0.0.1:8080");
    int graphTableRowCount = remoteRobot.callJs(script + "openTabAndReturnRowCount('" + repositoryMainDir + "')",
        /* runInEdt */ false);

    // There should be exactly 6 rows in the graph table, since there are 6 branches in machete file,
    // as set up via `init(SETUP_WITH_SINGLE_REMOTE)`.
    Assert.assertEquals(6, graphTableRowCount);

    remoteRobot.runJs(script + "closeIde();", /* runInEdt */ false);
  }
}
