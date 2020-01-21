package com.virtuslab.gitmachete.ui;

import static com.intellij.ui.IdeBorderFactory.createBorder;
import static com.intellij.util.ui.JBUI.Panels.simplePanel;
import static com.intellij.util.ui.UIUtil.addBorder;

import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentProvider;
import com.intellij.ui.JBColor;
import com.intellij.ui.SideBorder;
import com.intellij.util.ui.components.BorderLayoutPanel;
import javax.annotation.Nonnull;
import javax.swing.JComponent;

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

    BorderLayoutPanel gitMachetePanelWrapper =
        simplePanel(gitMachetePanel.getGitMacheteGraphTableManager().getGitMacheteGraphTable())
            .addToLeft(gitMacheteToolbar.getComponent());

    SimpleToolWindowPanel panel = new SimpleToolWindowPanel(false, true);
    panel.setToolbar(gitMacheteToolbar.getComponent());
    panel.setContent(gitMachetePanelWrapper);
    return panel;
  }

  @Override
  public void disposeContent() {}

  // todo: visibility predicate (invisible when machete file missing???)
}
