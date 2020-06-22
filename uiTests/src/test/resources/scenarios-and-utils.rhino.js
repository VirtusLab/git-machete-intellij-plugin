
importClass(java.lang.Runnable);
importClass(java.lang.System);
importClass(java.lang.Thread);

importClass(com.intellij.ide.GeneralSettings);
importClass(com.intellij.ide.impl.ProjectUtil);
importClass(com.intellij.openapi.application.ApplicationManager);
importClass(com.intellij.openapi.application.ModalityState);
importClass(com.intellij.openapi.wm.ToolWindowId);
importClass(com.intellij.openapi.wm.ToolWindowManager);
importClass(com.intellij.ui.GuiUtils);

importClass(com.virtuslab.gitmachete.frontend.ui.providerservice.GraphTableProvider);

// Do not run in EDT.
function sleep() {
  Thread.sleep(1000);
}

// Do not run in EDT.
function configureIde() {
  const settings = GeneralSettings.getInstance();
  settings.setConfirmExit(false);
  settings.setShowTipsOnStartup(false);
}

// Do not run in EDT.
function openProject(path) {
  ProjectUtil.openOrImport(path, /* projectToClose */ null, /* forceOpenInNewFrame */ false);
}

// Do not run in EDT.
function soleOpenedProject() {
  return ProjectUtil.getOpenProjects()[0];
}

// Do not run in EDT.
function closeOpenedProjects() {
  ProjectUtil.getOpenProjects().forEach(function (project) {
    GuiUtils.runOrInvokeAndWait(function () {
      System.out.println("project to close = " + project.toString());
      ProjectUtil.closeAndDispose(project);
    });
  });
}

// Do not run in EDT.
function closeIde() {
  // We can't just run `app.exit()` synchronously since then the Robot client would never receive a reply from the Robot server,
  // and the entire test would end up in a ConnectException.
  GuiUtils.invokeLaterIfNeeded(function () {
    // Just to give enough time for the Robot server to send back the response to the client.
    sleep();
    ApplicationManager.getApplication().exit();
  }, ModalityState.NON_MODAL);
}

// Do not run in EDT.
function openTabAndReturnRowCount(project) {

  const toolWindowManager = ToolWindowManager.getInstance(project);
  let toolWindow;
  do {
    toolWindow = toolWindowManager.getToolWindow(ToolWindowId.VCS);
    System.out.println("tool window present = " + (toolWindow != null));
    sleep();
  } while (toolWindow == null);

  // The test is (obviously) not run on UI thread,
  // so `runOrInvokeAndWait` really means `enqueue onto UI thread and wait until complete`.
  GuiUtils.runOrInvokeAndWait(function () {
    toolWindow.activate(function () {});
  });

  GuiUtils.runOrInvokeAndWait(function () {
    const contentManager = toolWindow.getContentManager();
    const tab = contentManager.findContent('Git Machete');
    contentManager.setSelectedContent(tab);
  });
  const graphTable = project.getService(GraphTableProvider).getGraphTable();
  graphTable.queueRepositoryUpdateAndModelRefresh();
  let graphTableRowCount;
  do {
    graphTableRowCount = graphTable.getModel().getRowCount();
    System.out.println("graph table row count = " + graphTableRowCount);
    sleep();
  } while (graphTableRowCount === 0);
  return graphTableRowCount;
}
