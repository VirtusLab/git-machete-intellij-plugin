package com.virtuslab.gitmachete.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import javax.annotation.Nonnull;
import lombok.Getter;

public class GitMachetePanel {
  @Getter private final GitMacheteGraphTableManager gitMacheteGraphTableManager;

  public GitMachetePanel(Project project) {
    gitMacheteGraphTableManager = new GitMacheteGraphTableManager(project);
    gitMacheteGraphTableManager.updateModel();
  }

  @Nonnull
  public ActionToolbar createGitMacheteToolbar() {
    DefaultActionGroup gitMacheteActions = new DefaultActionGroup();

    DefaultActionGroup refresh = new DefaultActionGroup("Refresh", false);
    refresh.getTemplatePresentation().setIcon(AllIcons.Actions.Refresh);
    refresh.add(new RefreshGitMacheteStatusAction());

    DefaultActionGroup listCommits = new DefaultActionGroup("List Commits", false);
    listCommits.getTemplatePresentation().setIcon(AllIcons.Actions.Show);
    listCommits.add(new ToggleListCommitsAction());

    gitMacheteActions.add(refresh);
    gitMacheteActions.add(listCommits);

    ActionToolbar toolbar =
        ActionManager.getInstance()
            .createActionToolbar(
                GitMacheteContentProvider.GIT_MACHETE_TOOLBAR, gitMacheteActions, false);
    toolbar.setTargetComponent(gitMacheteGraphTableManager.getGitMacheteGraphTable());
    return toolbar;
  }

  private class RefreshGitMacheteStatusAction extends AnAction {
    RefreshGitMacheteStatusAction() {
      super("Refresh Status", "Refresh status", AllIcons.Actions.Refresh);
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      gitMacheteGraphTableManager.updateModel();
    }
  }

  private class ToggleListCommitsAction extends ToggleAction implements DumbAware {
    ToggleListCommitsAction() {
      super("List Commits", "List commits", AllIcons.Actions.ShowHiddens);
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
      return gitMacheteGraphTableManager.isListingCommits();
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
      gitMacheteGraphTableManager.setListingCommits(state);
      gitMacheteGraphTableManager.updateModel();
    }
  }
}
