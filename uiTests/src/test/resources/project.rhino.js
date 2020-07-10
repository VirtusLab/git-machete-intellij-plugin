
importClass(com.intellij.ide.DataManager);
importClass(com.intellij.openapi.actionSystem.ActionManager);
importClass(com.intellij.openapi.actionSystem.AnActionEvent);
importClass(com.intellij.openapi.actionSystem.Presentation);
importClass(com.intellij.openapi.wm.ToolWindowId);
importClass(com.intellij.openapi.wm.ToolWindowManager);
importClass(com.intellij.ui.GuiUtils);

// Do not run any of the methods in EDT.
function Project(underlyingProject) {
  this.openTab = function () {
    const toolWindowManager = ToolWindowManager.getInstance(underlyingProject);
    let toolWindow;
    do {
      toolWindow = toolWindowManager.getToolWindow(ToolWindowId.VCS);
      sleep();
    } while (toolWindow == null);

    // The test is (obviously) not run on UI thread,
    // so `runOrInvokeAndWait` really means `enqueue onto UI thread and wait until complete`.
    GuiUtils.runOrInvokeAndWait(function () {
      toolWindow.activate(function () {});
      const contentManager = toolWindow.getContentManager();
      const tab = contentManager.findContent('Git Machete');
      contentManager.setSelectedContent(tab);
    });
  };

  const getGraphTable = function () {
    const toolWindowManager = ToolWindowManager.getInstance(underlyingProject);
    const toolWindow = toolWindowManager.getToolWindow(ToolWindowId.VCS);
    const contentManager = toolWindow.getContentManager();
    const tab = contentManager.findContent('Git Machete');
    const panel = tab.getComponent();
    return panel.getGraphTable();
  };

  const refreshModelAndWaitUntilDone = function (graphTable) {
    let refreshDone = false;
    graphTable.queueRepositoryUpdateAndModelRefresh(/* doOnUIThreadWhenReady */ function () {
      refreshDone = true;
    });
    do {
      sleep();
    } while (!refreshDone);
  };

  // Assumes that Git Machete tab is opened.
  this.refreshModelAndGetRowCount = function () {
    const graphTable = getGraphTable(underlyingProject);
    refreshModelAndWaitUntilDone(graphTable);
    return graphTable.getModel().getRowCount();
  };

  this.toggleListingCommits = function () {
    const actionManager = ActionManager.getInstance();
    const action = actionManager.getAction('GitMachete.ToggleListingCommitsAction');

    const graphTable = getGraphTable(underlyingProject);
    const dataContext = DataManager.getInstance().getDataContext(graphTable);
    const actionEvent = AnActionEvent.createFromDataContext('GitMacheteContextMenu', new Presentation(), dataContext);

    GuiUtils.runOrInvokeAndWait(function () {
      action.actionPerformed(actionEvent);
    });
  };
}
