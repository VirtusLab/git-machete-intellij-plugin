
importClass(java.lang.System);
importClass(java.util.stream.Collectors);

importClass(com.intellij.ide.util.PropertiesComponent);
importClass(com.intellij.openapi.actionSystem.ActionManager);
importClass(com.intellij.openapi.actionSystem.ActionPlaces);
importClass(com.intellij.openapi.actionSystem.AnActionEvent);
importClass(com.intellij.openapi.actionSystem.DataContext);
importClass(com.intellij.openapi.actionSystem.Presentation);
importClass(com.intellij.openapi.application.ModalityState);
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

  // Assumes that Git Machete tab is opened.
  this.refreshModelAndGetRowCount = function () {
    const graphTable = getGraphTable();

    let refreshDone = false;
    graphTable.queueRepositoryUpdateAndModelRefresh(/* doOnUIThreadWhenReady */ function () {
      refreshDone = true;
    });
    do {
      sleep();
    } while (!refreshDone);

    return graphTable.getModel().getRowCount();
  };


  const ACTION_PLACE_TOOLBAR = 'GitMacheteToolbar';
  const ACTION_PLACE_CONTEXT_MENU = 'GitMacheteContextMenu';
  const RESET_INFO_SHOWN = 'git-machete.reset.info.shown';

  const getActionByName = function (actionName) {
    return ActionManager.getInstance().getAction(actionName);
  };

  const createActionEvent = function (actionPlace, data) {
    const dataContext = new DataContext({
      getData: function (dataId) {
        if (dataId in data) return data[dataId];
        if (dataId.equals('project')) return underlyingProject;
        return getGraphTable().getData(dataId);
      }
    });
    return AnActionEvent.createFromDataContext(actionPlace, new Presentation(), dataContext);
  };

  const invokeActionAsync = function (actionName, actionPlace, data) {
    const action = getActionByName(actionName);
    const actionEvent = createActionEvent(actionPlace, data);

    GuiUtils.invokeLaterIfNeeded(function () {
      action.actionPerformed(actionEvent);
    }, ModalityState.NON_MODAL);
  };

  const invokeActionAndWait = function (actionName, actionPlace, data) {
    const action = getActionByName(actionName);
    const actionEvent = createActionEvent(actionPlace, data);

    GuiUtils.runOrInvokeAndWait(function () {
      action.actionPerformed(actionEvent);
    });
  };

  this.discoverBranchLayout = function () {
    invokeActionAsync('GitMachete.DiscoverAction', ActionPlaces.ACTION_SEARCH, {});

    const saveButton = robot.finder().find(function (component) {
      return 'javax.swing.JButton'.equals(component.getClass().getName())
        && 'Save'.equals(component.getText());
    });
    robot.click(saveButton);
  };

  this.toggleListingCommits = function () {
    invokeActionAndWait('GitMachete.ToggleListingCommitsAction', ACTION_PLACE_TOOLBAR, {});
  };

  this.checkoutBranch = function (branchName) {
    invokeActionAndWait('GitMachete.CheckoutSelectedBranchAction', ACTION_PLACE_CONTEXT_MENU, { SELECTED_BRANCH_NAME: branchName });
  };

  this.fastForwardParentToMatchBranch = function (branchName) {
    invokeActionAndWait('GitMachete.FastForwardParentToMatchSelectedBranchAction', ACTION_PLACE_CONTEXT_MENU, { SELECTED_BRANCH_NAME: branchName });
  };

  this.fastForwardParentToMatchCurrentBranch = function () {
    invokeActionAndWait('GitMachete.FastForwardParentToMatchCurrentBranchAction', ACTION_PLACE_TOOLBAR, {});
  };

  this.pullBranch = function (branchName) {
    invokeActionAndWait('GitMachete.PullSelectedBranchFastForwardOnlyAction', ACTION_PLACE_CONTEXT_MENU, { SELECTED_BRANCH_NAME: branchName });
  };

  this.pullCurrentBranch = function () {
    invokeActionAndWait('GitMachete.PullCurrentBranchFastForwardOnlyAction', ACTION_PLACE_TOOLBAR, {});
  };

  this.resetBranchToRemote = function (branchName) {
    PropertiesComponent.getInstance().setValue(RESET_INFO_SHOWN, true);

    invokeActionAndWait('GitMachete.ResetSelectedBranchToRemoteAction', ACTION_PLACE_CONTEXT_MENU, { SELECTED_BRANCH_NAME: branchName });
  };

  this.resetCurrentBranchToRemote = function () {
    PropertiesComponent.getInstance().setValue(RESET_INFO_SHOWN, true);

    invokeActionAndWait('GitMachete.ResetCurrentBranchToRemoteAction', ACTION_PLACE_TOOLBAR, {});
  };

  const getSelectedGitRepository = function () {
    // We can't rely on the Rhino's default classloader
    // since it operates in the context of the Remote Robot plugin, not our plugin.
    const providerClass = pluginClassLoader.loadClass('com.virtuslab.gitmachete.frontend.ui.providerservice.SelectedGitRepositoryProvider');
    const provider = underlyingProject.getService(providerClass);
    const gitRepository = provider.getSelectedGitRepository().get();
    // Let's make sure the data stored in the GitRepository object is up to date with the underlying .git/ folder.
    gitRepository.update();
    return gitRepository;
  };

  this.getCurrentBranchName = function () {
    const gitRepository = getSelectedGitRepository();
    return gitRepository.getCurrentBranch().getName();
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
