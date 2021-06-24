
importClass(java.lang.System);
importClass(java.util.stream.Collectors);
importClass(java.util.stream.Stream);

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
  this.soleOpenedProject = function () {
    const openProjects = ProjectUtil.getOpenProjects();
    return openProjects.length === 1 ? new Project(openProjects[0]) : null;
  };

  this.closeOpenedProjects = function () {
    ProjectUtil.getOpenProjects().forEach(function (project) {
      GuiUtils.runOrInvokeAndWait(function () {
        System.out.println('project to close = ' + project.toString());
        ProjectUtil.closeAndDispose(project);
      });
    });
  };
}

const ide = new Ide();
const pluginId = PluginId.getId('com.virtuslab.git-machete');
const pluginClassLoader = PluginManagerCore.getPlugin(pluginId).getPluginClassLoader();
const project = ide.soleOpenedProject();
