package com.virtuslab.gitmachete.frontend.ui.root;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentProvider;
import com.intellij.ui.ScrollPaneFactory;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.ui.table.GitMacheteGraphTable;

public class GitMacheteContentProvider implements ChangesViewContentProvider {
  public static final String GIT_MACHETE_TOOLBAR = "GitMacheteToolbar";
  private final Project project;

  public GitMacheteContentProvider(Project project) {
    this.project = project;
  }

  @Override
  @UIEffect
  public JComponent initContent() {
    GitMachetePanel gitMachetePanel = new GitMachetePanel(project);

    GitMacheteGraphTable gitMacheteGraphTable = gitMachetePanel.getGitMacheteGraphTableManager()
        .getGitMacheteGraphTable();
    JScrollPane scrollTable = ScrollPaneFactory.createScrollPane(gitMacheteGraphTable);

    SimpleToolWindowPanel toolbarAndTable = new SimpleToolWindowPanel(/* vertical */ false, /* borderless */ true);
    gitMachetePanel.addToolbarsToWindowPanel(toolbarAndTable);
    toolbarAndTable.setContent(scrollTable);

    return toolbarAndTable;
  }

  @Override
  public void disposeContent() {}

}
