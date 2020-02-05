package com.virtuslab.gitmachete.ui;

import static com.intellij.ui.IdeBorderFactory.createBorder;
import static com.intellij.util.ui.UIUtil.addBorder;

import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentProvider;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SideBorder;
import com.intellij.util.NotNullFunction;
import git4idea.GitVcs;
import javax.annotation.Nonnull;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

public class GitMacheteContentProvider implements ChangesViewContentProvider {
  public static final String GIT_MACHETE_TOOLBAR = "GitMacheteToolbar";
  private final Project project;
  private GitMachetePanel gitMachetePanel;

  public GitMacheteContentProvider(@Nonnull Project project) {
    this.project = project;
  }

  @Override
  public JComponent initContent() {
    gitMachetePanel = new GitMachetePanel(project);
    ActionToolbar gitMacheteToolbar = gitMachetePanel.createGitMacheteToolbar();
    addBorder(gitMacheteToolbar.getComponent(), createBorder(JBColor.border(), SideBorder.RIGHT));

    JScrollPane scrollTable =
        ScrollPaneFactory.createScrollPane(
            gitMachetePanel.getGitMacheteGraphTableManager().getGitMacheteGraphTable());

    SimpleToolWindowPanel panel =
        new SimpleToolWindowPanel(/*vertical*/ false, /*borderless*/ true);
    panel.setToolbar(gitMacheteToolbar.getComponent());
    panel.setContent(scrollTable);
    return panel;
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
