package com.virtuslab.gitmachete.frontend.ui.impl.root;

import javax.swing.JComponent;

import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTracker;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentProvider;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.externalsystem.MacheteProjectAware;

public class GitMacheteContentProvider implements ChangesViewContentProvider {
  private final Project project;

  public GitMacheteContentProvider(Project project) {
    this.project = project;
  }

  @Override
  @UIEffect
  public JComponent initContent() {
    var service = project.getService(MacheteProjectAware.class);
    var externalSystemProjectTracker = ExternalSystemProjectTracker.getInstance(project);
    externalSystemProjectTracker.register(service);
    externalSystemProjectTracker.activate(service.getProjectId());
    return new GitMachetePanel(project);
  }

  @Override
  public void disposeContent() {}

}
