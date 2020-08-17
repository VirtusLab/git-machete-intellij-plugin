package com.virtuslab.gitmachete.uitest;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

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

public class UITestSuite extends BaseGitRepositoryBackedIntegrationTestSuite {

  private final static String rhinoCodebase = List.of("common", "ide", "project").map(UITestSuite::loadScript).mkString();

  private static RemoteRobot remoteRobot;

  @SneakyThrows
  private static String loadScript(String baseName) {
    return new String(
        Files.readAllBytes(
            Paths.get(
                UITestSuite.class.getResource("/" + baseName + ".rhino.js").toURI())));
  }

  @BeforeClass
  public static void connectToIdeAndCloseProjects() {
    remoteRobot = new RemoteRobot("http://127.0.0.1:8080");
    runJs("ide.configure(/* enableDebugLog */ false)");

    // Let's close the opened projects first - there may be leftovers from the previous runs of IDE for UI tests.
    // First, let's make sure IDE actually initialized these projects - otherwise, we might end up with race conditions:
    //   'Already disposed: Project (name=machete-sandbox, containerState=DISPOSE_COMPLETED, ...)'
    awaitIdle();
    runJs("ide.closeOpenedProjects()");
  }

  public UITestSuite() {
    super(SETUP_WITH_SINGLE_REMOTE);
  }

  @Before
  public void openProjectAndAwaitIdle() {
    runJs("ide.openProject('" + repositoryMainDir + "').openTab()");
    awaitIdle();
  }

  // Note that due to how Remote Robot operates,
  // each `runJs`/`callJs` invocation is executed in a fresh JavaScript environment.
  // Thus, we can't store any state between the invocations.

  @Test
  public void toggleListingCommits() {
    int branchRowsCount = callJs("project.refreshModelAndGetRowCount()");
    // There should be exactly 6 rows in the graph table, since there are 6 branches in machete file,
    // as set up via `super(SETUP_WITH_SINGLE_REMOTE)`.
    Assert.assertEquals(6, branchRowsCount);

    runJs("project.toggleListingCommits()");
    int branchAndCommitRowsCount = callJs("project.refreshModelAndGetRowCount()");
    // 6 branch rows + 7 commit rows
    Assert.assertEquals(13, branchAndCommitRowsCount);
  }

  @Test
  public void skipNonExistentBranches() {
    overwriteMacheteFile(
        "develop",
        "  non-existent",
        "    allow-ownership-link",
        "      build-chain",
        "    non-existent-leaf",
        "  call-ws",
        "non-existent-root",
        "master",
        "  hotfix/add-trigger");
    int branchRowsCount = callJs("project.refreshModelAndGetRowCount()");
    // There should be exactly 6 rows in the graph table, since there are 6 existing branches in machete file;
    // non-existent branches should be skipped while causing no error (only a low-severity notification).
    Assert.assertEquals(6, branchRowsCount);
  }

  @Test
  public void fastForwardParentOfBranch_parentIsCurrentBranch() {
    runJs("project.checkoutBranch('master')");
    awaitIdle();
    // `master` is the parent of `hotfix/add-trigger`. Let's fast-forward `master` to match `hotfix/add-trigger`.
    runJs("project.fastForwardParentToMatchBranch('hotfix/add-trigger')");
    awaitIdle();

    String parentBranchHash = callJs("project.getHashOfCommitPointedByBranch('master')");
    String childBranchHash = callJs("project.getHashOfCommitPointedByBranch('hotfix/add-trigger')");
    Assert.assertEquals(childBranchHash, parentBranchHash);

    ArrayList<String> changes = callJs("project.getDiffOfWorkingTreeToHead()");
    Assert.assertEquals(new ArrayList<>(), changes);
  }

  @Test
  public void fastForwardParentOfBranch_childIsCurrentBranch() {
    runJs("project.checkoutBranch('hotfix/add-trigger')");
    awaitIdle();
    // `master` is the parent of `hotfix/add-trigger`. Let's fast-forward `master` to match `hotfix/add-trigger`.
    runJs("project.fastForwardParentToMatchBranch('hotfix/add-trigger')");
    awaitIdle();

    String parentBranchHash = callJs("project.getHashOfCommitPointedByBranch('master')");
    String childBranchHash = callJs("project.getHashOfCommitPointedByBranch('hotfix/add-trigger')");
    Assert.assertEquals(childBranchHash, parentBranchHash);

    ArrayList<String> changes = callJs("project.getDiffOfWorkingTreeToHead()");
    Assert.assertEquals(new ArrayList<>(), changes);
  }

