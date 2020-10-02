package com.virtuslab.gitmachete.frontend.ui.impl.root;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;

@CustomLog
public class GitMacheteStartupActivity implements StartupActivity {

  @Override
  @UIEffect
  public void runActivity(Project project) {
    addOpenGitMacheteTabListener(project);
  }

  @UIEffect
  private void addOpenGitMacheteTabListener(Project project) {
    var toolWindowManager = ToolWindowManager.getInstance(project);
    var toolWindow = toolWindowManager.getToolWindow(ToolWindowId.VCS);

    if (toolWindow == null) {
      LOG.debug("VCS tool window does not exist");
      VcsNotifier.getInstance(project).notifyWarning(
          getString("action.GitMachete.OpenMacheteTabAction.notification.title.could-not-open-tab"), // t0d0: proper msg here
          getString("action.GitMachete.OpenMacheteTabAction.notification.message.no-git")); // t0d0: proper msg here
      return;
    }

    var listener = new GitMacheteTabOpenListener();
    toolWindow.addContentManagerListener(listener);
  }
}
