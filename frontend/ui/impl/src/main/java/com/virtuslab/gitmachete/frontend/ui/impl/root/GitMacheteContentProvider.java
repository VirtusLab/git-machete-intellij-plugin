package com.virtuslab.gitmachete.frontend.ui.impl.root;

import javax.swing.JComponent;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentProvider;
import org.checkerframework.checker.guieffect.qual.UIEffect;

public class GitMacheteContentProvider implements ChangesViewContentProvider {
  private final Project project;

  public GitMacheteContentProvider(Project project) {
    this.project = project;
  }

  @Override
  @UIEffect
  public JComponent initContent() {
    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      System.out.println("Expected EDT:");
      System.out.println(stackTrace);
      throw new RuntimeException("Expected EDT: " + stackTrace);
    }
    return new GitMachetePanel(project);
  }

  @Override
  public void disposeContent() {}

}
