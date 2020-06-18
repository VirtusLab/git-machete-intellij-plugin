package com.virtuslab.gitmachete.frontend.ui.impl.root.providerservice;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;
import lombok.Getter;

import com.virtuslab.gitmachete.frontend.ui.impl.root.VcsRootComboBox;

@Service
public final class VcsRootComboBoxProvider {

  @Getter
  private final VcsRootComboBox vcsRootComboBox;

  public VcsRootComboBoxProvider(Project project) {
    this.vcsRootComboBox = new VcsRootComboBox(project);
  }

  public Option<GitRepository> getSelectedVcsRepository() {
    return vcsRootComboBox.getSelectedRepository();
  }
}