  @Test
  public void pullCurrentBranch() {
    runJs("project.checkoutBranch('allow-ownership-link')");
    awaitIdle();
    // Remote tracking data is purposefully NOT set for this branch.
    // Our plugin should infer the remote tracking branch based on its name.
    runJs("project.pullBranch('allow-ownership-link')");
    awaitIdle();

    String localBranchHash = callJs("project.getHashOfCommitPointedByBranch('allow-ownership-link')");
    String remoteBranchHash = callJs("project.getHashOfCommitPointedByBranch('origin/allow-ownership-link')");
    Assert.assertEquals(remoteBranchHash, localBranchHash);

    ArrayList<String> changes = callJs("project.getDiffOfWorkingTreeToHead()");
    Assert.assertEquals(new ArrayList<>(), changes);
  }

  @Test
  public void pullNonCurrentBranch() {
    runJs("project.checkoutBranch('develop')");
    awaitIdle();
    // Remote tracking data is purposefully NOT set for this branch.
    // Our plugin should infer the remote tracking branch based on its name.
    runJs("project.pullBranch('allow-ownership-link')");
    awaitIdle();

    String localBranchHash = callJs("project.getHashOfCommitPointedByBranch('allow-ownership-link')");
    String remoteBranchHash = callJs("project.getHashOfCommitPointedByBranch('origin/allow-ownership-link')");
    Assert.assertEquals(remoteBranchHash, localBranchHash);

    ArrayList<String> changes = callJs("project.getDiffOfWorkingTreeToHead()");
    Assert.assertEquals(new ArrayList<>(), changes);
  }

  @Test
  public void resetCurrentBranchToRemote() {
    runJs("project.checkoutBranch('hotfix/add-trigger')");
    awaitIdle();
    runJs("project.resetBranchToRemote('hotfix/add-trigger')");
    awaitIdle();

    String localBranchHash = callJs("project.getHashOfCommitPointedByBranch('hotfix/add-trigger')");
    String remoteBranchHash = callJs("project.getHashOfCommitPointedByBranch('origin/hotfix/add-trigger')");
    Assert.assertEquals(remoteBranchHash, localBranchHash);

    ArrayList<String> changes = callJs("project.getDiffOfWorkingTreeToHead()");
    Assert.assertEquals(new ArrayList<>(), changes);

    String currentBranchName = callJs("project.getCurrentBranchName()");
    Assert.assertEquals("hotfix/add-trigger", currentBranchName);
  }

  @Test
  public void resetNonCurrentBranchToRemote() {
    runJs("project.checkoutBranch('develop')");
    awaitIdle();
    runJs("project.resetBranchToRemote('hotfix/add-trigger')");
    awaitIdle();

    String localBranchHash = callJs("project.getHashOfCommitPointedByBranch('hotfix/add-trigger')");
    String remoteBranchHash = callJs("project.getHashOfCommitPointedByBranch('origin/hotfix/add-trigger')");
    Assert.assertEquals(remoteBranchHash, localBranchHash);

    ArrayList<String> changes = callJs("project.getDiffOfWorkingTreeToHead()");
    Assert.assertEquals(new ArrayList<>(), changes);

    String currentBranchName = callJs("project.getCurrentBranchName()");
    // TODO (#523): `develop` should remain the current branch
    Assert.assertEquals("hotfix/add-trigger", currentBranchName);
  }

  @SneakyThrows
  private void overwriteMacheteFile(String... lines) {
    Files.write(repositoryGitDir.resolve("machete"), Arrays.asList(lines));
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
    var indicators = getProgressIndicators();
    // This loop could theoretically be performed totally on the IDE side (in JS/Rhino code),
    // but this would lead to spurious socket read timeouts when e.g. the indexing task happens to take too long.
    while (!indicators.isEmpty()) {
      System.out.println("Waiting for ${indicators.size()} task(s) to complete...");
      Thread.sleep(1000);
      indicators = getProgressIndicators();
    }

    long withinMillis = 2000;
    System.out.println("Waiting for ${withinMillis} milliseconds to ensure no new task appears...");
    if (newTaskAppeared(withinMillis, /* intervalMillis */ 250)) {
      awaitIdle();
    } else {
      System.out.println("OK, IDE is idle");
    }
  }

  private static ArrayList<String> getProgressIndicators() {
    return callJs("ide.getProgressIndicators()");
  }

  @SneakyThrows
  private static boolean newTaskAppeared(long withinMillis, long intervalMillis) {
    long passedTimeMillis = 0;
    while (passedTimeMillis < withinMillis) {
      Thread.sleep(intervalMillis);
      passedTimeMillis += intervalMillis;

      var indicators = getProgressIndicators();
      if (!indicators.isEmpty()) {
        System.out.println("${indicators.size()} new task(s) appeared...");
        return true;
      }
    }
    return false;
  }

  private static void runJs(@Language("JS") String statement) {
    remoteRobot.runJs(rhinoCodebase + statement, /* runInEdt */ false);
  }

  private static <T extends Serializable> T callJs(@Language("JS") String expression) {
    return remoteRobot.callJs(rhinoCodebase + expression, /* runInEdt */ false);
  }
}
