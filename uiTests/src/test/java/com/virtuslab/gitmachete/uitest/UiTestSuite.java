package com.virtuslab.gitmachete.uitest;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import com.intellij.remoterobot.RemoteRobot;
import io.vavr.collection.List;
import lombok.SneakyThrows;
import org.intellij.lang.annotations.Language;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedIntegrationTestSuite;

public class UiTestSuite extends BaseGitRepositoryBackedIntegrationTestSuite {

  private final static String rhinoCodebase = List.of("common", "ide", "project").map(UiTestSuite::loadScript).mkString();

  private static RemoteRobot remoteRobot;

  @SneakyThrows
  private static String loadScript(String baseName) {
    return new String(
        Files.readAllBytes(
            Paths.get(
                UiTestSuite.class.getResource("/" + baseName + ".rhino.js").toURI())));
  }

  @BeforeClass
  public static void connectToIde() {
    remoteRobot = new RemoteRobot("http://127.0.0.1:8080");
    runJs("ide.configure(/* enableDebugLog */ false)");
    // In case there are any leftovers from the previous runs of IDE for UI tests.
    runJs("ide.closeOpenedProjects()");
  }

  public UiTestSuite() {
    super(SETUP_WITH_SINGLE_REMOTE);
  }

  @Before
  public void openProjectAndAwaitIdle() {
    runJs("ide.openProject('" + repositoryMainDir + "').openTab()");
    awaitIdle();
  }

  // Note that due to how Remote Robot operates,
  // each `runJs`/`callJs` invocation is executed in a fresh JavaScript environment.
  // Thus, we can't store any state (like a reference to the project,
  // i.e. the value of `ide.soleOpenedProject()`) between the invocations.

  @Test
  public void ensureCorrectGraphTableRowCounts() {
    int branchRowsCount = callJs("ide.soleOpenedProject().refreshModelAndGetRowCount()");
    // There should be exactly 6 rows in the graph table, since there are 6 branches in machete file,
    // as set up via `super(SETUP_WITH_SINGLE_REMOTE)`.
    Assert.assertEquals(6, branchRowsCount);

    runJs("ide.soleOpenedProject().toggleListingCommits()");
    int branchAndCommitRowsCount = callJs("ide.soleOpenedProject().refreshModelAndGetRowCount()");
    // 6 branch rows + 7 commit rows
    Assert.assertEquals(13, branchAndCommitRowsCount);
  }

  @SneakyThrows
  @Test
  public void fastForwardParentOfBranchAndEnsureCorrectRepositoryState() {
    runJs("ide.soleOpenedProject().fastForwardParentToMatchBranch('hotfix/add-trigger')");
    awaitIdle();

    String parentBranchHash = callJs("ide.soleOpenedProject().getHashOfCommitPointedByBranch('master')");
    String childBranchHash = callJs("ide.soleOpenedProject().getHashOfCommitPointedByBranch('hotfix/add-trigger')");
    Assert.assertEquals(childBranchHash, parentBranchHash);

    ArrayList<String> changes = callJs("ide.soleOpenedProject().getDiffOfWorkingTreeToHead()");
    Assert.assertEquals(new ArrayList<>(), changes);
  }

  @Test
  public void pullCurrentBranchAndEnsureCorrectRepositoryState() {
    runJs("ide.soleOpenedProject().checkoutBranch('allow-ownership-link')");
    awaitIdle();
    runJs("ide.soleOpenedProject().pullBranch('allow-ownership-link')");
    awaitIdle();

    ArrayList<String> changes = callJs("ide.soleOpenedProject().getDiffOfWorkingTreeToHead()");
    Assert.assertEquals(new ArrayList<>(), changes);
  }

  @After
  public void awaitIdleAndCloseProject() {
    awaitIdle();
    runJs("ide.closeOpenedProjects()");
  }

  // Note that since this suite is not responsible for opening the IDE,
  // it is not going to close the IDE at the end, either.

  @SneakyThrows
  private static void awaitIdle() {
    ArrayList<String> indicators = callJs("ide.getProgressIndicators()");
    // This loop could theoretically be performed totally on the IDE side (in JS/Rhino code),
    // but this would lead to spurious timeouts when e.g. the indexing task happens to take too long.
    while (!indicators.isEmpty()) {
      System.out.println("Waiting for ${indicators.size()} task(s) to complete...");
      Thread.sleep(1000);
      indicators = callJs("ide.getProgressIndicators()");
    }
  }

  private static void runJs(@Language("JS") String statement) {
    remoteRobot.runJs(rhinoCodebase + statement, /* runInEdt */ false);
  }

  private static <T extends Serializable> T callJs(@Language("JS") String expression) {
    return remoteRobot.callJs(rhinoCodebase + expression, /* runInEdt */ false);
  }
}
