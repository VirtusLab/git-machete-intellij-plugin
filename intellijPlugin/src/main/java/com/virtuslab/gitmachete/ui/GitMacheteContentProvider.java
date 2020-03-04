package com.virtuslab.gitmachete.ui;

import javax.annotation.Nonnull;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentProvider;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.NotNullFunction;

import com.virtuslab.gitmachete.ui.table.GitMacheteGraphTable;
import git4idea.GitVcs;

public class GitMacheteContentProvider implements ChangesViewContentProvider {
  public static final String GIT_MACHETE_TOOLBAR = "GitMacheteToolbar";
  private final Project project;

  public GitMacheteContentProvider(@Nonnull Project project) {
    this.project = project;
  }

  @Override
  public JComponent initContent() {
    GitMachetePanel gitMachetePanel = new GitMachetePanel(project);
    ActionToolbar gitMacheteToolbar = gitMachetePanel.createGitMacheteToolbar();

    GitMacheteGraphTable gitMacheteGraphTable = gitMachetePanel.getGitMacheteGraphTableManager()
        .getGitMacheteGraphTable();
    JScrollPane scrollTable = ScrollPaneFactory.createScrollPane(gitMacheteGraphTable);

    SimpleToolWindowPanel toolbarAndTable = new SimpleToolWindowPanel(/* vertical */ false, /* borderless */ true);
    toolbarAndTable.setToolbar(gitMacheteToolbar.getComponent());
    toolbarAndTable.setContent(scrollTable);

    return toolbarAndTable;
  }

  @Override
  public void disposeContent() {}

  public static class GitMacheteVisibilityPredicate implements NotNullFunction<Project, Boolean> {
    @Nonnull
    @Override
    public Boolean fun(Project project) {
      return ProjectLevelVcsManager.getInstance(project).checkVcsIsActive(GitVcs.NAME);
    }
  }
}
