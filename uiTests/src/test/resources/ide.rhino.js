
importClass(java.lang.System);
importClass(java.util.stream.Collectors);
importClass(java.util.stream.Stream);

importClass(com.intellij.diagnostic.DebugLogManager);
importClass(com.intellij.ide.GeneralSettings);
importClass(com.intellij.ide.impl.ProjectUtil);
importClass(com.intellij.ide.plugins.PluginManagerCore);
importClass(com.intellij.ide.util.PropertiesComponent);
importClass(com.intellij.openapi.extensions.PluginId);
importClass(com.intellij.openapi.progress.ProgressManager);
importClass(com.intellij.ui.GuiUtils);

// Do not run any of the methods on the UI thread.
function Ide() {
  this.configure = function (enableDebugLog) {
    const settings = GeneralSettings.getInstance();
    settings.setConfirmExit(false);
    settings.setShowTipsOnStartup(false);

    if (enableDebugLog) {
      const logCategories = Stream.of(
        'binding',
        'branchlayout',
        'gitcore',
        'gitmachete.backend',
        'gitmachete.frontend.actions',
        'gitmachete.frontend.externalsystem',
        'gitmachete.frontend.graph',
        'gitmachete.frontend.ui',
      ).map(function (name) {
        return new DebugLogManager.Category(name, DebugLogManager.DebugLogLevel.DEBUG);
      }).collect(Collectors.toList());

      const debugLogManager = DebugLogManager.getInstance();
      // `applyCategories` is non-persistent (so the categories don't stick for the future IDE runs), unlike `saveCategories`.
      debugLogManager.applyCategories(logCategories);
    }
  };

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
