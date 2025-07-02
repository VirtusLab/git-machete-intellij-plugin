importClass(java.lang.IllegalStateException);
importClass(java.lang.System);
importClass(java.lang.Thread);
importClass(java.nio.file.Paths);
importClass(java.util.Arrays);
importClass(java.util.stream.Collectors);

importClass(com.intellij.ide.plugins.PluginManagerCore);
importClass(com.intellij.ide.impl.OpenProjectTask);
importClass(com.intellij.ide.impl.ProjectUtil);
importClass(com.intellij.ide.impl.TrustedPathsSettings);
importClass(com.intellij.ide.util.PropertiesComponent);
importClass(com.intellij.openapi.actionSystem.ActionManager);
importClass(com.intellij.openapi.actionSystem.ActionPlaces);
importClass(com.intellij.openapi.actionSystem.AnActionEvent);
importClass(com.intellij.openapi.actionSystem.DataContext);
importClass(com.intellij.openapi.actionSystem.Presentation);
importClass(com.intellij.openapi.actionSystem.impl.ActionButton);
importClass(com.intellij.openapi.application.ApplicationManager);
importClass(com.intellij.openapi.application.ModalityState);
importClass(com.intellij.openapi.components.ServiceManager);
importClass(com.intellij.openapi.extensions.PluginId);
importClass(com.intellij.openapi.project.ex.ProjectManagerEx);
importClass(com.intellij.openapi.wm.ToolWindowId);
importClass(com.intellij.openapi.wm.ToolWindowManager);
importClass(com.intellij.util.ModalityUiUtil);

