importClass(java.lang.System);
importClass(java.lang.Thread);
importClass(java.util.stream.Collectors);
importClass(java.util.stream.IntStream);

importClass(com.intellij.ide.util.PropertiesComponent);
importClass(com.intellij.openapi.actionSystem.ActionManager);
importClass(com.intellij.openapi.actionSystem.ActionPlaces);
importClass(com.intellij.openapi.actionSystem.AnActionEvent);
importClass(com.intellij.openapi.actionSystem.DataContext);
importClass(com.intellij.openapi.actionSystem.Presentation);
importClass(com.intellij.openapi.actionSystem.impl.ActionButton);
importClass(com.intellij.openapi.actionSystem.impl.ActionToolbarImpl);
importClass(com.intellij.openapi.application.ModalityState);
importClass(com.intellij.openapi.wm.ToolWindowId);
importClass(com.intellij.openapi.wm.ToolWindowManager);
importClass(com.intellij.ui.GuiUtils);

importClass(org.assertj.swing.fixture.JComboBoxFixture);
importClass(org.assertj.swing.fixture.JPanelFixture);
importClass(org.assertj.swing.fixture.JTableFixture);
importClass(org.assertj.swing.data.TableCell);


let myClick = function (component, mouseButton) {
  robot.click(component, mouseButton);
};

