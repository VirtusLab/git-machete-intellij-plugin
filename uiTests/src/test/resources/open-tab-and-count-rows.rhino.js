
importClass(java.lang.Runnable);
importClass(java.lang.System);
importClass(java.lang.Thread);

importClass  (com.intellij.ide.impl.ProjectUtil);
importPackage(com.intellij.openapi.application);
importPackage(com.intellij.openapi.wm);
importPackage(com.intellij.remoterobot.fixtures);
importClass  (com.intellij.remoterobot.search.locators.Locators);
importClass  (com.intellij.ui.GuiUtils);
importPackage(com.intellij.ui.content);
importPackage(org.assertj.swing.core.matcher)

function sleep() {
  Thread.sleep(1000);
}

// Do not run in EDT.
function openTabAndReturnRowCount(repositoryMainDir) {
  let project = ProjectUtil.openOrImport(repositoryMainDir, /* projectToClose */ null, /* forceOpenInNewFrame */ false);

  let finder = robot.finder();
  let tipDialogs;
  do {
    tipDialogs = finder.findAll(DialogMatcher.withTitle('Tip of the Day'));
    System.out.println("tipDialogs = " + tipDialogs);
    sleep();
  } while (tipDialogs.isEmpty());

  let closeButton = finder.find(tipDialogs.toArray()[0], JButtonMatcher.withText('Close'));
  robot.click(closeButton);


  let toolWindowManager = ToolWindowManager.getInstance(project);
  let toolWindow;
  do {
    toolWindow = toolWindowManager.getToolWindow(ToolWindowId.VCS);
    System.out.println("tool window present = " + (toolWindow != null));
    sleep();
  } while (toolWindow == null);

  // The test is (obviously) not run on UI thread,
  // so `runOrInvokeAndWait` really means `enqueue UI thread and wait until complete`.
  GuiUtils.runOrInvokeAndWait(function () {
    toolWindow.activate(function () {
    });
  });

  let graphTable;
  GuiUtils.runOrInvokeAndWait(function () {
    let contentManager = toolWindow.getContentManager();
    let tab = contentManager.findContent('Git Machete');
    contentManager.setSelectedContent(tab);
    let panel = tab.getComponent();
    graphTable = panel.getGraphTable();
    graphTable.queueRepositoryUpdateAndModelRefresh();
  });

  let graphTableRowCount = 0;
  do {
    graphTableRowCount = graphTable.getModel().getRowCount();
    System.out.println("graph table row count = " + graphTableRowCount);
    sleep();
  } while (graphTableRowCount === 0);
  return graphTableRowCount;
}

// Do not run in EDT.
function closeIde() {
  // By closing the project first and only then closing the entire IDE (aka Application)
  // we don't need to deal with IDE closing confirmation dialog.
  GuiUtils.runOrInvokeAndWait(function () {
    let project = ProjectUtil.getOpenProjects()[0];
    ProjectUtil.closeAndDispose(project);
  });

  // We can't just run `app.exit()` synchronously since then the Robot client would never receive a reply from the Robot server,
  // and the entire test would end up in a ConnectException.
  GuiUtils.invokeLaterIfNeeded(function () {
    // Just to give enough time for the Robot server to send back the response to the client.
    sleep();
    ApplicationManager.getApplication().exit();
  }, ModalityState.NON_MODAL);
}
