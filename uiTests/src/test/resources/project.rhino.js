
importClass(java.lang.System);
importClass(java.util.ArrayList);
importClass(java.util.stream.Collectors);

importClass(com.intellij.ide.DataManager);
importClass(com.intellij.openapi.actionSystem.ActionManager);
importClass(com.intellij.openapi.actionSystem.AnActionEvent);
importClass(com.intellij.openapi.actionSystem.DataContext);
importClass(com.intellij.openapi.actionSystem.Presentation);
importClass(com.intellij.openapi.util.Key);
importClass(com.intellij.openapi.wm.ToolWindowId);
importClass(com.intellij.openapi.wm.ToolWindowManager);
importClass(com.intellij.ui.GuiUtils);

// Do not run any of the methods on the UI thread.
function Project(underlyingProject) {

  this.openTab = function () {
    const toolWindowManager = ToolWindowManager.getInstance(underlyingProject);
    let toolWindow;
    do {
      toolWindow = toolWindowManager.getToolWindow(ToolWindowId.VCS);
      sleep();
    } while (toolWindow === null);

    // The test is (obviously) not run on the UI thread,
    // so `runOrInvokeAndWait` really means `enqueue onto the UI thread and wait until complete`.
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
    const graphTable = getGraphTable();
    refreshModelAndWaitUntilDone(graphTable);
    return graphTable.getModel().getRowCount();
  };


  const ACTION_PLACE_TOOLBAR = 'GitMacheteToolbar';
  const ACTION_PLACE_CONTEXT_MENU = 'GitMacheteContextMenu';

  const createDataContextForSelectedBranch = function (branchName) {
    return new DataContext({
      getData: function(dataId) {
        if (dataId.equals('SELECTED_BRANCH_NAME')) return branchName;
        if (dataId.equals('project')) return underlyingProject;
        return getGraphTable().getData(dataId);
      }
    });
  };

  const invokeActionAndWait = function(actionName, actionPlace, dataContext) {
    const actionManager = ActionManager.getInstance();
    const action = actionManager.getAction(actionName);
    const actionEvent = AnActionEvent.createFromDataContext(actionPlace, new Presentation(), dataContext);

    GuiUtils.runOrInvokeAndWait(function () {
      action.actionPerformed(actionEvent);
    });
  };

  this.toggleListingCommits = function () {
    const dataContext = DataManager.getInstance().getDataContext(getGraphTable());
    invokeActionAndWait('GitMachete.ToggleListingCommitsAction', ACTION_PLACE_TOOLBAR, dataContext);
  };

  this.checkoutBranch = function (branchName) {
    const dataContext = createDataContextForSelectedBranch(branchName);
    invokeActionAndWait('GitMachete.CheckoutSelectedBranchAction', ACTION_PLACE_CONTEXT_MENU, dataContext);
  };

  this.pullBranch = function (branchName) {
    const dataContext = createDataContextForSelectedBranch(branchName);
    invokeActionAndWait('GitMachete.PullSelectedBranchAction', ACTION_PLACE_CONTEXT_MENU, dataContext);
  };

  const getSelectedGitRepository = function() {
    // We can't rely on the Rhino's default classloader
    // since it operates in the context of the Remote Robot plugin, not our plugin.
    const providerClass = pluginClassLoader.loadClass('com.virtuslab.gitmachete.frontend.ui.providerservice.SelectedGitRepositoryProvider');
    const provider = underlyingProject.getService(providerClass);
    return provider.getSelectedGitRepository().get();
  };

  this.getDiffOfWorkingTreeToHead = function () {
    // Since Remote Robot plugin does not declare <depends> on git4idea,
    // git4idea's classes won't be visible from Remote Robot plugin's classloader in the runtime.
    // But since our plugin <depends> on git4idea, we can access git4idea's classes from our classloader.
    const gitRepositoryClass = pluginClassLoader.loadClass('git4idea.repo.GitRepository');
    const gitChangeUtilsClass = pluginClassLoader.loadClass('git4idea.changes.GitChangeUtils');
    const getDiffWithWorkingTree = gitChangeUtilsClass.getMethod('getDiffWithWorkingTree', gitRepositoryClass, java.lang.String, java.lang.Boolean.TYPE);

    const diff = getDiffWithWorkingTree.invoke(/* (static method) */ null, getSelectedGitRepository(), 'HEAD', /* detectRenames */ false);
    return diff.stream().map(function (change) {
      // We can't return the com.intellij.openapi.vcs.changes.Change objects
      // since they won't properly serialize for the transfer from Robot Remote plugin (in the IDE) back to the client (UI tests).
      return change.toString();
    }).collect(Collectors.toList());
  };

  this.getHashOfCommitPointedByBranch = function (branchName) {
    const gitRepository = getSelectedGitRepository();
    const branchesCollection = gitRepository.getBranches();
    const branch = branchesCollection.findBranchByName(branchName);
    if (branch === null) return null;
    const hash = branchesCollection.getHash(branch);
    if (hash === null) return null;
    return hash.asString();
  };
}
