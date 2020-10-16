package com.virtuslab.gitmachete.frontend.ui.impl.root;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
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
      LOG.debug("Failed to attach activity for VCS tool window - VCS tool window does not exist");
      return;
    }

    var listener = new RediscoverSuggester(project);
    toolWindow.addContentManagerListener(listener);
  }
}
