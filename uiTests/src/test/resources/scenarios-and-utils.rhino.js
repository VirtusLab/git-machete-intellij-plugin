
importClass(java.lang.Runnable);
importClass(java.lang.System);
importClass(java.lang.Thread);

importClass(com.intellij.ide.DataManager);
importClass(com.intellij.ide.GeneralSettings);
importClass(com.intellij.ide.impl.ProjectUtil);
importClass(com.intellij.openapi.actionSystem.ActionManager);
importClass(com.intellij.openapi.actionSystem.AnActionEvent);
importClass(com.intellij.openapi.actionSystem.Presentation);
importClass(com.intellij.openapi.application.ApplicationManager);
importClass(com.intellij.openapi.application.ModalityState);
importClass(com.intellij.openapi.wm.ToolWindowId);
importClass(com.intellij.openapi.wm.ToolWindowManager);
importClass(com.intellij.ui.GuiUtils);


// Do not run in EDT.
function sleep() {
  Thread.sleep(500);
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
      System.out.println('project to close = ' + project.toString());
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
function openTab(project) {
  const toolWindowManager = ToolWindowManager.getInstance(project);
  let toolWindow;
  do {
    toolWindow = toolWindowManager.getToolWindow(ToolWindowId.VCS);
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
}

function _getGraphTable(project) {
  const toolWindowManager = ToolWindowManager.getInstance(project);
  const toolWindow = toolWindowManager.getToolWindow(ToolWindowId.VCS);
  const contentManager = toolWindow.getContentManager();
  const tab = contentManager.findContent('Git Machete');
  const panel = tab.getComponent();
  return panel.getGraphTable();
}

function _refreshModelAndWaitUntilDone(graphTable) {
  let refreshDone = false;
  graphTable.queueRepositoryUpdateAndModelRefresh(/* doOnUIThreadWhenReady */ function() {
    refreshDone = true;
  });
  do {
    sleep();
  } while (!refreshDone);
}

// Do not run in EDT.
// Assumes that Git Machete tab is opened.
function refreshModelAndGetRowCount(project) {
  const graphTable = _getGraphTable(project);
  _refreshModelAndWaitUntilDone(graphTable);
  return graphTable.getModel().getRowCount();
}

// Do not run in EDT.
function toggleListingCommits(project) {
  const actionManager = ActionManager.getInstance();
  const action = actionManager.getAction('GitMachete.ToggleListingCommitsAction');

  const graphTable = _getGraphTable(project);
  const dataContext = DataManager.getInstance().getDataContext(graphTable);
  const actionEvent = AnActionEvent.createFromDataContext('GitMacheteContextMenu', new Presentation(), dataContext);

  GuiUtils.runOrInvokeAndWait(function () {
    action.actionPerformed(actionEvent);
  });
}
