package com.virtuslab.gitmachete.frontend.ui.api.vcsrootcombobox;

import com.intellij.openapi.project.Project;

public interface IVcsRootComboBoxFactory {
  BaseVcsRootComboBox create(Project project);
}
