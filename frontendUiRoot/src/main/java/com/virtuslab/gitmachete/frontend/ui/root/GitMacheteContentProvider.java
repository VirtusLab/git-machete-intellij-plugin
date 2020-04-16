package com.virtuslab.gitmachete.frontend.ui.root;

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
    return new GitMachetePanel(project);
  }

  @Override
  public void disposeContent() {}

}
