
importClass(java.lang.System);

importClass(com.intellij.ide.GeneralSettings);
importClass(com.intellij.ide.impl.ProjectUtil);
importClass(com.intellij.openapi.application.ApplicationManager);
importClass(com.intellij.openapi.application.ModalityState);
importClass(com.intellij.openapi.progress.ProgressManager);
importClass(com.intellij.ui.GuiUtils);

// Do not run any of the methods in EDT.
function Ide() {
  this.configure = function () {
    const settings = GeneralSettings.getInstance();
    settings.setConfirmExit(false);
    settings.setShowTipsOnStartup(false);
  };

  this.openProject = function (path) {
    ProjectUtil.openOrImport(path, /* projectToClose */ null, /* forceOpenInNewFrame */ false);
  };

  this.soleOpenedProject = function () {
    return new Project(ProjectUtil.getOpenProjects()[0]);
  }

  this.awaitNoBackgroundTask = function () {
    const progressManager = ProgressManager.getInstance();
    const method = getMethod(progressManager.getClass(), 'getCurrentIndicators');
    method.setAccessible(true);
    while (!method.invoke(progressManager).isEmpty()) {
      sleep();
    }
  }

  this.closeOpenedProjects = function () {
    ProjectUtil.getOpenProjects().forEach(function (project) {
      GuiUtils.runOrInvokeAndWait(function () {
        System.out.println('project to close = ' + project.toString());
        ProjectUtil.closeAndDispose(project);
      });
    });
  };

  this.close = function () {
    // We can't just run `app.exit()` synchronously since then the Robot client would never receive a reply from the Robot server,
    // and the entire test would end up in a ConnectException.
    GuiUtils.invokeLaterIfNeeded(function () {
      // Just to give enough time for the Robot server to send back the response to the client.
      sleep();
      ApplicationManager.getApplication().exit();
    }, ModalityState.NON_MODAL);
  };
}

const ide = new Ide();
