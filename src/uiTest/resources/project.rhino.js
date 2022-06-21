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


// Do not run any of the methods on the UI thread.
function Project(underlyingProject) {

  const pluginId = PluginId.getId('com.virtuslab.git-machete');
  const pluginClassLoader = PluginManagerCore.getPlugin(pluginId).getPluginClassLoader();

  const sleep = function() {
    Thread.sleep(100);
  }

  this.configure = function () {
    // Let's disable VCS-related tooltips since they sometimes lead to an exception when closing the project.
    const projectPropertiesComponent = PropertiesComponent.getInstance(underlyingProject);
    projectPropertiesComponent.setValue('ASKED_ADD_EXTERNAL_FILES', true);
    projectPropertiesComponent.setValue('ASKED_SHARE_PROJECT_CONFIGURATION_FILES', true);
  };

  // Tab & model management
  this.openGitMacheteTab = function () {
    openTab(ToolWindowId.VCS, 'Git Machete');
  }

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
  }

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
  const RESET_INFO_SHOWN = 'git-machete.reset.info.shown';

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

  this.saveDiscoveredBranchLayout = function () {
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

  const outer = this;

  this.contextMenu = {
    checkout: function () {
      findAndClickContextMenuAction('Checkout');
    },
    checkoutAndSyncByRebase: function () {
      findAndClickContextMenuAction('Checkout and Sync to Parent by Rebase...');
    },
    syncByRebase: function () {
      findAndClickContextMenuAction('Sync to Parent by Rebase...');
    },
    checkoutAndSyncByMerge: function () {
      findAndClickContextMenuAction('Checkout and Sync to Parent by Merge');
    },
    syncByMerge: function () {
      findAndClickContextMenuAction('Sync to Parent by Merge');
    },
    overrideForkPoint: function () {
      findAndClickContextMenuAction('Override Fork Point...');
    },
    push: function () {
      findAndClickContextMenuAction('Push...');
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
    slideIn: function () {
      findAndClickContextMenuAction('Slide In Branch Below...');
    },
    slideOut: function () {
      System.out.println("slideOut()");
      findAndClickContextMenuAction('Slide Out');
    },
    slideOutDefensive: function (branch) {
      System.out.println("slideOutDefensive(branch=" + branch + ")");
      outer.findCellAndRightClick(branch);
      while (!findAndClickContextMenuActionNoWait('Slide Out')) {
        outer.clickMouseInTheMiddle();
        outer.findCellAndRightClick(branch);
      }
    },
    showInGitLog: function () {
      findAndClickContextMenuAction('Show in Git Log');
    }
  };

  this.toolbar = {
    syncByRebase: function () {
      findAndClickToolbarButton('Sync Current Branch to Parent by Rebase...');
    },
    syncByMerge: function () {
      findAndClickToolbarButton('Sync Current Branch to Parent by Merge');
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
      findAndClickToolbarButton('Discover Branch Layout...');
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
      for (var i = 0; i < text.length; i++) {
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
    System.out.println("findCellAndRightClick(" + name + "): cellRow = " + cellRow + ", tableCell = " + tableCell);
    fixture.cell(tableCell).click(MouseButton.RIGHT_BUTTON);
  };

  this.moveMouseToTheMiddleAndWait = function (secondsToWait) {
    const ideFrame = getComponentByClass('com.intellij.openapi.wm.impl.IdeFrameImpl');
    robot.moveMouse(ideFrame);
    for (var i = 0; i < 10 * secondsToWait; i++) {
      sleep();
    }
  };

  this.clickMouseInTheMiddle = function () {
    const ideFrame = getComponentByClass('com.intellij.openapi.wm.impl.IdeFrameImpl');
    robot.click(ideFrame);
  };

  const findAndClickButton = function (name) {
    const button = getComponentByClassAndText('javax.swing.JButton', name);
    prettyClick(button, MouseButton.LEFT_BUTTON);
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
      outer.clickMouseInTheMiddle();
      button = getButton();
    }
    prettyClick(button, MouseButton.LEFT_BUTTON);
  };

  const findAndClickContextMenuAction = function (name) {
    const actionMenuItem = getComponentByClassAndText('com.intellij.openapi.actionSystem.impl.ActionMenuItem', name);
    prettyClick(actionMenuItem, MouseButton.LEFT_BUTTON);
  };

  const findAndClickContextMenuActionNoWait = function (name) {
    const actionMenuItem = getComponentByClassAndTextNoWait('com.intellij.openapi.actionSystem.impl.ActionMenuItem', name);
    if (actionMenuItem) {
      prettyClick(actionMenuItem, MouseButton.LEFT_BUTTON);
      return true;
    } else {
      return false;
    }
  };

  this.findAndResizeIdeFrame = function () {
    const ideFrame = getComponentByClass('com.intellij.openapi.wm.impl.IdeFrameImpl');
    let ideFrameFixture = new FrameFixture(ideFrame);
    let dimension = new Dimension(1024, 768);
    ideFrameFixture.resizeTo(dimension);
  };

  const getComponentByClass = function (className) {
    return getComponent(
        className,
        /* text */ null,
        /* textCmp */ function (_, _) { return true; }
    );
  };

  const getComponentByClassAndText = function (className, text, textCmp) {
    if (textCmp === undefined) { // default
      textCmp = function (t, c) { return t.equals(c.getText()); };
    }
    return getComponent(className, text, textCmp);
  };

  const getComponentByClassAndTextNoWait = function (className, text, textCmp) {
    if (textCmp === undefined) { // default
      textCmp = function (t, c) { return t.equals(c.getText()); };
    }
    return getComponentNoWait(className, text, textCmp);
  };

  const getComponent = function (className, text, textCmp) {
    const searchForComponent = function () {
      const result = robot.finder().findAll(component =>
        className.equals(component.getClass().getName()) && textCmp(text, component)
      ).toArray();
      return result.length === 1 ? result[0] : null;
    }
    // The action is invoked asynchronously, let's first make sure the component has already appeared.
    let component = searchForComponent();
    while (component === null) {
      sleep();
      component = searchForComponent();
    }
    return component;
  };

  const getComponentNoWait = function (className, text, textCmp) {
    const resultClassOnly = robot.finder().findAll(component =>
      className.equals(component.getClass().getName())
    ).toArray();
    System.out.println("getComponentNoWait(className=" + className + ", text=" + text + "): resultClassOnly " + resultClassOnly.length)

    const result = robot.finder().findAll(component =>
      className.equals(component.getClass().getName()) && textCmp(text, component)
    ).toArray();
    System.out.println("getComponentNoWait(className=" + className + ", text=" + text + "): result " + result.length)

    return result.length === 1 ? result[0] : null;
  };

  const prettyClick = function (component, mouseButton) {
    robot.moveMouse(component);
    // Wait for a while before clicking to allow the scenario spectator to see the button being clicked
    for (var i = 0; i < 5; i++) {
      sleep();
    }
    robot.click(component, mouseButton);
  }

  this.toggleListingCommits = function () {
    invokeActionAndWait('GitMachete.ToggleListingCommitsAction', ACTION_PLACE_TOOLBAR, {});
  };

  this.checkoutBranch = function (branchName) {
    invokeActionAndWait('GitMachete.CheckoutSelectedBranchAction', ACTION_PLACE_CONTEXT_MENU, { SELECTED_BRANCH_NAME: branchName });
  };

  this.fastForwardMergeSelectedBranchToParent = function (branchName) {
    invokeActionAndWait('GitMachete.FastForwardMergeSelectedBranchToParentAction', ACTION_PLACE_CONTEXT_MENU, { SELECTED_BRANCH_NAME: branchName });
  };

  this.fastForwardMergeCurrentBranchToParent = function () {
    invokeActionAndWait('GitMachete.FastForwardMergeCurrentBranchToParentAction', ACTION_PLACE_TOOLBAR, {});
  };

  this.syncSelectedToParentByRebaseAction = function (branchName) {
    invokeActionAsync('GitMachete.SyncSelectedToParentByRebaseAction', ACTION_PLACE_CONTEXT_MENU, { SELECTED_BRANCH_NAME: branchName });
  };

  this.syncCurrentToParentByRebaseAction = function (branchName) {
    invokeActionAsync('GitMachete.SyncCurrentToParentByRebaseAction', ACTION_PLACE_CONTEXT_MENU, {});
  };

  this.syncSelectedToParentByMergeAction = function (branchName) {
    invokeActionAndWait('GitMachete.SyncSelectedToParentByMergeAction', ACTION_PLACE_CONTEXT_MENU, { SELECTED_BRANCH_NAME: branchName });
  };

  this.syncCurrentToParentByMergeAction = function () {
    invokeActionAndWait('GitMachete.SyncCurrentToParentByMergeAction', ACTION_PLACE_TOOLBAR, {});
  };

  this.pullSelectedBranch = function (branchName) {
    invokeActionAndWait('GitMachete.PullSelectedBranchAction', ACTION_PLACE_CONTEXT_MENU, { SELECTED_BRANCH_NAME: branchName });
  };

  this.pullCurrentBranch = function () {
    invokeActionAndWait('GitMachete.PullCurrentBranchAction', ACTION_PLACE_TOOLBAR, {});
  };

  this.resetBranchToRemote = function (branchName) {
    PropertiesComponent.getInstance().setValue(RESET_INFO_SHOWN, true);

    invokeActionAndWait('GitMachete.ResetSelectedBranchToRemoteAction', ACTION_PLACE_CONTEXT_MENU, { SELECTED_BRANCH_NAME: branchName });
  };

  this.resetCurrentBranchToRemote = function () {
    PropertiesComponent.getInstance().setValue(RESET_INFO_SHOWN, true);

    invokeActionAndWait('GitMachete.ResetCurrentBranchToRemoteAction', ACTION_PLACE_TOOLBAR, {});
  };

  this.slideOutBranch = function (branchName) {
    invokeActionAndWait('GitMachete.SlideOutSelectedBranchAction', ACTION_PLACE_CONTEXT_MENU, { SELECTED_BRANCH_NAME: branchName });
  };


  // Git utilities

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
    return snapshot.getManagedBranchByName(child)
      .map(p => p.asNonRoot().getSyncToParentStatus().name())
      .getOrElse('');
  };
}
