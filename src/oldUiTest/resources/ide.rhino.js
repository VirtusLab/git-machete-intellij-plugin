
importClass(java.lang.System);
importClass(java.util.Collections);

importClass(com.intellij.ide.GeneralSettings);
importClass(com.intellij.ide.impl.ProjectUtil);
importClass(com.intellij.ide.plugins.PluginManagerCore);
importClass(com.intellij.ide.util.PropertiesComponent);
importClass(com.intellij.openapi.application.ApplicationInfo);
importClass(com.intellij.openapi.application.ApplicationManager);
importClass(com.intellij.openapi.extensions.PluginId);
importClass(com.intellij.openapi.progress.ProgressManager);

// Do not run any of the methods on the UI thread.
function Ide() {
  this.configure = function () {
    const settings = GeneralSettings.getInstance();
    settings.setConfirmExit(false);
    settings.setShowTipsOnStartup(false);
  };

  this.soleOpenedProject = function () {
    const openProjects = ProjectUtil.getOpenProjects();
    return openProjects.length === 1 ? new Project(openProjects[0]) : null;
  };

  this.closeOpenedProjects = function () {
    ProjectUtil.getOpenProjects().forEach(project => {
      ApplicationManager.getApplication().invokeAndWait(() => {
        System.out.println('project to close = ' + project.toString());
        ProjectUtil.closeAndDispose(project);
      });
    });
  };
}

// See https://github.com/JetBrains/intellij-ui-test-robot#store-data-between-runjscalljs-requests
global.put('ide', new Ide());
