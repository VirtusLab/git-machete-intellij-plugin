
importClass(java.lang.System);
importClass(java.util.stream.Collectors);
importClass(java.util.stream.Stream);

importClass(com.intellij.diagnostic.DebugLogManager);
importClass(com.intellij.ide.GeneralSettings);
importClass(com.intellij.ide.impl.ProjectUtil);
importClass(com.intellij.ide.plugins.PluginManagerCore);
importClass(com.intellij.ide.util.PropertiesComponent);
importClass(com.intellij.openapi.application.ApplicationManager);
importClass(com.intellij.openapi.application.ModalityState);
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

  this.openProject = function (path) {
    const project = ProjectUtil.openOrImport(path, /* projectToClose */ null, /* forceOpenInNewFrame */ false);
    // Let's disable VCS-related tooltips since they sometimes lead to an exception when closing the project.
    const propertiesComponent = PropertiesComponent.getInstance(project);
    propertiesComponent.setValue('ASKED_ADD_EXTERNAL_FILES', true);
    propertiesComponent.setValue('ASKED_SHARE_PROJECT_CONFIGURATION_FILES', true);
    return new Project(project);
  };

  this.soleOpenedProject = function () {
    const openProjects = ProjectUtil.getOpenProjects();
    return openProjects.length === 1 ? new Project(openProjects[0]) : null;
  }

  this.getProgressIndicators = function () {
    const progressManager = ProgressManager.getInstance();
    const method = getNonPublicMethod(progressManager.getClass(), 'getCurrentIndicators');
    method.setAccessible(true);
    return method.invoke(progressManager).stream().map(function (indicator) {
      return indicator.toString();
    }).collect(Collectors.toList());
  }

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