// Do not run any of the methods on the UI thread.
function Project(underlyingProject) {

  const pluginId = PluginId.getId('com.virtuslab.git-machete');
  const pluginClassLoader = PluginManagerCore.getPlugin(pluginId).getPluginClassLoader();

  const prettyClick = function (component, mouseButton) {
    robot.moveMouse(component);
    // Wait for a while before clicking to allow the scenario spectator to see the button being clicked
    sleep(500);
    robot.click(component, mouseButton);
  };

  this.usePrettyClick = function () {
    myClick = prettyClick;
  };

  const sleep = function(ms) {
    if (ms === undefined) {
      ms = 100;
    }
    Thread.sleep(ms);
  };

  this.configure = function () {
    // Let's disable VCS-related tooltips since they sometimes lead to an exception when closing the project.
    const projectPropertiesComponent = PropertiesComponent.getInstance(underlyingProject);
    projectPropertiesComponent.setValue('ASKED_ADD_EXTERNAL_FILES', true);
    projectPropertiesComponent.setValue('ASKED_SHARE_PROJECT_CONFIGURATION_FILES', true);
  };

  // Tab & model management
  this.openGitMacheteTab = function () {
    openTab(ToolWindowId.VCS, 'Git Machete');
  };

  const openTab = function (toolWindowId, tabName) {
    const toolWindowManager = ToolWindowManager.getInstance(underlyingProject);
    let toolWindow;
    do {
      toolWindow = toolWindowManager.getToolWindow(toolWindowId);
      sleep();
    } while (toolWindow === null);

    // The method is NOT meant to be executed on the UI thread,
    // so `runOrInvokeAndWait` really means `enqueue onto the UI thread and wait until complete`.
    GuiUtils.runOrInvokeAndWait(() => {
      toolWindow.activate(() => {});
      const contentManager = toolWindow.getContentManager();
      const tab = contentManager.findContent(tabName);
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

  this.getManagedBranches = function () {
    const snapshot = getGraphTable().getGitMacheteRepositorySnapshot();
    if (snapshot != null) {
      return snapshot.getManagedBranches().map(b => b.getName()).toJavaArray(java.lang.String);
    } else {
      return java.lang.reflect.Array.newInstance(java.lang.String, 0);
    }
  };

  // Assumes that Git Machete tab is opened.
  // Returns the refreshed model.
  this.refreshGraphTableModel = function () {
    const graphTable = getGraphTable();

    let refreshDone = false;
    graphTable.queueRepositoryUpdateAndModelRefresh(/* doOnUIThreadWhenReady */ () => {
      refreshDone = true;
    });
    do {
      sleep();
    } while (!refreshDone);

    return graphTable.getModel();
  };


  // Actions

  const ACTION_PLACE_TOOLBAR = 'GitMacheteToolbar';
  const ACTION_PLACE_CONTEXT_MENU = 'GitMacheteContextMenu';
  const ACTION_PLACE_EMPTY = '';
  const SHOW_RESET_INFO = 'git-machete.reset.info.show';
  const SHOW_MERGE_WARNING = 'git-machete.merge.warning.show';

  const getActionByName = function (actionName) {
    return ActionManager.getInstance().getAction(actionName);
  };

  const createActionEvent = function (actionPlace, data) {
    const dataContext = new DataContext({
      getData: dataId => {
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

    GuiUtils.invokeLaterIfNeeded(
      () => action.actionPerformed(actionEvent),
      ModalityState.NON_MODAL
    );
  };

  const invokeActionAndWait = function (actionName, actionPlace, data) {
    const action = getActionByName(actionName);
    const actionEvent = createActionEvent(actionPlace, data);

    GuiUtils.runOrInvokeAndWait(() => action.actionPerformed(actionEvent));
  };

  this.discoverBranchLayout = function () {
    invokeActionAsync('GitMachete.DiscoverAction', ActionPlaces.ACTION_SEARCH, {});
    findAndClickButton('Save');
  }

  this.acceptCreateNewBranch = function () {
    findAndClickButton('Create');
  };

  this.acceptPush = function () {
    findAndClickButton('Push');
  };

  this.acceptForcePush = function () {
    findAndClickButton('Force Push');
  };

  this.acceptSquash = function () {
    findAndClickButton('OK');
  };

  this.acceptSlideIn = function () {
    findAndClickButton('Slide In');
  };

  this.acceptRebase = function () {
    findAndClickButton('Start Rebasing');
  };

  this.acceptResetToRemote = function () {
    findAndClickButton('Reset');
  };

  this.acceptSuggestedBranchLayout = function () {
    findAndClickButton('Yes');
  };

  this.acceptBranchDeletionOnSlideOut = function () {
    findAndClickButton('Slide Out & Delete Local Branch');
  };

  this.rejectBranchDeletionOnSlideOut = function () {
    findAndClickButton('Slide Out & Keep Local Branch');
  };

  this.contextMenu = {
    checkout: function () {
      findAndClickContextMenuAction('Checkout');
    },
    checkoutAndSyncByRebase: function () {
      findAndClickContextMenuAction('Checkout and Sync to Parent by Rebase\u2026');
    },
    syncByRebase: function () {
      findAndClickContextMenuAction('Sync to Parent by Rebase\u2026');
    },
    checkoutAndSyncByMerge: function () {
      findAndClickContextMenuAction('Checkout and Sync to Parent by Merge');
    },
    syncByMerge: function () {
      findAndClickContextMenuAction('Sync to Parent by Merge');
    },
    overrideForkPoint: function () {
      findAndClickContextMenuAction('Override Fork Point\u2026');
    },
    push: function () {
      findAndClickContextMenuAction('Push\u2026');
    },
    pull: function () {
      findAndClickContextMenuAction('Pull');
    },
    resetToRemote: function () {
      findAndClickContextMenuAction('Reset to Remote');
    },
    fastForwardMerge: function () {
      findAndClickContextMenuAction('Fast-forward Merge into Parent');
    },
    checkoutAndSquash: function () {
      findAndClickContextMenuAction('Checkout and Squash\u2026');
    },
    slideIn: function () {
      findAndClickContextMenuAction('Slide In Branch Below\u2026');
    },
    slideOut: function () {
      findAndClickContextMenuAction('Slide Out');
    },
    showInGitLog: function () {
      findAndClickContextMenuAction('Show in Git Log');
    }
  };

  this.toolbar = {
    syncByRebase: function () {
      findAndClickToolbarButton('Sync Current Branch to Parent by Rebase\u2026');
    },
    squash: function () {
      findAndClickToolbarButton('Squash Current Branch\u2026');
    },
    pull: function () {
      findAndClickToolbarButton('Pull Current Branch');
    },
    resetToRemote: function () {
      findAndClickToolbarButton('Reset Current Branch to Remote');
    },
    fastForwardMerge: function () {
      findAndClickToolbarButton('Fast-forward Merge Current Branch into Parent');
    },
    discoverBranchLayout: function () {
      findAndClickToolbarButton('Discover Branch Layout\u2026');
    },
    fetchAll: function () {
      findAndClickToolbarButton('Fetch All Remotes');
    },
    toggleListingCommits: function () {
      findAndClickToolbarButton('Toggle Listing Commits');
    }
  };

  this.findTextFieldAndWrite = function (text, instant) {
    const textField = getComponentByClass('com.intellij.ui.components.JBTextField');
    if (instant) {
      textField.setText(text);
    } else {
      for (let i = 0; i < text.length; i++) {
        sleep();
        let t = textField.getText() + text[i];
        textField.setText(t);
      }
    }
  };

  this.findComboBoxAndSwitchRepo = function (idx) {
    const comboBox = getComponentByClass('com.virtuslab.gitmachete.frontend.ui.impl.gitrepositoryselection.GitRepositoryComboBox');
    let fixture = new JComboBoxFixture(robot, comboBox);
    let newSelection = fixture.valueAt(idx);
    fixture.selectItem(newSelection);
  };

  this.findCellAndRightClick = function (name) {
    const graphTable = getComponentByClass('com.virtuslab.gitmachete.frontend.ui.impl.table.EnhancedGraphTable');
    let fixture = new JTableFixture(robot, graphTable);
    let contents = fixture.contents();

    const getCellRow = function () {
      const result = IntStream.range(0, contents.length)
        .filter(idx => contents[idx][0].includes('text=' + name))
        .toArray();
      return result.length === 1 ? result[0] : null;
    };

    let cellRow = getCellRow();
    let tableCell = TableCell.row(cellRow).column(0);
    fixture.cell(tableCell).click(MouseButton.RIGHT_BUTTON);
  };

  this.moveMouseToTheMiddleAndWait = function (secondsToWait) {
    const ideFrame = getIdeFrame();
    robot.moveMouse(ideFrame);
    sleep(1000 * secondsToWait);
  };

  const clickMouseInGraphTable = function () {
    const graphTable = getGraphTable();
    robot.click(graphTable);
    sleep();
  };

  const getIdeFrame = function() {
    return getComponentByClassAndPredicate(
        /* className */ 'com.intellij.openapi.wm.impl.IdeFrameImpl',
        // Using "contains" as in UITestSuite the project title is "machete-sandbox-worktree"
        // but in UIScenarioSuite it's "machete-sandbox"
        /* predicate */ component => component.getTitle().contains('machete-sandbox')
    );
  }

  const findAndClickButton = function (name) {
    const button = getComponentByClassAndPredicate('javax.swing.JButton',
        /* predicate */ component => name.equals(component.getText())
    );
    myClick(button, MouseButton.LEFT_BUTTON);
  };

  const findAndClickToolbarButton = function (name) {
    let fullName = 'Git Machete: ' + name;
    const getButton = function () {
      // findAll() returns a LinkedHashSet
      const result = robot.finder().findAll(component =>
        component instanceof ActionButton
          && fullName.equals(component.getAction().getTemplatePresentation().getText())
          && component.isEnabled()
      ).toArray();
      return result.length === 1 ? result[0] : null;
    };

    // The action is invoked asynchronously, let's first make sure the button has already appeared.
    let button = getButton();
    while (button === null) {
      clickMouseInGraphTable();
      button = getButton();
    }
    myClick(button, MouseButton.LEFT_BUTTON);
  };

  const findAndClickContextMenuAction = function (name) {
    const actionMenuItem = getComponentByClassAndPredicate('com.intellij.openapi.actionSystem.impl.ActionMenuItem',
        /* predicate */ component => name.equals(component.getText())
    );
    myClick(actionMenuItem, MouseButton.LEFT_BUTTON);
  };

  this.findAndResizeIdeFrame = function () {
    const ideFrame = getIdeFrame();
    let ideFrameFixture = new FrameFixture(ideFrame);
    let dimension = new Dimension(1024, 768);
    ideFrameFixture.resizeTo(dimension);
  };

  const getComponentByClass = function (className) {
    return getComponentByClassAndPredicate(
        className,
        /* predicate */ _ => true
    );
  };

  const getComponentByClassAndPredicate = function (className, predicate) {
    const searchForComponent = function () {
      const result = robot.finder().findAll(component =>
        className.equals(component.getClass().getName()) && predicate(component)
      ).toArray();
      return result.length === 1 ? result[0] : null;
    }
    // The action is invoked asynchronously, let's first make sure the component has already appeared.
    let component = searchForComponent();
    while (component === null) {
      clickMouseInGraphTable();
      component = searchForComponent();
    }
    return component;
  };

  this.toggleListingCommits = function () {
    invokeActionAndWait('GitMachete.ToggleListingCommitsAction', ACTION_PLACE_TOOLBAR, {});
  };

  this.checkoutBranch = function (branchName) {
    invokeActionAndWait('GitMachete.CheckoutSelectedAction', ACTION_PLACE_CONTEXT_MENU, { SELECTED_BRANCH_NAME: branchName });
  };

  this.checkoutFirstChildBranch = function () {
    invokeActionAndWait('GitMachete.CheckoutFirstChildAction', ACTION_PLACE_EMPTY, {});
  };

  this.checkoutNextBranch = function () {
    invokeActionAndWait('GitMachete.CheckoutNextAction', ACTION_PLACE_EMPTY, {});
  };

  this.checkoutPreviousBranch = function () {
    invokeActionAndWait('GitMachete.CheckoutPreviousAction', ACTION_PLACE_EMPTY, {});
  };

  this.checkoutParentBranch = function () {
    invokeActionAndWait('GitMachete.CheckoutParentAction', ACTION_PLACE_EMPTY, {});
  };

  this.fastForwardMergeSelectedToParent = function (branchName) {
    invokeActionAndWait('GitMachete.FastForwardMergeSelectedToParentAction', ACTION_PLACE_CONTEXT_MENU, { SELECTED_BRANCH_NAME: branchName });
  };

  this.fastForwardMergeCurrentToParent = function () {
    invokeActionAndWait('GitMachete.FastForwardMergeCurrentToParentAction', ACTION_PLACE_TOOLBAR, {});
  };

  this.syncSelectedToParentByRebase = function (branchName) {
    invokeActionAsync('GitMachete.SyncSelectedToParentByRebaseAction', ACTION_PLACE_CONTEXT_MENU, { SELECTED_BRANCH_NAME: branchName });
    this.acceptRebase()
  };

  this.syncCurrentToParentByRebase = function () {
    invokeActionAsync('GitMachete.SyncCurrentToParentByRebaseAction', ACTION_PLACE_CONTEXT_MENU, {});
    this.acceptRebase()
  };

  this.syncSelectedToParentByMerge = function (branchName) {
    PropertiesComponent.getInstance(underlyingProject).setValue(SHOW_MERGE_WARNING, false, /* default value */ true);

    invokeActionAndWait('GitMachete.SyncSelectedToParentByMergeAction', ACTION_PLACE_CONTEXT_MENU, { SELECTED_BRANCH_NAME: branchName });
  };

  this.syncCurrentToParentByMerge = function () {
    invokeActionAndWait('GitMachete.SyncCurrentToParentByMergeAction', ACTION_PLACE_TOOLBAR, {});
  };

  this.squashSelected = function (branchName) {
    invokeActionAndWait('GitMachete.SquashSelectedAction', ACTION_PLACE_CONTEXT_MENU, { SELECTED_BRANCH_NAME: branchName });
  };

  this.squashCurrent = function () {
    invokeActionAndWait('GitMachete.SquashCurrentAction', ACTION_PLACE_TOOLBAR, {});
  };

  this.pullSelected = function (branchName) {
    invokeActionAndWait('GitMachete.PullSelectedAction', ACTION_PLACE_CONTEXT_MENU, { SELECTED_BRANCH_NAME: branchName });
  };

  this.pullCurrent = function () {
    invokeActionAndWait('GitMachete.PullCurrentAction', ACTION_PLACE_TOOLBAR, {});
  };

  this.resetToRemote = function (branchName) {
    PropertiesComponent.getInstance(underlyingProject).setValue(SHOW_RESET_INFO, false, /* default value */ true);

    invokeActionAndWait('GitMachete.ResetSelectedToRemoteAction', ACTION_PLACE_CONTEXT_MENU, { SELECTED_BRANCH_NAME: branchName });
  };

  this.resetCurrentToRemote = function () {
    PropertiesComponent.getInstance(underlyingProject).setValue(SHOW_RESET_INFO, false, /* default value */ true);

    invokeActionAndWait('GitMachete.ResetCurrentToRemoteAction', ACTION_PLACE_TOOLBAR, {});
  };

  this.slideOutSelected = function (branchName) {
    invokeActionAndWait('GitMachete.SlideOutSelectedAction', ACTION_PLACE_CONTEXT_MENU, { SELECTED_BRANCH_NAME: branchName });
  };


  // Git utilities

  const getSelectedGitRepository = function () {
    // We can't rely on the Rhino's default classloader
    // since it operates in the context of the Remote Robot plugin, not our plugin.
    const providerClass = pluginClassLoader.loadClass('com.virtuslab.gitmachete.frontend.ui.providerservice.SelectedGitRepositoryProvider');
    const provider = underlyingProject.getService(providerClass);
    const gitRepository = provider.getSelectedGitRepository();
    // Let's make sure the data stored in the GitRepository object is up-to-date with the underlying .git/ folder.
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
    // We can't return the com.intellij.openapi.vcs.changes.Change objects
    // since they won't properly serialize for the transfer from Robot Remote plugin (in the IDE) back to the client (UI tests).
    return diff.stream().map(change => change.toString()).collect(Collectors.toList());
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

  this.getSyncToParentStatus = function (child) {
    const snapshot = getGraphTable().getGitMacheteRepositorySnapshot();
    const managedBranch = snapshot.getManagedBranchByName(child);
    return managedBranch.asNonRoot().getSyncToParentStatus().name();
  };
}
