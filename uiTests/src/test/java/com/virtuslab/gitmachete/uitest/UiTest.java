package com.virtuslab.gitmachete.uitest;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.ContainerFixture;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;

import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedTest;

public class UiTest extends BaseGitRepositoryBackedTest {

  @SneakyThrows
  private static void sleep() {
    Thread.sleep(1000);
  }

  @Test
  public void openTabAndCountRows() {
    init(SETUP_WITH_SINGLE_REMOTE);

    String imports = "importClass(com.intellij.ide.impl.ProjectUtil);" +
        "importPackage(com.intellij.openapi.application);" +
        "importPackage(com.intellij.openapi.wm);" +
        "importPackage(com.intellij.ui.content);" +
        "importClass(java.lang.Runnable);";

    RemoteRobot remoteRobot = new RemoteRobot("http://127.0.0.1:8080");
    remoteRobot.runJs(
        imports +
            "ProjectUtil.openOrImport('" + repositoryMainDir +
            "', /* projectToClose */ null, /* forceOpenInNewFrame */ false);",
        /* runInEdt */ false);

    java.util.List<ContainerFixture> tipDialog;
    do {
      tipDialog = remoteRobot.findAll(ContainerFixture.class,
          byXpath("//div[@accessiblename='Tip of the Day' and @class='MyDialog']"));
      System.out.println("tip dialog = " + tipDialog);
      sleep();
    } while (tipDialog.isEmpty());

    var closeButton = tipDialog.get(0).find(ComponentFixture.class,
        byXpath("//div[@accessiblename='Close' and @class='JButton' and @text='Close']"));
    closeButton.click();

    var toolWindowPresent = false;
    do {
      toolWindowPresent = remoteRobot.callJs(
          imports +
              "var toolWindowManager = ToolWindowManager.getInstance(ProjectUtil.getOpenProjects()[0]);" +
              "var toolWindow = toolWindowManager.getToolWindow(ToolWindowId.VCS);" +
              "toolWindow != null",
          /* runInEdt */ false);
      System.out.println("tool window present = " + toolWindowPresent);
      sleep();
    } while (!toolWindowPresent);

    remoteRobot.runJs(
        imports +
            "var toolWindowManager = ToolWindowManager.getInstance(ProjectUtil.getOpenProjects()[0]);" +
            "var toolWindow = toolWindowManager.getToolWindow(ToolWindowId.VCS);" +
            "var runnable = new Runnable({ run: function () { } });" +
            "toolWindow.activate(runnable);",
        /* runInEdt */ true);

    remoteRobot.runJs(
        imports +
            "var toolWindowManager = ToolWindowManager.getInstance(ProjectUtil.getOpenProjects()[0]);" +
            "var toolWindow = toolWindowManager.getToolWindow(ToolWindowId.VCS);" +
            "var contentManager = toolWindow.getContentManager();" +
            "var tab = contentManager.findContent('Git Machete');" +
            "contentManager.setSelectedContent(tab);" +
            "var panel = tab.getComponent();" +
            "var graphTable = panel.getGraphTable();" +
            "graphTable.queueRepositoryUpdateAndModelRefresh();",
        /* runInEdt */ true);

    var graphTableRowCount = 0;
    do {
      graphTableRowCount = remoteRobot.callJs(
          imports +
              "var toolWindowManager = ToolWindowManager.getInstance(ProjectUtil.getOpenProjects()[0]);" +
              "var toolWindow = toolWindowManager.getToolWindow(ToolWindowId.VCS);" +
              "var contentManager = toolWindow.getContentManager();" +
              "var tab = contentManager.findContent('Git Machete');" +
              "var panel = tab.getComponent();" +
              "var graphTable = panel.getGraphTable();" +
              "graphTable.getModel().getRowCount()",
          /* runInEdt */ true);
      System.out.println("graph table row count = " + graphTableRowCount);
      sleep();
    } while (graphTableRowCount == 0);

    // There should be exactly 6 rows in the graph table, since there are 6 branches in machete file,
    // as set up via `init(SETUP_WITH_SINGLE_REMOTE)`.
    Assert.assertEquals(6, graphTableRowCount);

    remoteRobot.runJs(
        imports +
            "var app = ApplicationManager.getApplication();" +
            "var project = ProjectUtil.getOpenProjects()[0];" +
            // By closing the project first and only then closing the entire IDE (aka Application)
            // we don't need to deal with IDE closing confirmation dialog.
            "var runnable = new Runnable({ run: function () { ProjectUtil.closeAndDispose(project); } });" +
            "app.invokeLater(runnable, ModalityState.NON_MODAL);",
        /* runInEdt */ true);

    remoteRobot.runJs(
        imports +
            "var app = ApplicationManager.getApplication();" +
            // We can't just run `app.exit()` synchronously since then we'd never receive a reply from the Robot Server,
            // and the entire test would end up in a ConnectException.
            "var runnable = new Runnable({ run: function () { app.exit(); } });" +
            "app.invokeLater(runnable, ModalityState.NON_MODAL);",
        /* runInEdt */ true);
  }
}
