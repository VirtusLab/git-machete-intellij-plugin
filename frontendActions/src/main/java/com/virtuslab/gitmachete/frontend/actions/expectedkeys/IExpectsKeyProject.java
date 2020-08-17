package com.virtuslab.gitmachete.frontend.actions.expectedkeys;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;

import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.gitmachete.frontend.ui.providerservice.BranchLayoutWriterProvider;
import com.virtuslab.gitmachete.frontend.ui.providerservice.GraphTableProvider;
import com.virtuslab.gitmachete.frontend.ui.providerservice.SelectedGitRepositoryProvider;

public interface IExpectsKeyProject extends IWithLogger {
  default Project getProject(AnActionEvent anActionEvent) {
    var project = anActionEvent.getProject();
    assert project != null : "Can't get project from action event";
    return project;
  }

  default Option<Project> tryGetProject(AnActionEvent anActionEvent) {
    return Option.of(anActionEvent.getProject());
  }

  default IBranchLayoutWriter getBranchLayoutWriter(AnActionEvent anActionEvent) {
    return getProject(anActionEvent).getService(BranchLayoutWriterProvider.class).getBranchLayoutWriter();
  }

  default BaseEnhancedGraphTable getGraphTable(AnActionEvent anActionEvent) {
    return getProject(anActionEvent).getService(GraphTableProvider.class).getGraphTable();
  }

  default Option<GitRepository> getSelectedGitRepositoryWithoutLogging(AnActionEvent anActionEvent) {
    return getProject(anActionEvent).getService(SelectedGitRepositoryProvider.class).getSelectedGitRepository();
  }

  default Option<GitRepository> getSelectedGitRepositoryWithLogging(AnActionEvent anActionEvent) {
    var gitRepository = getProject(anActionEvent).getService(SelectedGitRepositoryProvider.class)
        .getSelectedGitRepository();
    if (gitRepository.isEmpty()) {
      log().warn("No Git repository is selected");
    }
    return gitRepository;
  }
}