// Do not run any of the methods on the UI thread.
function Project(underlyingProject) {

  const pluginId = PluginId.getId('com.virtuslab.git-machete');
  const pluginClassLoader = PluginManagerCore.getPlugin(pluginId).getPluginClassLoader();

  const sleep = function (ms) {
    if (ms === undefined) {
      ms = 100;
    }
    Thread.sleep(ms);
  };

  // Tab & model management
  this.openGitMacheteTab = function () {
    openTab(ToolWindowId.VCS, 'Git Machete');
  };

  const openTab = function (toolWindowId, tabName) {
    const toolWindowManager = ToolWindowManager.getInstance(underlyingProject);

    let toolWindow, i = 0;
    do {
      toolWindow = toolWindowManager.getToolWindow(toolWindowId);
      sleep();
    } while (toolWindow === null && ++i < 50);
    if (toolWindow === null) {
      throw new IlegalStateException("Waiting for " + toolWindowId + " tool window timed out");
    }

    // The method is NOT meant to be executed on the UI thread,
    // so `runOrInvokeAndWait` really means `enqueue onto the UI thread and wait until complete`.
    ApplicationManager.getApplication().invokeAndWait(() => {
      toolWindow.activate(() => {});
      const contentManager = toolWindow.getContentManager();
      let tab, i = 0;
      do {
        // It can (very rarely) happen that the tab isn't instantly available.
        tab = contentManager.findContent(tabName);
        sleep();
      } while (tab === null && ++i < 10);
      if (tab === null) {
        throw new IlegalStateException("Waiting for " + tabName + " tab timed out");
      }
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
      // Array is already defined in JS, cannot be imported
      return java.lang.reflect.Array.newInstance(java.lang.String, 0);
    }
  };

  this.getManagedBranchesAndCommits = function () {
    const snapshot = getGraphTable().getGitMacheteRepositorySnapshot();
    if (snapshot != null) {
      return snapshot.getManagedBranches().flatMap(b => {
        const branchesAndCommits = new LinkedList();
        if (b.isNonRoot()) {
          const nonRoot = b.asNonRoot();
          branchesAndCommits.addAll(nonRoot.getUniqueCommits().map(c => c.getShortMessage()).toJavaList());
        }
        branchesAndCommits.add(b.getName());
        return branchesAndCommits;
      }).toJavaArray(java.lang.String);
    } else {
      // Array is already defined in JS, cannot be imported
      return java.lang.reflect.Array.newInstance(java.lang.String, 0);
    }
  };

  // Assumes that Git Machete tab is opened.
  // Returns the refreshed model.
  this.refreshGraphTableModel = function () {
    const graphTable = getGraphTable();
    graphTable.refreshMacheteFile();
    let refreshDone = false;
    graphTable.queueRepositoryUpdateAndModelRefresh(/* doOnUIThreadWhenReady */ () => {
      refreshDone = true;
    });

    let i = 0;
    do {
      sleep();
    } while (!refreshDone && ++i < 50);
    if (!refreshDone) {
      throw new IlegalStateException("Waiting for condition timed out");
    }

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

    ModalityUiUtil.invokeLaterIfNeeded(ModalityState.NON_MODAL,
      () => action.actionPerformed(actionEvent)
    );
  };

  const invokeActionAndWait = function (actionName, actionPlace, data) {
    const action = getActionByName(actionName);
    const actionEvent = createActionEvent(actionPlace, data);

    ApplicationManager.getApplication().invokeAndWait(() => action.actionPerformed(actionEvent));
  };

  this.discoverBranchLayout = function () {
    invokeActionAsync('GitMachete.DiscoverAction', ActionPlaces.ACTION_SEARCH, {});
    findAndClickButton('Save');
  }

  const acceptRebase = function () {
    findAndClickButton('Start Rebasing');
  };

  this.acceptSquash = function () {
    findAndClickButton('OK');
  };

  this.acceptSuggestedBranchLayout = function () {
    findAndClickButton('Yes');
  };

  this.acceptBranchDeletionOnSlideOut = function () {
    findAndClickButton('Slide Out & Delete Local Branch');
  };

  this.pullCurrent = function () {
    findAndClickToolbarButton('Pull Current Branch');
  };

  this.toggleListingCommits = function () {
    findAndClickToolbarButton('Toggle Listing Commits');
  };

  const clickMouseInGraphTable = function () {
    const graphTable = getGraphTable();
    robot.click(graphTable);
    sleep();
  };

  const findAndClickButton = function (name) {
    const button = getComponentByClassAndPredicate('javax.swing.JButton',
        /* predicate */ component => name.equals(component.getText())
    );
    robot.click(button, MouseButton.LEFT_BUTTON);
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
    let button = getButton(), i = 0;
    while (button === null && i++ < 50) {
      clickMouseInGraphTable();
      button = getButton();
    }
    if (button === null) {
      throw new IllegalStateException("Waiting for condition timed out");
    }
    robot.click(button, MouseButton.LEFT_BUTTON);
  };

  /** Note that we're checking for the components that are exactly of the provided class, or of any subclass */
  const getComponentByClassAndPredicate = function (className, predicate) {
    const searchForComponent = function () {
      const clazz = pluginClassLoader.loadClass(className);
      const result = robot.finder().findAll(component =>
        clazz.isInstance(component) && predicate(component)
      ).toArray();
      // TODO (#1079): in `squashBranch` UI test, the second squash seems to never execute...
      //  is there a problem with a different "OK" button getting clicked?
      System.out.println("getComponentByClassAndPredicate(" + className + ", [predicate]) = "
          + Arrays.deepToString(result) + "(" + result.length + " element(s))");
      return result.length === 1 ? result[0] : null;
    }
    // The action is invoked asynchronously, let's first make sure the component has already appeared.
    let component = searchForComponent(), i = 0;
    while (component === null && i++ < 100) {
      clickMouseInGraphTable();
      component = searchForComponent();
    }
    if (component === null) {
      throw new IllegalStateException("Waiting for condition timed out");
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
    acceptRebase()
  };

  this.syncCurrentToParentByRebase = function () {
    invokeActionAsync('GitMachete.SyncCurrentToParentByRebaseAction', ACTION_PLACE_CONTEXT_MENU, {});
    acceptRebase()
  };

  this.syncSelectedToParentByMerge = function (branchName) {
    PropertiesComponent.getInstance(underlyingProject).setValue(SHOW_MERGE_WARNING, false, /* default value */ true);

    invokeActionAndWait('GitMachete.SyncSelectedToParentByMergeAction', ACTION_PLACE_CONTEXT_MENU, { SELECTED_BRANCH_NAME: branchName });
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
    const gitRepositorySelectionProviderClass = pluginClassLoader.loadClass('com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection.IGitRepositorySelectionProvider');
    const gitRepositorySelectionProvider = underlyingProject.getService(gitRepositorySelectionProviderClass);
    const gitRepository = gitRepositorySelectionProvider.getSelectedGitRepository();
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

  this.doesBranchExist = function (branchName) {
    return this.getHashOfCommitPointedByBranch(branchName) != null;
  }

  this.getSyncToParentStatus = function (child) {
    const snapshot = getGraphTable().getGitMacheteRepositorySnapshot();
    const managedBranch = snapshot.getManagedBranchByName(child);
    return managedBranch.asNonRoot().getSyncToParentStatus().name();
  };
}

function getSoleOpenProject() {
  const openProjects = ProjectUtil.getOpenProjects();
  if (openProjects.length !== 1) {
    throw new IllegalStateException("Expected exactly one open project, found " + openProjects.length + " instead: " + openProjects);
  }
  return new Project(openProjects[0]);
}
// See https://github.com/JetBrains/intellij-ui-test-robot#store-data-between-runjscalljs-requests
global.put('getSoleOpenProject', getSoleOpenProject);

function openProject(projectPath) {
  const projectManager = ProjectManagerEx.getInstanceEx();
  const currentProject = projectManager.getOpenProjects()[0];
  if (currentProject) {
    ApplicationManager.getApplication().invokeAndWait(() => projectManager.closeAndDispose(currentProject));
  }

  const trustedPathsSettings = ServiceManager.getService(TrustedPathsSettings);
  trustedPathsSettings.addTrustedPath(projectPath);

  ApplicationManager.getApplication().invokeAndWait(() => {
    const newProject = projectManager.openProject(Paths.get(projectPath), OpenProjectTask.build());
    ProjectUtil.focusProjectWindow(newProject, true);
  });
}
global.put('openProject', openProject);
