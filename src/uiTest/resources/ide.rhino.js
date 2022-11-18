
importClass(java.lang.System);
importClass(java.util.Collections);

importClass(com.intellij.diagnostic.DebugLogManager);
importClass(com.intellij.ide.GeneralSettings);
importClass(com.intellij.ide.impl.ProjectUtil);
importClass(com.intellij.ide.plugins.PluginManagerCore);
importClass(com.intellij.ide.util.PropertiesComponent);
importClass(com.intellij.openapi.application.ApplicationInfo);
importClass(com.intellij.openapi.extensions.PluginId);
importClass(com.intellij.openapi.progress.ProgressManager);
importClass(com.intellij.ui.GuiUtils);

// Do not run any of the methods on the UI thread.
function Ide() {
  this.configure = function (enableDebugLog) {
    const settings = GeneralSettings.getInstance();
    settings.setConfirmExit(false);
    settings.setShowTipsOnStartup(false);

    const logCategories = new LinkedList();
    if (enableDebugLog) {
      logCategories.add(new DebugLogManager.Category('com.virtuslab', DebugLogManager.DebugLogLevel.DEBUG))
    }

    // `applyCategories` is non-persistent (so the categories don't stick for the future IDE runs), unlike `saveCategories`.
    const debugLogManager = DebugLogManager.getInstance();
    debugLogManager.applyCategories(logCategories);
  };

  this.soleOpenedProject = function () {
    const openProjects = ProjectUtil.getOpenProjects();
    return openProjects.length === 1 ? new Project(openProjects[0]) : null;
  };

  this.closeOpenedProjects = function () {
    ProjectUtil.getOpenProjects().forEach(project => {
      GuiUtils.runOrInvokeAndWait(() => {
        System.out.println('project to close = ' + project.toString());
        ProjectUtil.closeAndDispose(project);
      });
    });
  };
}

// See https://github.com/JetBrains/intellij-ui-test-robot#store-data-between-runjscalljs-requests
global.put('ide', new Ide());
