package com.virtuslab.gitmachete.frontend.ui.impl.vcsrootcombobox;

import com.intellij.openapi.project.Project;

import com.virtuslab.gitmachete.frontend.ui.api.vcsrootcombobox.BaseVcsRootComboBox;
import com.virtuslab.gitmachete.frontend.ui.api.vcsrootcombobox.IVcsRootComboBoxFactory;

public class GitMacheteVcsRootComboBoxFactory implements IVcsRootComboBoxFactory {
  @Override
  public BaseVcsRootComboBox create(Project project) {
    return new VcsRootComboBox(project);
  }
}
