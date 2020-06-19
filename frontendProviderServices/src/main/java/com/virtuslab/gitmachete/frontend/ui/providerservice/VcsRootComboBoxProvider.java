package com.virtuslab.gitmachete.frontend.ui.providerservice;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;
import lombok.Getter;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.gitmachete.frontend.ui.api.vcsrootcombobox.BaseVcsRootComboBox;
import com.virtuslab.gitmachete.frontend.ui.api.vcsrootcombobox.IVcsRootComboBoxFactory;

@Service
public final class VcsRootComboBoxProvider {

  @Getter
  private final BaseVcsRootComboBox vcsRootComboBox;

  public VcsRootComboBoxProvider(Project project) {
    this.vcsRootComboBox = RuntimeBinding
        .instantiateSoleImplementingClass(IVcsRootComboBoxFactory.class)
        .create(project);
  }

  public Option<GitRepository> getSelectedVcsRepository() {
    return vcsRootComboBox.getSelectedRepository();
  }
}
