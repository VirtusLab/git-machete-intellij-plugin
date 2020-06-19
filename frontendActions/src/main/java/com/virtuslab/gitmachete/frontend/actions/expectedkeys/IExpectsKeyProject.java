package com.virtuslab.gitmachete.frontend.actions.expectedkeys;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;

import com.virtuslab.gitmachete.frontend.ui.providerservice.VcsRootComboBoxProvider;

public interface IExpectsKeyProject {
  default Project getProject(AnActionEvent anActionEvent) {
    var project = anActionEvent.getProject();
    assert project != null : "Can't get project from action event";
    return project;
  }

  default Option<GitRepository> getSelectedVcsRepository(AnActionEvent anActionEvent) {
    return getProject(anActionEvent).getService(VcsRootComboBoxProvider.class).getSelectedVcsRepository();
  }
}
